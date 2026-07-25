package com.diosaraiva.plantumlgui.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public final class I18n {

    /** Location of the messages files relative to a resources root. */
    private static final String BUNDLE_DIR = "i18n";
    private static final String BUNDLE_BASE = "messages";

    public static final Locale EN_US = new Locale("en", "US");
    public static final Locale PT_BR = new Locale("pt", "BR");
    public static final Locale ES_ES = new Locale("es", "ES");

    private static Locale current = EN_US;
    private static Properties messages = load(current);

    private I18n() { }

    public static Locale getLocale() { return current; }

    public static void setLocale(Locale locale) {
        current = locale == null ? EN_US : locale;
        messages = load(current);
    }

    public static String get(String key) {
        var value = messages.getProperty(key);
        return value != null ? value : "!" + key + "!";
    }

    public static String get(String key, Object... args) {
        return java.text.MessageFormat.format(get(key), args);
    }

    /**
     * Loads the messages for the given locale directly from disk (no classpath).
     * Falls back from the most specific candidate to the least specific:
     * messages_en_US -> messages_en -> messages.
     */
    private static Properties load(Locale locale) {
        var props = new Properties();
        for (var name : candidateFileNames(locale)) {
            if (loadInto(props, BUNDLE_DIR + "/" + name)) {
                return props;
            }
        }
        System.err.println("I18n: no messages file found on disk for locale " + locale);
        return props; // empty -> get() returns !key!
    }

    private static String[] candidateFileNames(Locale locale) {
        var lang = locale.getLanguage();
        var country = locale.getCountry();
        if (!country.isEmpty()) {
            return new String[] {
                    BUNDLE_BASE + "_" + lang + "_" + country + ".properties",
                    BUNDLE_BASE + "_" + lang + ".properties",
                    BUNDLE_BASE + ".properties",
            };
        }
        return new String[] {
                BUNDLE_BASE + "_" + lang + ".properties",
                BUNDLE_BASE + ".properties",
        };
    }

    /** Reads a UTF-8 .properties file from the first matching on-disk location. */
    private static boolean loadInto(Properties props, String relativePath) {
        var baseDir = Path.of(System.getProperty("user.dir"));
        Path[] candidates = {
                baseDir.resolve("src/main/resources").resolve(relativePath),
                baseDir.resolve("resources").resolve(relativePath),
                baseDir.resolve("bin").resolve(relativePath),
                baseDir.resolve(relativePath),
        };
        for (var candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                try (InputStream in = Files.newInputStream(candidate);
                        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    props.load(reader);
                    return true;
                } catch (IOException ex) {
                    System.err.println("I18n: failed to read " + candidate + " (" + ex.getMessage() + ")");
                }
            }
        }
        return false;
    }
}