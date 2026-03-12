package com.diosaraiva.archutils.ui;

import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Factory that builds the application menu bar.
 */
public final class MenuBarFactory {

    private MenuBarFactory() { }

    public static JMenuBar create(MainFrame frame) {
        JMenuBar bar = new JMenuBar();
        bar.add(createFileMenu(frame));
        bar.add(createServicesMenu(frame));
        bar.add(createSettingsMenu(frame));
        bar.add(createHelpMenu(frame));
        return bar;
    }

    private static JMenu createFileMenu(MainFrame frame) {
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);

        JMenuItem open = new JMenuItem("Open");
        open.setMnemonic(KeyEvent.VK_O);
        open.addActionListener(e -> onOpenFile(frame));
        menu.add(open);

        menu.addSeparator();

        JMenuItem quit = new JMenuItem("Quit");
        quit.setMnemonic(KeyEvent.VK_Q);
        quit.addActionListener(e -> System.exit(0));
        menu.add(quit);
        return menu;
    }

    private static void onOpenFile(MainFrame frame) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open PlantUML File");
        chooser.setFileFilter(new FileNameExtensionFilter("PlantUML files (*.puml)", "puml"));
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                frame.showPanel(frame.getPlantUmlPanel());
                frame.getPlantUmlPanel().getInputPanel().setCode(content);
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(frame,
                        "Failed to open file: " + ex.getMessage(),
                        "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static JMenu createServicesMenu(MainFrame frame) {
        JMenu menu = new JMenu("Services");
        menu.setMnemonic(KeyEvent.VK_V);
        JMenuItem plantUml = new JMenuItem("PlantUML");
        plantUml.addActionListener(e -> frame.showPanel(frame.getPlantUmlPanel()));
        menu.add(plantUml);
        JMenuItem csv = new JMenuItem("CSV/JSON/MD");
        csv.addActionListener(e -> frame.showPanel(frame.getCsvPanel()));
        menu.add(csv);
        return menu;
    }

    private static JMenu createSettingsMenu(MainFrame frame) {
        JMenu menu = new JMenu("Settings");
        menu.setMnemonic(KeyEvent.VK_S);

        JMenu themeMenu = new JMenu("Theme");
        themeMenu.setMnemonic(KeyEvent.VK_T);

        ButtonGroup group = new ButtonGroup();
        String currentLaf = UIManager.getLookAndFeel().getClass().getName();

        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(info.getName());
            item.setSelected(info.getClassName().equals(currentLaf));
            item.addActionListener(e -> switchLookAndFeel(info.getClassName(), frame));
            group.add(item);
            themeMenu.add(item);
        }

        menu.add(themeMenu);
        return menu;
    }

    private static void switchLookAndFeel(String className, MainFrame frame) {
        try {
            UIManager.setLookAndFeel(className);
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }
            frame.pack();
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(frame,
                    "Failed to switch theme: " + ex.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JMenu createHelpMenu(MainFrame frame) {
        JMenu menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        JMenuItem about = new JMenuItem("About");
        about.setMnemonic(KeyEvent.VK_A);
        about.addActionListener(e -> new AboutDialog(frame).setVisible(true));
        menu.add(about);
        return menu;
    }
}
