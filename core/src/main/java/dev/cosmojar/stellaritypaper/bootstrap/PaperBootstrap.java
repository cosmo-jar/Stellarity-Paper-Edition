package dev.cosmojar.stellaritypaper.bootstrap;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PaperBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        boolean nullscapeCompatEnabled = isNullscapeCompatibilityEnabled(context.getDataDirectory());
        Path datapacksDir = getMainWorldDatapacksFolder();
        handleNullscapeDatapack(nullscapeCompatEnabled, datapacksDir);

        context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, event -> {
            try {
                URI uriPaintings = Objects.requireNonNull(
                    PaperBootstrap.class.getResource("/stellarity_paintings_pack")
                ).toURI();
                event.registrar().discoverPack(uriPaintings, "stellarity_paintings");
            } catch (Exception e) {
                throw new RuntimeException("Failed to register custom paintings datapack", e);
            }
            
            try {
                URI uriStructures = Objects.requireNonNull(
                    PaperBootstrap.class.getResource("/stellarity_structures_pack")
                ).toURI();
                event.registrar().discoverPack(uriStructures, "stellarity_structures");
            } catch (Exception e) {
                throw new RuntimeException("Failed to register custom structures datapack", e);
            }

            try {
                URI uriMobs = Objects.requireNonNull(
                    PaperBootstrap.class.getResource("/stellarity_mobs_pack")
                ).toURI();
                event.registrar().discoverPack(uriMobs, "stellarity_mobs");
            } catch (Exception e) {
                throw new RuntimeException("Failed to register custom mob variants datapack", e);
            }

            try {
                URI uriAdvancements = Objects.requireNonNull(
                    PaperBootstrap.class.getResource("/stellarity_advancements_pack")
                ).toURI();
                event.registrar().discoverPack(uriAdvancements, "stellarity_advancements");
            } catch (Exception e) {
                throw new RuntimeException("Failed to register custom advancements datapack", e);
            }
        });
    }

    private boolean isNullscapeCompatibilityEnabled(Path dataDir) {
        Path configFile = dataDir.resolve("config.yml");
        if (!Files.exists(configFile)) {
            try {
                Files.createDirectories(dataDir);
                try (InputStream in = PaperBootstrap.class.getResourceAsStream("/config.yml")) {
                    if (in != null) {
                        Files.copy(in, configFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (Exception e) {
                System.err.println("[Stellarity Paper] Failed to create default config.yml: " + e.getMessage());
            }
        }

        if (Files.exists(configFile)) {
            try (BufferedReader reader = Files.newBufferedReader(configFile)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("nullscape-compatibility:")) {
                        String[] parts = trimmed.split(":", 2);
                        if (parts.length > 1) {
                            String val = parts[1].trim().toLowerCase();
                            return val.equals("true") || val.startsWith("true");
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[Stellarity Paper] Failed to read nullscape-compatibility from config.yml: " + e.getMessage());
            }
        }
        return false;
    }

    private Path getMainWorldDatapacksFolder() {
        String levelName = "world";
        File serverProps = new File("server.properties");
        if (serverProps.exists()) {
            Properties properties = new Properties();
            try (FileInputStream fis = new FileInputStream(serverProps)) {
                properties.load(fis);
                levelName = properties.getProperty("level-name", "world");
            } catch (Exception e) {
                System.err.println("[Stellarity Paper] Failed to read level-name from server.properties: " + e.getMessage());
            }
        }
        return Path.of(levelName, "datapacks");
    }

    private void handleNullscapeDatapack(boolean enabled, Path datapacksDir) {
        Path targetZip = datapacksDir.resolve("zzz_stellarity_x_nullscape.zip");
        if (!enabled) {
            try {
                if (Files.exists(targetZip)) {
                    Files.deleteIfExists(targetZip);
                    System.out.println("[Stellarity Paper] Nullscape compatibility is disabled. Removed: " + targetZip);
                }
            } catch (Exception e) {
                System.err.println("[Stellarity Paper] Failed to remove unused compatibility datapack: " + e.getMessage());
            }
            return;
        }

        try {
            Files.createDirectories(datapacksDir);
            URL resourceUrl = PaperBootstrap.class.getResource("/Zstellarity_x_nullscape_pack");
            if (resourceUrl == null) {
                resourceUrl = PaperBootstrap.class.getResource("/stellarity_x_nullscape_pack");
            }
            if (resourceUrl == null) {
                System.err.println("[Stellarity Paper] Resource Zstellarity_x_nullscape_pack not found in classpath!");
                return;
            }

            URI uri = resourceUrl.toURI();
            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(
                    Files.newOutputStream(targetZip, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)))) {
                
                if ("jar".equalsIgnoreCase(uri.getScheme())) {
                    String jarPath = uri.getSchemeSpecificPart();
                    if (jarPath.startsWith("file:")) {
                        jarPath = jarPath.substring(5);
                    }
                    int bangIdx = jarPath.indexOf("!");
                    if (bangIdx != -1) {
                        jarPath = jarPath.substring(0, bangIdx);
                    }
                    try (JarFile jar = new JarFile(new File(jarPath))) {
                        Enumeration<JarEntry> entries = jar.entries();
                        String prefix1 = "Zstellarity_x_nullscape_pack/";
                        String prefix2 = "stellarity_x_nullscape_pack/";
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            String relativeName = null;
                            if (name.startsWith(prefix1) && !name.equals(prefix1)) {
                                relativeName = name.substring(prefix1.length());
                            } else if (name.startsWith(prefix2) && !name.equals(prefix2)) {
                                relativeName = name.substring(prefix2.length());
                            }

                            if (relativeName != null && !relativeName.isEmpty()) {
                                if (entry.isDirectory()) {
                                    if (!relativeName.endsWith("/")) {
                                        relativeName += "/";
                                    }
                                    zos.putNextEntry(new ZipEntry(relativeName));
                                    zos.closeEntry();
                                } else {
                                    zos.putNextEntry(new ZipEntry(relativeName));
                                    try (InputStream is = jar.getInputStream(entry)) {
                                        is.transferTo(zos);
                                    }
                                    zos.closeEntry();
                                }
                            }
                        }
                    }
                } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                    Path rootDir = Path.of(uri);
                    try (var stream = Files.walk(rootDir)) {
                        stream.forEach(source -> {
                            String relativePath = rootDir.relativize(source).toString().replace("\\", "/");
                            if (relativePath.isEmpty()) return;
                            try {
                                if (Files.isDirectory(source)) {
                                    if (!relativePath.endsWith("/")) relativePath += "/";
                                    zos.putNextEntry(new ZipEntry(relativePath));
                                    zos.closeEntry();
                                } else {
                                    zos.putNextEntry(new ZipEntry(relativePath));
                                    try (InputStream is = Files.newInputStream(source)) {
                                        is.transferTo(zos);
                                    }
                                    zos.closeEntry();
                                }
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
                    }
                }
            }
            System.out.println("[Stellarity Paper] Nullscape compatibility is enabled! Exported datapack to: " + targetZip.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("[Stellarity Paper] Failed to export Nullscape compatibility datapack: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

