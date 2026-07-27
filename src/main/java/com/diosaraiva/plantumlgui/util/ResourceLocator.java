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

/**
 * Centralized resource lookup used by the whole application.
 *
 * <p>Resolution strategy for a classpath-style resource path (e.g.
 * {@code "i18n/messages_en_US.properties"} or {@code "plantuml/samples"}):</p>
 * <ol>
 *   <li>Try the classpath first (works from Eclipse and from a packaged JAR).</li>
 *   <li>If not found, walk upward from the current working directory until the
 *       project root is reached, probing the known resource roots at each level
 *       (this makes {@code java Main.java} work straight from the source tree,
 *       with no build step, classpath entries, or VM arguments).</li>
 * </ol>
 *
 * <p>The upward walk is what lets the application run when the working directory
 * is deep inside the package tree (e.g.
 * {@code src/main/java/com/diosaraiva/plantumlgui}); it keeps ascending until a
 * directory that actually contains the requested resource is found.</p>
 */
public final class ResourceLocator {

    private static final Logger LOG = Logger.getLogger(ResourceLocator.class.getName());

    /**
     * Resource roots probed at every directory level during the filesystem walk,
     * relative to each candidate project directory. An empty string means the
     * candidate directory itself. Order defines precedence.
     */
    private static final List<String> RESOURCE_ROOTS = List.of(
            "src/main/resources",
            "resources",
            "bin",
            "");

    private ResourceLocator() { }

    /**
     * Opens a stream for the resource, throwing if it cannot be found anywhere.
     *
     * @param resourcePath classpath-style, {@code /}-separated resource path
     * @return an open {@link InputStream}; caller is responsible for closing it
     * @throws IOException if the resource is not found on the classpath or disk
     */
    public static InputStream openStream(String resourcePath) throws IOException {
        return tryOpenStream(resourcePath)
                .orElseThrow(() -> new IOException(
                        "Resource not found: " + resourcePath
                        + " (searched classpath and: "
                        + String.join(", ", searchedLocations(resourcePath)) + ")"));
    }

    /**
     * Attempts to open a stream for the resource without throwing.
     *
     * @param resourcePath classpath-style, {@code /}-separated resource path
     * @return the stream if found on the classpath or disk, otherwise empty
     */
    public static Optional<InputStream> tryOpenStream(String resourcePath) {
        InputStream classpath = ResourceLocator.class.getClassLoader()
                .getResourceAsStream(resourcePath);
        if (classpath != null) {
            LOG.fine(() -> "Loaded resource from classpath: " + resourcePath);
            return Optional.of(classpath);
        }
        Optional<Path> onDisk = findOnFilesystem(resourcePath);
        if (onDisk.isPresent()) {
            Path file = onDisk.get();
            LOG.info(() -> "Loaded resource '" + resourcePath
                    + "' from filesystem: " + file.toAbsolutePath());
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

    /**
     * Resolves the resource to a real filesystem {@link Path}, if one exists.
     *
     * <p>Prefers a {@code file:} URL on the classpath (Eclipse / exploded
     * classes), and otherwise falls back to the upward filesystem walk. Resources
     * packaged inside a JAR are intentionally not returned here, since they have
     * no addressable path on disk.</p>
     *
     * @param resourcePath classpath-style, {@code /}-separated resource path
     * @return the path (which may denote a file or a directory) if found on disk
     */
    public static Optional<Path> find(String resourcePath) {
        URL url = ResourceLocator.class.getClassLoader().getResource(resourcePath);
        if (url != null && "file".equals(url.getProtocol())) {
            try {
                Path path = Path.of(url.toURI());
                LOG.fine(() -> "Resolved resource '" + resourcePath
                        + "' from classpath: " + path.toAbsolutePath());
                return Optional.of(path);
            } catch (URISyntaxException ex) {
                LOG.warning(() -> "Invalid classpath URL for " + resourcePath
                        + ": " + ex.getMessage());
            }
        }
        Optional<Path> onDisk = findOnFilesystem(resourcePath);
        onDisk.ifPresent(path -> LOG.info(() -> "Resolved resource '" + resourcePath
                + "' from filesystem: " + path.toAbsolutePath()));
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
