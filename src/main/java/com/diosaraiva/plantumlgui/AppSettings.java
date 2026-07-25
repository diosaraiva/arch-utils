package com.diosaraiva.plantumlgui;

import java.util.Locale;
import java.util.prefs.Preferences;

import com.diosaraiva.plantumlgui.util.I18n;

public final class AppSettings {

    private static final Preferences PREFS =
            Preferences.userRoot().node("com/diosaraiva/plantumlgui");

    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_JAR_PATH = "jarPath";

    private AppSettings() { }

    public static Locale getLanguage() {
        var tag = PREFS.get(KEY_LANGUAGE, "en-US");
        var locale = Locale.forLanguageTag(tag);
        return locale.getLanguage().isEmpty() ? I18n.EN_US : locale;
    }

    public static void setLanguage(Locale locale) {
        PREFS.put(KEY_LANGUAGE, locale.toLanguageTag());
    }

    public static String getJarPath() {
        return PREFS.get(KEY_JAR_PATH, "").trim();
    }

    public static void setJarPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            PREFS.remove(KEY_JAR_PATH);
        } else {
            PREFS.put(KEY_JAR_PATH, path.trim());
        }
    }
}
