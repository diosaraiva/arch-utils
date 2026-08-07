package com.diosaraiva.plantumlgui.service;

import java.io.IOException;

import com.diosaraiva.plantumlgui.util.ResourceLocator;

// Reads the bundled .puml samples shown in the Samples tab.
public final class SampleLoader {

    private static final String SAMPLES_DIR = "plantuml/samples";

    private SampleLoader() { }

    public static String load(String fileName) throws IOException {
        return ResourceLocator.readString(SAMPLES_DIR + "/" + fileName);
    }
}