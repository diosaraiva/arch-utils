package com.diosaraiva.plantumlgui.service;

import java.util.Optional;

public enum PlantUmlFormat {

    PNG("png", true),
    SVG("svg", true),
    PUML("puml", false);

    private final String extension;
    private final boolean needsJar;

    PlantUmlFormat(String extension, boolean needsJar) {
        this.extension = extension;
        this.needsJar = needsJar;
    }

    public String extension() { return extension; }

    public boolean needsJar() { return needsJar; }

    public String cliFlag() { return "-t" + extension; }

    public static Optional<PlantUmlFormat> fromExtension(String ext) {
        if (ext == null) return Optional.empty();
        var normalized = ext.toLowerCase().strip();
        for (var format : values()) {
            if (format.extension.equals(normalized)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }
}
