package com.diosaraiva.plantumlgui.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Locates and executes an external JAR in a child JVM, capturing its output.
public final class JarUtils {

    private JarUtils() { }

    public static File extractJar(String resourcePath) throws IOException {
        // Prefer a real file on disk (exploded classes or source tree)...
        Optional<Path> onDisk = ResourceLocator.find(resourcePath);
        if (onDisk.isPresent()) {
            return onDisk.get().toFile();
        }
        // ...otherwise the resource is packaged inside our own JAR: copy it to a temp file.
        try (InputStream in = JarUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found on classpath or filesystem: " + resourcePath);
            }
            Path temp = Files.createTempFile(resourcePath.replaceAll(".*/", "") + "-", ".jar");
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            temp.toFile().deleteOnExit();
            return temp.toFile();
        }
    }

    // Runs `java [jvmOptions] -jar <jar> [args]` and blocks until it exits.
    public static JarRunResult runJar(File jar, File workingDir, List<String> jvmOptions, String... args)
            throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("java"));
        if (jvmOptions != null) { cmd.addAll(jvmOptions); }
        cmd.add("-jar");
        cmd.add(jar.getAbsolutePath());
        cmd.addAll(Arrays.asList(args));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (workingDir != null) { pb.directory(workingDir); }

        Process process = pb.start();
        // Both pipes must be drained concurrently or the child blocks on a full buffer.
        StreamCollector out = new StreamCollector(process.getInputStream());
        StreamCollector err = new StreamCollector(process.getErrorStream());
        Thread outThread = Thread.ofVirtual().name("jar-stdout").start(out);
        Thread errThread = Thread.ofVirtual().name("jar-stderr").start(err);
        int exit = process.waitFor();
        outThread.join();
        errThread.join();
        return new JarRunResult(exit, out.text(), err.text());
    }

    public record JarRunResult(int exitCode, String stdout, String stderr) {

        public boolean isSuccess() { return exitCode == 0; }

        public String combinedOutput() {
            return Stream.of(stdout, stderr)
                    .filter(s -> !s.isBlank())
                    .map(String::strip)
                    .collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private static final class StreamCollector implements Runnable {
        private final InputStream in;
        private final StringBuilder sb = new StringBuilder();

        StreamCollector(InputStream in) { this.in = in; }

        @Override
        public void run() {
            try {
                sb.append(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException ex) {
                sb.append(ex.getMessage());
            }
        }

        String text() { return sb.toString(); }
    }
}