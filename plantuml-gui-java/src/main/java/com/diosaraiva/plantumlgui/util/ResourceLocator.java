package com.diosaraiva.plantumlgui.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

// Single entry point for bundled resources: classpath first, then disk walking up from the working dir.
public final class ResourceLocator {

    private static final Logger LOG = Logger.getLogger(ResourceLocator.class.getName());

    // Relative roots probed at every level of the upward walk ("" = the level itself).
    private static final List<String> ROOTS = List.of("src/main/resources", "resources", "bin", "");

    private ResourceLocator() { }

    public static InputStream openStream(String resourcePath) throws IOException {
        return tryOpenStream(resourcePath).orElseThrow(
                () -> new IOException("Resource not found on classpath or disk: " + resourcePath));
    }

    public static Optional<InputStream> tryOpenStream(String resourcePath) {
        InputStream classpath = ResourceLocator.class.getClassLoader().getResourceAsStream(resourcePath);
        return classpath != null ? Optional.of(classpath) : find(resourcePath).flatMap(ResourceLocator::open);
    }

    // Canonical way to read a bundled text resource.
    public static String readString(String resourcePath) throws IOException {
        try (InputStream in = openStream(resourcePath)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // Classpath URL when it points to a real file, otherwise the first match on disk.
    public static Optional<Path> find(String resourcePath) {
        URL url = ResourceLocator.class.getClassLoader().getResource(resourcePath);
        if (url != null && "file".equals(url.getProtocol())) {
            try {
                return Optional.of(Path.of(url.toURI()));
            } catch (URISyntaxException ex) {
                LOG.warning(() -> "Invalid classpath URL for " + resourcePath + ": " + ex.getMessage());
            }
        }
        return candidates(resourcePath).filter(Files::exists).findFirst();
    }

    private static Stream<Path> candidates(String resourcePath) {
        Path workingDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        return Stream.iterate(workingDir, p -> p != null, Path::getParent)
                .flatMap(dir -> ROOTS.stream().map(root ->
                        root.isEmpty() ? dir.resolve(resourcePath) : dir.resolve(root).resolve(resourcePath)));
    }

    private static Optional<InputStream> open(Path file) {
        try {
            return Optional.of(Files.newInputStream(file));
        } catch (IOException ex) {
            LOG.warning(() -> "Failed to open " + file + ": " + ex.getMessage());
            return Optional.empty();
        }
    }
}