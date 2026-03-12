package com.diosaraiva.archutils.ui;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Main application window with menus and content panels.
 */
public class MainFrame extends JFrame {

    private final JPanel contentPanel;
    private final PlantUmlPanel plantUmlPanel;
    private final CsvPanel csvPanel;

    public MainFrame() {
        super("Arch Utils");
        contentPanel = new JPanel(new BorderLayout());
        plantUmlPanel = new PlantUmlPanel();
        csvPanel = new CsvPanel();
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1024, 768));
        setLayout(new BorderLayout());

        contentPanel.add(plantUmlPanel, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        setJMenuBar(MenuBarFactory.create(this));
        pack();
        setLocationRelativeTo(null);
    }

    /** Replaces the content area with the given panel. */
    public void showPanel(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public PlantUmlPanel getPlantUmlPanel() { return plantUmlPanel; }

    public CsvPanel getCsvPanel() { return csvPanel; }
}
