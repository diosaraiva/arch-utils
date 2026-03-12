package com.diosaraiva.archutils.ui;

import com.diosaraiva.archutils.service.PlantUmlService;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;

/**
 * Coordinator panel that wires input, buttons and diagram preview.
 */
public class PlantUmlPanel extends JPanel {

    private final PlantUmlInputPanel inputPanel;
    private final PlantUmlButtonPanel buttonPanel;
    private final DiagramPreviewPanel previewPanel;

    public PlantUmlPanel() {
        String defaultTarget = resolveDefaultTarget("png");
        inputPanel = new PlantUmlInputPanel(defaultTarget);
        buttonPanel = new PlantUmlButtonPanel();
        previewPanel = new DiagramPreviewPanel();
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

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
        add(buttonPanel, BorderLayout.SOUTH);

        buttonPanel.onGenerateDiagram(e -> onGenerateDiagram());
        buttonPanel.onFormatChanged(e -> onFormatChanged());
    }

    private void onFormatChanged() {
        inputPanel.setTargetFileExtension(buttonPanel.getSelectedFormat());
    }

    private void onGenerateDiagram() {
        String code = inputPanel.getCode();
        String target = inputPanel.getTargetFile();
        if (code.isEmpty()) { previewPanel.showMessage("PlantUML code is empty."); return; }
        if (target.isEmpty()) { previewPanel.showMessage("Target file path is empty."); return; }
        previewPanel.showMessage("Rendering diagram...");
        final String tempDir = resolveTempDir();
        new SwingWorker<File[], Void>() {
            @Override protected File[] doInBackground() throws Exception {
                PlantUmlService.render(code, target);
                File output = new File(target);
                File preview = null;
                if (target.toLowerCase().endsWith(".svg")) {
                    preview = PlantUmlService.renderPreview(code, tempDir);
                }
                return new File[]{output, preview};
            }
            @Override protected void done() {
                try {
                    File[] result = get();
                    previewPanel.showDiagram(result[0], result[1]);
                } catch (Exception ex) {
                    previewPanel.showMessage("Error: " + ex.getMessage());
                }
            }
        }.execute();
    }

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
