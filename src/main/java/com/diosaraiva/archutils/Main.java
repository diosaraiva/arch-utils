package com.diosaraiva.archutils;

import com.diosaraiva.archutils.ui.MainFrame;

import javax.swing.SwingUtilities;
import java.io.File;

/**
 * Entry point for the Arch Utils application.
 */
public class Main {

    private static final String TEMP_DIR =
            System.getProperty("user.dir") + File.separator + "temp";

    public static void main(String[] args) {
        cleanTempDir();
        Runtime.getRuntime().addShutdownHook(new Thread(Main::cleanTempDir));
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }

    private static void cleanTempDir() {
        File dir = new File(TEMP_DIR);
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        }
    }
}
