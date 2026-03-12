package com.diosaraiva.archutils.ui;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;

/**
 * Output section of the PlantUML panel: displays command or status messages.
 */
public class PlantUmlOutputPanel extends JPanel {

    private final JTextArea outputTextArea;

    public PlantUmlOutputPanel() {
        outputTextArea = new JTextArea(4, 50);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Output"));
        outputTextArea.setEditable(false);
        outputTextArea.setLineWrap(true);
        outputTextArea.setWrapStyleWord(false);
        add(new JScrollPane(outputTextArea), BorderLayout.CENTER);
    }

    public void setText(String text) { outputTextArea.setText(text); }

    public void appendText(String text) { outputTextArea.append(text); }
}
