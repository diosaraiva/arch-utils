package com.diosaraiva.plantumlgui.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.diosaraiva.plantumlgui.AppSettings;
import com.diosaraiva.plantumlgui.service.PlantUmlRenderer;
import com.diosaraiva.plantumlgui.util.I18n;
import com.diosaraiva.plantumlgui.util.SwingUtils;

public class PlantUmlFooterPanel extends JTabbedPane {

    private static final Map<String, String> FORMATS = new LinkedHashMap<>();
    static {
        FORMATS.put("PNG", "png");
        FORMATS.put("SVG", "svg");
        FORMATS.put("PUML", "puml");
        FORMATS.put(I18n.get("format.archimate"), "xml");
    }

    private final JTextField targetFileField = new JTextField(20);
    private final JButton browseButton = new JButton(I18n.get("export.browse"));
    private final JComboBox<String> formatCombo = new JComboBox<>(
            new DefaultComboBoxModel<>(FORMATS.keySet().toArray(new String[0])));
    private final JButton exportButton = new JButton(I18n.get("export.button"));
    private final JButton copyImageButton = new JButton(I18n.get("export.copy"));
    private final JLabel targetFileLabel = new JLabel(I18n.get("export.targetFile"));
    private final JLabel formatLabel = new JLabel(I18n.get("export.format"));

    private final JLabel jarPathLabel = new JLabel(I18n.get("config.jarPath"));
    private final JTextField jarPathField = new JTextField(24);
    private final JButton jarBrowseButton = new JButton(I18n.get("export.browse"));

    public PlantUmlFooterPanel(String defaultTargetFile) {
        targetFileField.setText(defaultTargetFile);
        formatCombo.setSelectedItem("PNG");
        formatCombo.setToolTipText(I18n.get("export.format.tooltip"));
        copyImageButton.setToolTipText(I18n.get("export.copy.tooltip"));
        browseButton.addActionListener(e -> onBrowse());

        jarPathField.setText(effectiveJarPath());
        jarBrowseButton.addActionListener(e -> onJarBrowse());
        jarPathField.getDocument().addDocumentListener(
                SwingUtils.onDocumentChange(this::persistJarPath));

        addTab(I18n.get("export.panel.title"), buildExportTab());
        addTab(I18n.get("config.tab"), buildConfigTab());
    }

    private JPanel buildExportTab() {
        var panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(targetFileLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 6, 4, 1);
        panel.add(targetFileField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(4, 1, 4, 6);
        panel.add(browseButton, gbc);

        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.gridx = 3;
        panel.add(formatLabel, gbc);
        gbc.gridx = 4;
        panel.add(formatCombo, gbc);
        gbc.gridx = 5;
        panel.add(exportButton, gbc);
        gbc.gridx = 6;
        panel.add(copyImageButton, gbc);
        return panel;
    }

    private JPanel buildConfigTab() {
        var panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        panel.add(jarPathLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 6, 4, 1);
        panel.add(jarPathField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(4, 1, 4, 6);
        panel.add(jarBrowseButton, gbc);
        return panel;
    }

    public void applyLanguage() {
        setTitleAt(0, I18n.get("export.panel.title"));
        setTitleAt(1, I18n.get("config.tab"));
        targetFileLabel.setText(I18n.get("export.targetFile"));
        formatLabel.setText(I18n.get("export.format"));
        browseButton.setText(I18n.get("export.browse"));
        exportButton.setText(I18n.get("export.button"));
        copyImageButton.setText(I18n.get("export.copy"));
        copyImageButton.setToolTipText(I18n.get("export.copy.tooltip"));
        formatCombo.setToolTipText(I18n.get("export.format.tooltip"));
        jarPathLabel.setText(I18n.get("config.jarPath"));
        jarBrowseButton.setText(I18n.get("export.browse"));
        repaint();
    }

    private void onBrowse() {
        var chooser = new JFileChooser();
        chooser.setDialogTitle(I18n.get("export.browse.title"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            targetFileField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void onJarBrowse() {
        var chooser = new JFileChooser();
        chooser.setDialogTitle(I18n.get("config.browse.title"));
        chooser.setFileFilter(new FileNameExtensionFilter(I18n.get("config.jar.filter"), "jar"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            jarPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void persistJarPath() {
        var path = jarPathField.getText().trim();
        AppSettings.setJarPath(path.equals(PlantUmlRenderer.bundledJarPath()) ? "" : path);
    }

    private static String effectiveJarPath() {
        var custom = AppSettings.getJarPath();
        return custom.isEmpty() ? PlantUmlRenderer.bundledJarPath() : custom;
    }

    public void onExportDiagram(ActionListener listener) {
        exportButton.addActionListener(listener);
    }

    public void onCopyImage(ActionListener listener) {
        copyImageButton.addActionListener(listener);
    }

    public void setCopyImageEnabled(boolean enabled) {
        copyImageButton.setEnabled(enabled);
    }

    public void onFormatChanged(ActionListener listener) {
        formatCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                listener.actionPerformed(null);
            }
        });
    }

    public String getSelectedFormat() {
        return FORMATS.getOrDefault(formatCombo.getSelectedItem(), "png");
    }

    public boolean isArchimateSelected() {
        return "xml".equals(getSelectedFormat());
    }

    public String getTargetFile() {
        return targetFileField.getText().trim();
    }

    public void setTargetFileExtension(String ext) {
        var current = targetFileField.getText().trim();
        int dot = current.lastIndexOf('.');
        var base = dot > 0 ? current.substring(0, dot) : current;
        targetFileField.setText(base + "." + ext);
    }
}
