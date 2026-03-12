package com.diosaraiva.archutils.ui;

import com.diosaraiva.archutils.service.PlantUmlService;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;

/**
 * Coordinator panel that wires input, live preview and export.
 * <p>
 * Live preview: as the user types, a debounced timer triggers a background
 * render into the {@code temp/} folder and refreshes the preview panel.
 * <p>
 * Export: target file, format selection and the Export button live in
 * {@link ExportDiagramPanel}, independent of the preview.
 */
public class PlantUmlPanel extends JPanel {

    /** Delay (ms) after the last keystroke before the live preview fires. */
    private static final int PREVIEW_DELAY_MS = 800;

    private final PlantUmlInputPanel inputPanel;
    private final ExportDiagramPanel exportPanel;
    private final DiagramPreviewPanel previewPanel;
    private final Timer previewTimer;

    public PlantUmlPanel() {
        String defaultTarget = resolveDefaultTarget("png");
        inputPanel = new PlantUmlInputPanel();
        exportPanel = new ExportDiagramPanel(defaultTarget);
        previewPanel = new DiagramPreviewPanel();

        // Debounce timer – fires once after the user stops typing
        previewTimer = new Timer(PREVIEW_DELAY_MS, e -> onLivePreview());
        previewTimer.setRepeats(false);

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ---------- centre: input | preview ----------
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, previewPanel);
        splitPane.setResizeWeight(0.35);
        splitPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                splitPane.setDividerLocation(0.35);
                splitPane.removeComponentListener(this);
            }
        });
        add(splitPane, BorderLayout.CENTER);

        // ---------- south: export panel ----------
        add(exportPanel, BorderLayout.SOUTH);

        // ---------- wiring ----------
        exportPanel.onExportDiagram(e -> onExportDiagram());
        exportPanel.onFormatChanged(e -> onFormatChanged());

        // Live preview: listen to every code change
        inputPanel.addCodeDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { restartPreviewTimer(); }
            @Override public void removeUpdate(DocumentEvent e)  { restartPreviewTimer(); }
            @Override public void changedUpdate(DocumentEvent e) { restartPreviewTimer(); }
        });

        // Pre-render the initial sample so the preview is ready on startup
        SwingUtilities.invokeLater(this::onLivePreview);
    }

    // -------------------- live preview --------------------

    private void restartPreviewTimer() {
        previewTimer.restart();
    }

    private void onLivePreview() {
        String code = inputPanel.getCode();
        if (code.isEmpty()) {
            previewPanel.showMessage("PlantUML code is empty.");
            return;
        }
        previewPanel.showMessage("Rendering preview...");
        final String tempDir = resolveTempDir();

        new SwingWorker<File, Void>() {
            @Override
            protected File doInBackground() throws Exception {
                return PlantUmlService.renderPreview(code, tempDir);
            }

            @Override
            protected void done() {
                try {
                    File preview = get();
                    if (preview != null && preview.isFile()) {
                        previewPanel.showDiagram(preview);
                    } else {
                        previewPanel.showMessage("Preview generation returned no image.");
                    }
                } catch (Exception ex) {
                    previewPanel.showMessage("Preview error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // -------------------- export --------------------

    private void onFormatChanged() {
        exportPanel.setTargetFileExtension(exportPanel.getSelectedFormat());
    }

    private void onExportDiagram() {
        String code = inputPanel.getCode();
        String target = exportPanel.getTargetFile();
        if (code.isEmpty()) { previewPanel.showMessage("PlantUML code is empty."); return; }
        if (target.isEmpty()) { previewPanel.showMessage("Target file path is empty."); return; }
        previewPanel.showMessage("Exporting diagram...");
        final String tempDir = resolveTempDir();

        new SwingWorker<File[], Void>() {
            @Override
            protected File[] doInBackground() throws Exception {
                PlantUmlService.render(code, target);
                File output = new File(target);
                File preview = null;
                if (target.toLowerCase().endsWith(".svg")) {
                    preview = PlantUmlService.renderPreview(code, tempDir);
                }
                return new File[]{output, preview};
            }

            @Override
            protected void done() {
                try {
                    File[] result = get();
                    previewPanel.showDiagram(result[0], result[1]);
                } catch (Exception ex) {
                    previewPanel.showMessage("Export error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // -------------------- helpers --------------------

    private static String resolveTempDir() {
        return System.getProperty("user.dir") + File.separator + "temp";
    }

    private static String resolveDefaultTarget(String ext) {
        String userDir = System.getProperty("user.dir");
        return userDir + File.separator + "output"
                + File.separator + "target." + ext;
    }

    public PlantUmlInputPanel getInputPanel() { return inputPanel; }
}
