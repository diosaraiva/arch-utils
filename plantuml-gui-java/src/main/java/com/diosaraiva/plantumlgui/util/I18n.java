package com.diosaraiva.plantumlgui.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

// UTF-8 message bundles under resources/i18n; every user-visible string goes through I18n.get.
public final class I18n {

    private static final Logger LOG = Logger.getLogger(I18n.class.getName());

    private static final String BUNDLE_DIR = "i18n";
    private static final String BUNDLE_BASE = "messages";

    public static final Locale EN_US = Locale.forLanguageTag("en-US");
    public static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    public static final Locale ES_ES = Locale.forLanguageTag("es-ES");

    private static Locale current = EN_US;
    private static Properties messages = load(current);

    private I18n() { }

    public static Locale getLocale() { return current; }

    public static void setLocale(Locale locale) {
        current = locale == null ? EN_US : locale;
        messages = load(current);
    }

    // Missing keys surface as !key! instead of throwing, so the UI never breaks on a typo.
    public static String get(String key) {
        return messages.getProperty(key, "!" + key + "!");
    }

    public static String get(String key, Object... args) {
        return MessageFormat.format(get(key), args);
    }

    // Most specific bundle wins: messages_ll_CC -> messages_ll -> messages.
    private static Properties load(Locale locale) {
        var props = new Properties();
        for (String name : candidateFileNames(locale)) {
            if (loadInto(props, BUNDLE_DIR + "/" + name)) {
                return props;
            }
        }
        LOG.warning(() -> "I18n: no messages file found for locale " + locale);
        return props;
    }

    private static List<String> candidateFileNames(Locale locale) {
        String lang = locale.getLanguage();
        String country = locale.getCountry();
        String base = BUNDLE_BASE + "_" + lang;
        return country.isEmpty()
                ? List.of(base + ".properties", BUNDLE_BASE + ".properties")
                : List.of(base + "_" + country + ".properties", base + ".properties",
                        BUNDLE_BASE + ".properties");
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
            LOG.warning(() -> "I18n: failed to read " + relativePath + " (" + ex.getMessage() + ")");
            return false;
        }
    }
}