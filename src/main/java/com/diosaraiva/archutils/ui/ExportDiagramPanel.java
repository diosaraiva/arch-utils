package com.diosaraiva.archutils.ui;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

/**
 * Export panel: target file selector, format radio buttons and Export Diagram action.
 * This panel is independent of the live‑preview functionality.
 */
public class ExportDiagramPanel extends JPanel {

    private final JTextField targetFileField;
    private final JButton browseButton;
    private final JRadioButton pngRadio;
    private final JRadioButton svgRadio;
    private final JRadioButton pumlRadio;
    private final JButton exportButton;

    public ExportDiagramPanel(String defaultTargetFile) {
        targetFileField = new JTextField(20);
        targetFileField.setText(defaultTargetFile);
        browseButton = new JButton("Browse...");
        pngRadio = new JRadioButton("PNG", true);
        svgRadio = new JRadioButton("SVG");
        pumlRadio = new JRadioButton("PUML");
        exportButton = new JButton("Export Diagram");
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Export Diagram"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        // Target File label
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        add(new JLabel("Target File:"), gbc);

        // Target File text field
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(targetFileField, gbc);

        // Browse button
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        browseButton.addActionListener(e -> onBrowse());
        add(browseButton, gbc);

        // Format radio buttons
        ButtonGroup group = new ButtonGroup();
        group.add(pngRadio);
        group.add(svgRadio);
        group.add(pumlRadio);

        gbc.gridx = 3;
        add(pngRadio, gbc);
        gbc.gridx = 4;
        add(svgRadio, gbc);
        gbc.gridx = 5;
        add(pumlRadio, gbc);

        // Export button
        gbc.gridx = 6;
        add(exportButton, gbc);
    }

    private void onBrowse() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Target File");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            targetFileField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    // ---------- public API ----------

    public void onExportDiagram(ActionListener listener) {
        exportButton.addActionListener(listener);
    }

    public void onFormatChanged(ActionListener listener) {
        pngRadio.addActionListener(listener);
        svgRadio.addActionListener(listener);
        pumlRadio.addActionListener(listener);
    }

    /** Returns "png", "svg" or "puml" based on selected radio. */
    public String getSelectedFormat() {
        if (svgRadio.isSelected()) return "svg";
        if (pumlRadio.isSelected()) return "puml";
        return "png";
    }

    public String getTargetFile() {
        return targetFileField.getText().trim();
    }

    public void setTargetFileExtension(String ext) {
        String current = targetFileField.getText().trim();
        int dot = current.lastIndexOf('.');
        String base = dot > 0 ? current.substring(0, dot) : current;
        targetFileField.setText(base + "." + ext);
    }
}
