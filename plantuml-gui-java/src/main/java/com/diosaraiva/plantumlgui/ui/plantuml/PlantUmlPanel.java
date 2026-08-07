package com.diosaraiva.plantumlgui.ui.plantuml;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.diosaraiva.plantumlgui.service.ArchimatePlantUmlConverter;
import com.diosaraiva.plantumlgui.service.PlantUmlFormat;
import com.diosaraiva.plantumlgui.service.PlantUmlRenderer;
import com.diosaraiva.plantumlgui.util.Background;
import com.diosaraiva.plantumlgui.util.I18n;
import com.diosaraiva.plantumlgui.util.SwingUtils;

public class PlantUmlPanel extends JPanel {

    private static final int PREVIEW_DELAY_MS = 800;
    private static final double INPUT_WEIGHT = 0.4;

    private final PlantUmlInputPanel inputPanel;
    private final PlantUmlOutputPanel outputPanel;
    private final PlantUmlFooterPanel footerPanel;
    private final Timer previewTimer;

    public PlantUmlPanel() {
        var defaultTarget = resolveDefaultTarget("png");
        inputPanel = new PlantUmlInputPanel();
        outputPanel = new PlantUmlOutputPanel(this::onLivePreview);
        footerPanel = new PlantUmlFooterPanel(defaultTarget);

        previewTimer = new Timer(PREVIEW_DELAY_MS, e -> onLivePreview());
        previewTimer.setRepeats(false);

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        var input = section(inputPanel.getEditorComponent(), inputPanel.getControlsComponent());
        var output = section(outputPanel, null);
        var split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, input, output);
        split.setResizeWeight(INPUT_WEIGHT);
        split.setContinuousLayout(true);
        split.setBorder(null);
        split.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                split.setDividerLocation(INPUT_WEIGHT);
                split.removeComponentListener(this);
            }
        });

        add(split, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);

        footerPanel.onExportDiagram(e -> onExportDiagram());
        footerPanel.onFormatChanged(e -> onFormatChanged());
        footerPanel.onCopyImage(e -> copyImageToClipboard());
        footerPanel.setCopyImageEnabled(false);

        inputPanel.addCodeDocumentListener(SwingUtils.onDocumentChange(this::restartPreviewTimer));
        inputPanel.addPreviewButtonListener(e -> onLivePreview());
        inputPanel.addAutoPreviewListener(e -> {
            if (inputPanel.isAutoPreviewEnabled()) { onLivePreview(); }
        });

        SwingUtilities.invokeLater(this::onLivePreview);
    }

    private static JPanel section(Component center, Component south) {
        var panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        if (center != null) { panel.add(center, BorderLayout.CENTER); }
        if (south != null) { panel.add(south, BorderLayout.SOUTH); }
        return panel;
    }

    private void restartPreviewTimer() {
        if (inputPanel.isAutoPreviewEnabled()) { previewTimer.restart(); }
    }

    private void onLivePreview() {
        var code = inputPanel.getCode();
        if (code.isEmpty()) {
            outputPanel.showMessage(I18n.get("plantuml.code.empty"));
            return;
        }
        outputPanel.showRendering();
        var tempDir = resolveTempDir();

        Background.run(
                () -> PlantUmlRenderer.compilePreview(code, tempDir),
                result -> {
                    outputPanel.showCompileResult(result);
                    footerPanel.setCopyImageEnabled(outputPanel.getCurrentImage() != null);
                },
                ex -> {
                    outputPanel.showRenderError(ex);
                    footerPanel.setCopyImageEnabled(false);
                });
    }

    private void onFormatChanged() {
        footerPanel.setTargetFileExtension(footerPanel.getSelectedFormat());
    }

    private void onExportDiagram() {
        var code = inputPanel.getCode();
        var target = footerPanel.getTargetFile();
        if (code.isEmpty()) { showError(I18n.get("plantuml.code.empty")); return; }
        if (target.isEmpty()) { showError(I18n.get("export.target.empty")); return; }
        if (footerPanel.isArchimateSelected()) { onExportArchimate(code, target); return; }

        outputPanel.showMessage(I18n.get("export.exporting"));
        var tempDir = resolveTempDir();
        var format = PlantUmlFormat.fromExtension(footerPanel.getSelectedFormat())
                .orElse(PlantUmlFormat.PNG);

        Background.run(
                () -> {
                    var output = new File(target);
                    PlantUmlRenderer.export(code, output, format);

                    File preview = format == PlantUmlFormat.SVG
                            ? PlantUmlRenderer.compilePreview(code, tempDir).previewImage() : null;
                    return new ExportResult(output, preview);
                },
                result -> {
                    try {
                        outputPanel.showDiagram(result.output(), result.preview());
                        footerPanel.setCopyImageEnabled(outputPanel.getCurrentImage() != null);
                        JOptionPane.showMessageDialog(this,
                                I18n.get("export.success.msg", result.output().getAbsolutePath()),
                                I18n.get("export.success.title"), JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        outputPanel.showMessage(I18n.get("plantuml.preview.error", ex.getMessage()));
                    }
                },
                ex -> {
                    outputPanel.showMessage(I18n.get("plantuml.preview.error", ex.getMessage()));
                    JOptionPane.showMessageDialog(this, I18n.get("export.fail.msg", ex.getMessage()),
                            I18n.get("export.error.title"), JOptionPane.ERROR_MESSAGE);
                });
    }

    private void onExportArchimate(String code, String target) {
        var path = target.toLowerCase().endsWith(".xml") ? target : target + ".xml";
        var output = new File(path);
        var modelName = deriveModelName(output);
        outputPanel.selectConsole();
        outputPanel.appendConsole(I18n.get("archimate.export.started"), I18n.get("archimate.export.converting"));

        Background.run(
                () -> {
                    var result = ArchimatePlantUmlConverter.convert(code, modelName);
                    result.model().writeTo(output);
                    return result;
                },
                result -> {
                    var warnings = result.warnings();
                    var sb = new StringBuilder("Wrote ").append(output.getAbsolutePath())
                            .append(System.lineSeparator())
                            .append("Elements: ").append(result.model().getElementCount())
                            .append(", Relationships: ").append(result.model().getRelationshipCount());
                    for (var w : warnings) {
                        sb.append(System.lineSeparator()).append("  - ").append(w);
                    }
                    outputPanel.appendConsole(I18n.get("archimate.export.finished"), sb.toString());
                    var extra = warnings.isEmpty() ? ""
                            : I18n.get("archimate.export.warnings", warnings.size());
                    JOptionPane.showMessageDialog(this,
                            I18n.get("archimate.export.msg", output.getAbsolutePath()) + extra,
                            I18n.get("export.success.title"), JOptionPane.INFORMATION_MESSAGE);
                },
                ex -> {
                    outputPanel.appendConsole(I18n.get("archimate.export.failed"), String.valueOf(ex.getMessage()));
                    JOptionPane.showMessageDialog(this, I18n.get("archimate.export.failmsg", ex.getMessage()),
                            I18n.get("export.error.title"), JOptionPane.ERROR_MESSAGE);
                });
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message,
                I18n.get("export.error.title"), JOptionPane.ERROR_MESSAGE);
    }

    private static String deriveModelName(File file) {
        var name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    public boolean copyImageToClipboard() {
        var image = outputPanel.getCurrentImage();
        if (image == null) {
            outputPanel.showMessage(I18n.get("copy.none"));
            return false;
        }
        SwingUtils.copyImage(image);
        return true;
    }

    public void applyLanguage() {
        inputPanel.applyLanguage();
        outputPanel.applyLanguage();
        footerPanel.applyLanguage();
    }

    private static String resolveTempDir() {
        return System.getProperty("user.dir") + File.separator + "temp";
    }

    private static String resolveDefaultTarget(String ext) {
        return System.getProperty("user.dir") + File.separator + "output"
                + File.separator + "target." + ext;
    }

    public PlantUmlInputPanel getInputPanel() { return inputPanel; }

    private static final class ExportResult {
        private final File output;
        private final File preview;

        ExportResult(File output, File preview) {
            this.output = output;
            this.preview = preview;
        }

        File output() { return output; }

        File preview() { return preview; }
    }
}
