package com.diosaraiva.plantumlgui.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.diosaraiva.plantumlgui.util.ResourceLocator;

public final class SampleLoader {

    private static final String SAMPLES_RESOURCE = "plantuml/samples";

    private SampleLoader() { }

    public static String load(String fileName) throws IOException {
        try (InputStream in = ResourceLocator.openStream(SAMPLES_RESOURCE + "/" + fileName)) {
            return readStream(in);
        }
    }

    private static String readStream(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return readAll(reader);
        }
    }

    private static String readAll(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(line);
        }
        return sb.toString();
    }
}