package com.diosaraiva.archutils.ui;

import com.diosaraiva.archutils.util.SampleLoader;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Input section: sample selector, code editor and target file.
 */
public class PlantUmlInputPanel extends JPanel {

    private final JComboBox<DiagramSample> sampleCombo;
    private final JTextArea codeTextArea;
    private final JTextField targetFileField;

    public PlantUmlInputPanel(String defaultTargetFile) {
        sampleCombo = new JComboBox<>(DiagramSample.values());
        codeTextArea = new JTextArea(10, 20);
        targetFileField = new JTextField(15);
        targetFileField.setText(defaultTargetFile);
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("PlantUML Input"));
        GridBagConstraints gbc = createGbc();

        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(new JLabel("Code:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        sampleCombo.addActionListener(e -> loadSample());
        add(sampleCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        codeTextArea.setLineWrap(true);
        codeTextArea.setWrapStyleWord(false);
        add(new JScrollPane(codeTextArea), gbc);

        gbc.gridy = 2;
        gbc.weighty = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel targetRow = new JPanel(new java.awt.BorderLayout(4, 0));
        targetRow.add(new JLabel("Target File: "), java.awt.BorderLayout.WEST);
        targetRow.add(targetFileField, java.awt.BorderLayout.CENTER);
        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(e -> onBrowse());
        targetRow.add(browseBtn, java.awt.BorderLayout.EAST);
        add(targetRow, gbc);

        loadSample();
    }

    private void loadSample() {
        DiagramSample sample = (DiagramSample) sampleCombo.getSelectedItem();
        if (sample == null) return;
        try {
            codeTextArea.setText(SampleLoader.load(sample.getFileName()));
            codeTextArea.setCaretPosition(0);
        } catch (Exception ex) {
            codeTextArea.setText("Error loading sample: " + ex.getMessage());
        }
    }

    private GridBagConstraints createGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        return gbc;
    }

    private void onBrowse() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Target File");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            targetFileField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    public String getCode() { return codeTextArea.getText().trim(); }

    public void setCode(String code) {
        codeTextArea.setText(code);
        codeTextArea.setCaretPosition(0);
    }

    public String getTargetFile() { return targetFileField.getText().trim(); }

    public void setTargetFileExtension(String ext) {
        String current = targetFileField.getText().trim();
        int dot = current.lastIndexOf('.');
        String base = dot > 0 ? current.substring(0, dot) : current;
        targetFileField.setText(base + "." + ext);
    }
}
