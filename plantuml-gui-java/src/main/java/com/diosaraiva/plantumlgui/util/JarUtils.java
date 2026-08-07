package com.diosaraiva.plantumlgui.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class JarUtils {

    private JarUtils() { }

    public static File extractJar(String resourcePath) throws IOException {

        // Prefer a real file on disk: a 'file:' classpath URL (Eclipse / exploded
        // classes) or the upward filesystem walk (running from the source tree).
        Optional<Path> onDisk = ResourceLocator.find(resourcePath);
        if (onDisk.isPresent()) {
            return onDisk.get().toFile();
        }

        // Otherwise the resource is packaged inside a JAR: copy it to a temp file.
        try (InputStream in = JarUtils.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (in != null) {
                String baseName = resourcePath.replaceAll(".*/", "");
                Path temp = Files.createTempFile(baseName + "-", ".jar");
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
                temp.toFile().deleteOnExit();
                return temp.toFile();
            }
        }

        throw new IOException("Resource not found on classpath or filesystem: "
                + resourcePath);
    }

    public static int runJar(File jar, File workingDir,
                             List<String> jvmOptions, String... args)
            throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        if (jvmOptions != null) { cmd.addAll(jvmOptions); }
        cmd.add("-jar");
        cmd.add(jar.getAbsolutePath());
        cmd.addAll(Arrays.asList(args));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (workingDir != null) { pb.directory(workingDir); }

        Process process = pb.start();
        Thread out = pump(process.getInputStream(), System.out);
        Thread err = pump(process.getErrorStream(), System.err);
        out.start();
        err.start();
        int exit = process.waitFor();
        out.join();
        err.join();
        return exit;
    }

    private static Thread pump(InputStream in, java.io.PrintStream target) {
        var thread = new Thread(() -> {
            try {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    target.write(buffer, 0, read);
                }
                target.flush();
            } catch (IOException ignored) {

            }
        }, "jar-io-pump");
        thread.setDaemon(true);
        return thread;
    }

    public static final class JarRunResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        public JarRunResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public int exitCode() { return exitCode; }

        public String stdout() { return stdout; }

        public String stderr() { return stderr; }

        public String combinedOutput() {
            StringBuilder sb = new StringBuilder();
            if (!stdout.isBlank()) { sb.append(stdout.strip()).append(System.lineSeparator()); }
            if (!stderr.isBlank()) { sb.append(stderr.strip()).append(System.lineSeparator()); }
            return sb.toString();
        }
    }

    public static JarRunResult runJarCapture(File jar, File workingDir,
                                             List<String> jvmOptions, String... args)
            throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        if (jvmOptions != null) { cmd.addAll(jvmOptions); }
        cmd.add("-jar");
        cmd.add(jar.getAbsolutePath());
        cmd.addAll(Arrays.asList(args));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (workingDir != null) { pb.directory(workingDir); }

        Process process = pb.start();

        StreamCollector out = new StreamCollector(process.getInputStream());
        StreamCollector err = new StreamCollector(process.getErrorStream());
        Thread outThread = new Thread(out, "jar-stdout");
        Thread errThread = new Thread(err, "jar-stderr");
        outThread.start();
        errThread.start();
        int exit = process.waitFor();
        outThread.join();
        errThread.join();
        return new JarRunResult(exit, out.text(), err.text());
    }

    private static final class StreamCollector implements Runnable {
        private final InputStream in;
        private final StringBuilder sb = new StringBuilder();

        StreamCollector(InputStream in) { this.in = in; }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    sb.append(new String(buffer, 0, read, java.nio.charset.StandardCharsets.UTF_8));
                }
            } catch (IOException ignored) {

            }
        }

        String text() { return sb.toString(); }
    }
}
