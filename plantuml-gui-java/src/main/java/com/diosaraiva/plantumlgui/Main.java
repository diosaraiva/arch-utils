package com.diosaraiva.plantumlgui;

import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.diosaraiva.plantumlgui.service.AppSettings;
import com.diosaraiva.plantumlgui.service.PlantUmlRenderer;
import com.diosaraiva.plantumlgui.ui.MainFrame;
import com.diosaraiva.plantumlgui.util.I18n;
import com.diosaraiva.plantumlgui.util.SwingUtils;

// Entry point: applies the persisted settings from java_config.ini, then shows the main window.
public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    private Main() { }

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", I18n.get("app.title"));

        applyTheme(AppSettings.get(AppSettings.THEME));
        SwingUtils.applyFontFamily(AppSettings.get(AppSettings.FONT));
        I18n.setLocale(AppSettings.getLanguage());

        PlantUmlRenderer.cleanTempDir();
        Runtime.getRuntime().addShutdownHook(new Thread(PlantUmlRenderer::cleanTempDir));

        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }

    // A broken theme name must not stop start-up: fall back to the current look and feel.
    private static void applyTheme(String className) {
        try {
            SwingUtils.applyLookAndFeel(className);
        } catch (Exception ex) {
            LOG.warning(() -> "Could not apply look and feel '" + className + "': " + ex.getMessage());
        }
    }
}