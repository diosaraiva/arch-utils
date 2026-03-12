package com.diosaraiva.archutils.ui;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Panel that displays a rendered PNG or PUML diagram inline.
 * For SVG, a message is shown since inline preview is not supported.
 */
public class DiagramPreviewPanel extends JPanel {

    private static final String PNG_CARD = "png";
    private static final String PUML_CARD = "puml";
    private static final String MSG_CARD = "msg";

    private final CardLayout cards;
    private final JPanel cardPanel;
    private final JLabel pngLabel;
    private final JTextArea pumlArea;
    private final JLabel msgLabel;

    public DiagramPreviewPanel() {
        cards = new CardLayout();
        cardPanel = new JPanel(cards);
        pngLabel = new JLabel();
        pumlArea = new JTextArea();
        msgLabel = new JLabel();
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Preview"));

        pngLabel.setHorizontalAlignment(JLabel.CENTER);
        cardPanel.add(new JScrollPane(pngLabel), PNG_CARD);

        pumlArea.setEditable(false);
        pumlArea.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 13));
        cardPanel.add(new JScrollPane(pumlArea), PUML_CARD);

        msgLabel.setHorizontalAlignment(JLabel.CENTER);
        cardPanel.add(msgLabel, MSG_CARD);

        add(cardPanel, BorderLayout.CENTER);
        showMessage("No diagram generated yet.");
    }

    /** Shows an informational or error message. */
    public void showMessage(String text) {
        msgLabel.setText(text);
        cards.show(cardPanel, MSG_CARD);
    }

    /** Loads and displays the diagram from the given file. */
    public void showDiagram(File file) throws IOException {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".svg")) {
            showMessage("SVG created, Preview not supported. File: "
                    + file.getAbsolutePath());
        } else if (name.endsWith(".png")) {
            showPng(file);
        } else if (name.endsWith(".puml")) {
            showPuml(file);
        } else {
            showMessage("Unsupported format: " + name);
        }
    }

    private void showPng(File file) {
        // Read bytes to avoid ImageIcon caching stale images by path
        ImageIcon icon = new ImageIcon(file.getAbsolutePath());
        icon.getImage().flush();
        pngLabel.setIcon(icon);
        cards.show(cardPanel, PNG_CARD);
    }

    private void showPuml(File file) throws IOException {
        String puml = new String(Files.readAllBytes(file.toPath()),
                StandardCharsets.UTF_8);
        pumlArea.setText(puml);
        pumlArea.setCaretPosition(0);
        cards.show(cardPanel, PUML_CARD);
    }
}
