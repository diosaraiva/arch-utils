package com.diosaraiva.plantumlgui.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

public final class I18n {

    private static final Logger LOG = Logger.getLogger(I18n.class.getName());

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

    private static Properties load(Locale locale) {
        var props = new Properties();
        for (var name : candidateFileNames(locale)) {
            if (loadInto(props, BUNDLE_DIR + "/" + name)) {
                return props;
            }
        }
        LOG.warning(() -> "I18n: no messages file found on classpath or disk for locale " + locale);
        return props;
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

    private static boolean loadInto(Properties props, String relativePath) {
        Optional<InputStream> stream = ResourceLocator.tryOpenStream(relativePath);
        if (stream.isEmpty()) {
            return false;
        }
        try (InputStream in = stream.get();
                Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            props.load(reader);
            return true;
        } catch (IOException ex) {
            LOG.warning(() -> "I18n: failed to read " + relativePath
                    + " (" + ex.getMessage() + ")");
            return false;
        }
    }
}
