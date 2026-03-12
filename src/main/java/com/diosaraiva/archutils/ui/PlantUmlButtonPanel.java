package com.diosaraiva.archutils.ui;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

/**
 * Button bar with format selection and Generate Diagram action.
 */
public class PlantUmlButtonPanel extends JPanel {

    private final JRadioButton pngRadio;
    private final JRadioButton svgRadio;
    private final JRadioButton pumlRadio;
    private final JButton generateDiagramButton;

    public PlantUmlButtonPanel() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 12, 4));

        pngRadio = new JRadioButton("PNG", true);
        svgRadio = new JRadioButton("SVG");
        pumlRadio = new JRadioButton("PUML");
        ButtonGroup group = new ButtonGroup();
        group.add(pngRadio);
        group.add(svgRadio);
        group.add(pumlRadio);

        generateDiagramButton = new JButton("Generate Diagram");

        add(pngRadio);
        add(svgRadio);
        add(pumlRadio);
        add(generateDiagramButton);
    }

    public void onGenerateDiagram(ActionListener listener) {
        generateDiagramButton.addActionListener(listener);
    }

    /** Returns "png", "svg" or "puml" based on selected radio. */
    public String getSelectedFormat() {
        if (svgRadio.isSelected()) return "svg";
        if (pumlRadio.isSelected()) return "puml";
        return "png";
    }

    public void onFormatChanged(ActionListener listener) {
        pngRadio.addActionListener(listener);
        svgRadio.addActionListener(listener);
        pumlRadio.addActionListener(listener);
    }
}
