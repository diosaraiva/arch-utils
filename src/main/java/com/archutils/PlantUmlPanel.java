package com.archutils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Panel for generating PlantUML commands from user-provided code and target file.
 */
public class PlantUmlPanel extends JPanel {

    private JTextArea codeTextArea;
    private JTextField targetFileField;
    private JTextArea outputTextArea;

    public PlantUmlPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // --- Input panel (top) ---
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("PlantUML Input"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.BOTH;

        // PlantUML code label
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        inputPanel.add(new JLabel("PlantUML Code:"), gbc);

        // PlantUML code text area
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        codeTextArea = new JTextArea(10, 50);
        codeTextArea.setLineWrap(true);
        codeTextArea.setWrapStyleWord(false);
        codeTextArea.setText("@startuml\n\nAlice -> Bob: Hello\nBob --> Alice: Hi!\n\n@enduml");
        JScrollPane codeScrollPane = new JScrollPane(codeTextArea);
        inputPanel.add(codeScrollPane, gbc);

        // Target File label
        gbc.gridy = 2;
        gbc.weighty = 0.0;
        gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Target File:"), gbc);

        // Target file field + browse button
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        targetFileField = new JTextField(40);
        inputPanel.add(targetFileField, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.0;
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onBrowse();
            }
        });
        inputPanel.add(browseButton, gbc);

        add(inputPanel, BorderLayout.CENTER);

        // --- Generate button (middle) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton generateButton = new JButton("Generate");
        generateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onGenerate();
            }
        });
        buttonPanel.add(generateButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- Output panel (bottom) ---
        JPanel outputPanel = new JPanel(new BorderLayout(4, 4));
        outputPanel.setBorder(BorderFactory.createTitledBorder("Output Command"));
        outputTextArea = new JTextArea(4, 50);
        outputTextArea.setEditable(false);
        outputTextArea.setLineWrap(true);
        outputTextArea.setWrapStyleWord(false);
        JScrollPane outputScrollPane = new JScrollPane(outputTextArea);
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);

        // Re-arrange: stack input on top, output below, generate button between them
        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.add(inputPanel, BorderLayout.CENTER);
        centerPanel.add(outputPanel, BorderLayout.SOUTH);

        removeAll();
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(centerPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void onBrowse() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Target File");
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            targetFileField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void onGenerate() {
        String code = codeTextArea.getText().trim();
        String targetFile = targetFileField.getText().trim();

        if (code.isEmpty()) {
            outputTextArea.setText("Error: PlantUML code cannot be empty.");
            return;
        }

        if (targetFile.isEmpty()) {
            outputTextArea.setText("Error: Target file path cannot be empty.");
            return;
        }

        String command = buildCommand(code, targetFile);
        outputTextArea.setText(command);
    }

    private String buildCommand(String code, String targetFile) {
        File target = new File(targetFile);
        String outputDir = target.getParent() != null ? target.getParent() : ".";
        String fileName = target.getName();

        // Determine source file name (strip extension if present, add .puml)
        String baseName = fileName.contains(".")
                ? fileName.substring(0, fileName.lastIndexOf('.'))
                : fileName;
        String sourceFile = outputDir + File.separator + baseName + ".puml";

        StringBuilder sb = new StringBuilder();
        sb.append("# Step 1: Save PlantUML code to a source file\n");
        sb.append("# ").append(sourceFile).append("\n\n");
        sb.append("# Step 2: Run PlantUML to generate the output\n");
        sb.append("java -jar plantuml.jar");

        // Detect output format from target file extension
        String ext = "";
        if (fileName.contains(".")) {
            ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        }
        if (!ext.isEmpty() && !ext.equals("puml") && !ext.equals("txt")) {
            sb.append(" -t").append(ext);
        }

        sb.append(" \"").append(sourceFile).append("\"");
        sb.append(" -o \"").append(outputDir).append("\"");

        return sb.toString();
    }
}
