package com.archutils;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Main application window with menus and content panels.
 */
public class MainFrame extends JFrame {

    private JPanel contentPanel;
    private PlantUmlPanel plantUmlPanel;
    private PreferencesPanel preferencesPanel;

    public MainFrame() {
        super("Arch Utils");
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(800, 600));
        setLayout(new BorderLayout());

        plantUmlPanel = new PlantUmlPanel();
        preferencesPanel = new PreferencesPanel();

        contentPanel = new JPanel(new BorderLayout());
        // Default panel is PlantUml (first option in Settings)
        contentPanel.add(plantUmlPanel, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        setJMenuBar(createMenuBar());
        pack();
        setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        menuBar.add(createFileMenu());
        menuBar.add(createServicesMenu());
        menuBar.add(createSettingsMenu());
        menuBar.add(createHelpMenu());

        return menuBar;
    }

    private JMenu createFileMenu() {
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem quitItem = new JMenuItem("Quit");
        quitItem.setMnemonic(KeyEvent.VK_Q);
        quitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        fileMenu.add(quitItem);
        return fileMenu;
    }

    private JMenu createServicesMenu() {
        JMenu servicesMenu = new JMenu("Services");
        servicesMenu.setMnemonic(KeyEvent.VK_V);
        return servicesMenu;
    }

    private JMenu createSettingsMenu() {
        JMenu settingsMenu = new JMenu("Settings");
        settingsMenu.setMnemonic(KeyEvent.VK_S);

        JMenuItem plantUmlItem = new JMenuItem("PlantUML");
        plantUmlItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPanel(plantUmlPanel);
            }
        });

        JMenuItem preferencesItem = new JMenuItem("Preferences");
        preferencesItem.setMnemonic(KeyEvent.VK_P);
        preferencesItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPanel(preferencesPanel);
            }
        });

        settingsMenu.add(plantUmlItem);
        settingsMenu.add(preferencesItem);
        return settingsMenu;
    }

    private JMenu createHelpMenu() {
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.setMnemonic(KeyEvent.VK_A);
        aboutItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AboutDialog dialog = new AboutDialog(MainFrame.this);
                dialog.setVisible(true);
            }
        });

        helpMenu.add(aboutItem);
        return helpMenu;
    }

    private void showPanel(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }
}
