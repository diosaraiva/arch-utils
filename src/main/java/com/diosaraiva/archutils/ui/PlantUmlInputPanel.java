package com.diosaraiva.archutils.ui;

import com.diosaraiva.archutils.util.SampleLoader;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Input section: sample selector and code editor.
 */
public class PlantUmlInputPanel extends JPanel {

    private final JComboBox<DiagramSample> sampleCombo;
    private final JTextArea codeTextArea;

    public PlantUmlInputPanel() {
        sampleCombo = new JComboBox<>(DiagramSample.values());
        codeTextArea = new JTextArea(10, 20);
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

    public String getCode() { return codeTextArea.getText().trim(); }

    public void setCode(String code) {
        codeTextArea.setText(code);
        codeTextArea.setCaretPosition(0);
    }

    /** Allows external listeners to observe code changes for live preview. */
    public void addCodeDocumentListener(DocumentListener listener) {
        codeTextArea.getDocument().addDocumentListener(listener);
    }
}
