package com.diosaraiva.plantumlgui.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public final class ResourceLocator {

    private static final Logger LOG = Logger.getLogger(ResourceLocator.class.getName());

    private static final List<String> RESOURCE_ROOTS = List.of(
            "src/main/resources",
            "resources",
            "bin",
            "");

    private ResourceLocator() { }

    public static InputStream openStream(String resourcePath) throws IOException {
        return tryOpenStream(resourcePath)
                .orElseThrow(() -> new IOException(
                        "Resource not found: " + resourcePath
                        + " (searched classpath and: "
                        + String.join(", ", searchedLocations(resourcePath)) + ")"));
    }

    public static Optional<InputStream> tryOpenStream(String resourcePath) {
        InputStream classpath = ResourceLocator.class.getClassLoader()
                .getResourceAsStream(resourcePath);
        if (classpath != null) {
            return Optional.of(classpath);
        }
        Optional<Path> onDisk = findOnFilesystem(resourcePath);
        if (onDisk.isPresent()) {
            Path file = onDisk.get();
            try {
                return Optional.of(Files.newInputStream(file));
            } catch (IOException ex) {
                LOG.warning(() -> "Failed to open " + file.toAbsolutePath()
                        + ": " + ex.getMessage());
            }
        }
        LOG.fine(() -> "Resource not found: " + resourcePath
                + " (searched classpath and: "
                + String.join(", ", searchedLocations(resourcePath)) + ")");
        return Optional.empty();
    }

    public static Optional<Path> find(String resourcePath) {
        URL url = ResourceLocator.class.getClassLoader().getResource(resourcePath);
        if (url != null && "file".equals(url.getProtocol())) {
            try {
                Path path = Path.of(url.toURI());
                return Optional.of(path);
            } catch (URISyntaxException ex) {
                LOG.warning(() -> "Invalid classpath URL for " + resourcePath
                        + ": " + ex.getMessage());
            }
        }
        Optional<Path> onDisk = findOnFilesystem(resourcePath);
        return onDisk;
    }

    private static Optional<Path> findOnFilesystem(String resourcePath) {
        for (Path dir = workingDir(); dir != null; dir = dir.getParent()) {
            for (String root : RESOURCE_ROOTS) {
                Path candidate = resolve(dir, root, resourcePath);
                if (Files.exists(candidate)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    private static List<String> searchedLocations(String resourcePath) {
        List<String> searched = new ArrayList<>();
        for (Path dir = workingDir(); dir != null; dir = dir.getParent()) {
            for (String root : RESOURCE_ROOTS) {
                searched.add(resolve(dir, root, resourcePath).toString());
            }
        }
        return searched;
    }

    private static Path resolve(Path dir, String root, String resourcePath) {
        return root.isEmpty()
                ? dir.resolve(resourcePath)
                : dir.resolve(root).resolve(resourcePath);
    }

    private static Path workingDir() {
        return Path.of(System.getProperty("user.dir")).toAbsolutePath();
    }
}
