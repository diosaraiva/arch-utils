package com.diosaraiva.plantumlgui.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.diosaraiva.plantumlgui.service.AppSettings;
import com.diosaraiva.plantumlgui.ui.plantuml.PlantUmlPanel;
import com.diosaraiva.plantumlgui.util.I18n;

// Application window: hosts the PlantUML panel and owns the menu bar and window size.
@SuppressWarnings("serial")
public final class MainFrame extends JFrame {

    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private final PlantUmlPanel plantUmlPanel = new PlantUmlPanel();

    private int selectedWidth = AppSettings.getInt(AppSettings.WINDOW_WIDTH, AppSettings.DEFAULT_WINDOW_WIDTH);
    private int selectedHeight = AppSettings.getInt(AppSettings.WINDOW_HEIGHT, AppSettings.DEFAULT_WINDOW_HEIGHT);

    public MainFrame() {
        super(I18n.get("app.title"));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(selectedWidth, selectedHeight));

        contentPanel.add(plantUmlPanel, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        rebuildMenuBar();
        pack();
        setLocationRelativeTo(null);
    }

    public PlantUmlPanel getPlantUmlPanel() { return plantUmlPanel; }

    public int getSelectedWidth() { return selectedWidth; }

    public int getSelectedHeight() { return selectedHeight; }

    // Rebuilds every localised widget after a language change.
    public void reloadLanguage() {
        SwingUtilities.invokeLater(() -> {
            setTitle(I18n.get("app.title"));
            rebuildMenuBar();
            plantUmlPanel.applyLanguage();
            refresh();
        });
    }

    public void showPanel(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // Applies the requested size, never growing beyond the usable screen area.
    // The choice is remembered in memory only; Config > Save persists it.
    public void applyResolution(int width, int height) {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        selectedWidth = width;
        selectedHeight = height;
        AppSettings.set(AppSettings.WINDOW_WIDTH, width);
        AppSettings.set(AppSettings.WINDOW_HEIGHT, height);

        setSize(Math.min(width, screen.width), Math.min(height, screen.height));
        setLocationRelativeTo(null);
        rebuildMenuBar();
        refresh();
    }

    private void rebuildMenuBar() {
        setJMenuBar(MenuBar.create(this));
    }

    private void refresh() {
        revalidate();
        repaint();
    }
}