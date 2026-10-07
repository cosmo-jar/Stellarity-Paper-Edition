package dev.cosmojar.stellaritypaper.command;

import dev.cosmojar.stellaritypaper.StellarityPaperPlugin;
import dev.cosmojar.stellaritypaper.text.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.nbt.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public final class UninstallService {

    private static final String[] BYE_STELLARITY_ASCII = {
            "  ___              ___ _       _ _            _ _         ",
            " | _ )_  _ ___    / __| |_ ___| | |__ _ _ _ (_) |_ _  _   ",
            " | _ \\ || / -_)   \\__ \\  _/ -_) | / _` | '_|| |  _| || |  ",
            " |___/\\_, \\___|   |___/\\__\\___|_|_\\__,_|_|  |_|\\__|\\_, |  ",
            "      |__/                                         |__/   "
    };

    private static final Set<String> CUSTOM_NAMESPACES = Set.of(
            "stellarity",
            "awesomedungeonend",
            "far_end",
            "kohara",
            "fwaystones",
            "waystones",
            "fokastudio",
            "nullscape"
    );

    private final Plugin plugin;
    private final MessageService messageService;
    private final AtomicBoolean uninstallTriggered = new AtomicBoolean(false);
    private final AtomicBoolean asciiPrinted = new AtomicBoolean(false);
    private final AtomicBoolean shutdownHookRegistered = new AtomicBoolean(false);
    private final AtomicBoolean shutdownHookExecuted = new AtomicBoolean(false);
    private final Set<File> pendingDatapacksToDelete = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Set<String> scheduledDeletions = ConcurrentHashMap.newKeySet();
    private volatile File pluginJarFile;

    public UninstallService(final Plugin plugin, final MessageService messageService) {
        this.plugin = plugin;
        this.messageService = messageService;
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase(Locale.ROOT).contains("win");
    }

    private File resolvePluginJar() {
        try {
            if (plugin instanceof StellarityPaperPlugin srp) {
                File f = srp.getPluginJarFile();
                if (f != null && f.exists()) return f;
            }
        } catch (Throwable ignored) {}
        try {
            return Paths.get(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toFile();
        } catch (Throwable ignored) {}
        return null;
    }

    private static Component parseAsciiLine(String line) {
        String escaped = line.replace("\\", "\\\\");
        return MiniMessage.miniMessage().deserialize("<gradient:#c084fc:#f472b6>" + escaped + "</gradient>");
    }

    private void sendConsole(final Component component) {
        try {
            if (Bukkit.getServer() != null) {
                Bukkit.getConsoleSender().sendMessage(component);
                return;
            }
        } catch (Throwable ignored) {
        }

        try {
            System.out.println(net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer.ansi().serialize(component));
            System.out.flush();
        } catch (Throwable fallback) {
            try {
                System.out.println(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component));
                System.out.flush();
            } catch (Throwable fallback2) {
                plugin.getLogger().info(component.toString());
            }
        }
    }

    private void notifySenderAndConsole(final CommandSender sender, final Component component) {
        sender.sendMessage(component);
        if (!(sender instanceof ConsoleCommandSender)) {
            sendConsole(component);
        }
    }

    private void onStopTriggered(final CommandSender sender) {
        if (!uninstallTriggered.get()) return;
        if (asciiPrinted.compareAndSet(false, true)) {
            printAsciiFarewell();
            if (sender instanceof Player p && p.isOnline()) {
                p.sendMessage(messageService.message("command.uninstall.console-finished"));
                p.sendMessage(messageService.message("command.uninstall.console-jar-hint"));
            }
        }
    }

    private void printAsciiFarewell() {
        sendConsole(Component.empty());
        for (String line : BYE_STELLARITY_ASCII) {
            sendConsole(parseAsciiLine(line));
        }
        sendConsole(Component.empty());
        sendConsole(messageService.message("command.uninstall.console-finished"));
        sendConsole(messageService.message("command.uninstall.console-jar-hint"));
        sendConsole(Component.empty());
    }

    public void executeUninstall(final CommandSender sender) {
        uninstallTriggered.set(true);
        this.pluginJarFile = resolvePluginJar();

        try {
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler(priority = EventPriority.LOWEST)
                public void onServerCommand(ServerCommandEvent event) {
                    String cmd = event.getCommand().trim().toLowerCase();
                    if (cmd.equals("stop") || cmd.startsWith("stop ")
                            || cmd.equals("minecraft:stop") || cmd.startsWith("minecraft:stop ")) {
                        onStopTriggered(sender);
                    }
                }

                @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
                public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
                    String msg = event.getMessage().trim().toLowerCase();
                    if (msg.equals("/stop") || msg.startsWith("/stop ")
                            || msg.equals("/minecraft:stop") || msg.startsWith("/minecraft:stop ")) {
                        onStopTriggered(sender);
                    }
                }
            }, plugin);
        } catch (Throwable ignored) {
        }

        notifySenderAndConsole(sender, messageService.message("command.uninstall.start"));

        try {
            List<File> nullscapePacks = findNullscapeDatapackFiles();
            pendingDatapacksToDelete.addAll(nullscapePacks);

            notifySenderAndConsole(sender, messageService.message("command.uninstall.datapacks-disabled"));
        } catch (Exception e) {
            notifySenderAndConsole(sender, messageService.message("command.uninstall.datapacks-error", Map.of("error", e.getMessage())));
        }

        World overworld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (overworld != null) {
            Location spawn = overworld.getSpawnLocation();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().getEnvironment() == World.Environment.THE_END) {
                    player.teleport(spawn);
                }
            }
        }

        try {
            for (World world : Bukkit.getWorlds()) {
                if (world.getEnvironment() == World.Environment.THE_END) {
                    try {
                        dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().resetDragonFightForUninstall(world);
                    } catch (Throwable ignored) {}

                    try {
                        List<File> extraFiles = dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().getExtraFilesToDeleteOnUninstall(world);
                        if (extraFiles != null && !extraFiles.isEmpty()) {
                            pendingDatapacksToDelete.addAll(extraFiles);
                            for (File df : extraFiles) {
                                deletePhysically(df, false);
                            }
                        }
                    } catch (Throwable ignored) {}

                    for (org.bukkit.entity.Entity entity : world.getEntities()) {
                        if (entity.getType() == EntityType.END_CRYSTAL) {
                            Location loc = entity.getLocation();
                            if (Math.abs(loc.getBlockX()) <= 256 && Math.abs(loc.getBlockZ()) <= 256) {
                                entity.remove();
                            }
                        } else if (entity.getType() == EntityType.ENDER_DRAGON) {
                            entity.remove();
                        }
                    }
                }
                for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                    for (BlockState state : chunk.getTileEntities()) {
                        if (state.getType() == Material.STRUCTURE_BLOCK) {
                            state.getBlock().setType(Material.AIR, false);
                        }
                    }
                    world.unloadChunk(chunk);
                }
                world.save();
            }
            notifySenderAndConsole(sender, messageService.message("command.uninstall.saved-chunks"));
        } catch (Exception e) {
            notifySenderAndConsole(sender, messageService.message("command.uninstall.save-error", Map.of("error", e.getMessage())));
        }

        final List<File> levelDatFiles = new ArrayList<>();
        final List<File> regionDirs = new ArrayList<>();

        try {
            for (World world : Bukkit.getWorlds()) {
                scanWorldFolder(world.getWorldFolder(), levelDatFiles, regionDirs, 0);
            }
            File container = Bukkit.getWorldContainer();
            if (container != null && container.exists()) {
                scanWorldFolder(container, levelDatFiles, regionDirs, 0);
            }
        } catch (Exception e) {
            notifySenderAndConsole(sender, messageService.message("command.uninstall.scan-error", Map.of("error", e.getMessage())));
        }

        final List<File> allMcaFiles = new ArrayList<>();
        for (File regionDir : regionDirs) {
            File[] mcaFiles = regionDir.listFiles((dir, name) -> name.endsWith(".mca"));
            if (mcaFiles != null) {
                for (File mcaFile : mcaFiles) {
                    if (!allMcaFiles.contains(mcaFile)) {
                        allMcaFiles.add(mcaFile);
                    }
                }
            }
        }

        final Map<String, String> scanInfo = Map.of(
                "leveldats", String.valueOf(levelDatFiles.size()),
                "mcas", String.valueOf(allMcaFiles.size())
        );
        notifySenderAndConsole(sender, messageService.message("command.uninstall.scan-found", scanInfo));

        registerShutdownHook(levelDatFiles, regionDirs, sender);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            runFullNbtCleanup(levelDatFiles, regionDirs, allMcaFiles, sender);
            notifySenderAndConsole(sender, messageService.message("command.uninstall.hook-success"));
            notifySenderAndConsole(sender, messageService.message("command.uninstall.stop-hint"));
        });
    }

    private void runFullNbtCleanup(List<File> levelDatFiles, List<File> regionDirs, List<File> allMcaFiles, CommandSender sender) {
        for (File levelDatFile : levelDatFiles) {
            try {
                if (levelDatFile.exists()) {
                    fixLevelDatNMS(levelDatFile);
                    sendConsole(messageService.message("command.uninstall.level-dat-cleaned", Map.of("world", levelDatFile.getParentFile().getName())));
                }
            } catch (Exception e) {
                sendConsole(messageService.message("command.uninstall.level-dat-error", Map.of("file", levelDatFile.getName(), "error", e.getMessage())));
            }
        }

        resetEndMainIsland(regionDirs);

        int totalMcaCleaned = 0;
        int totalChunksCleaned = 0;
        int processed = 0;
        final int totalFiles = allMcaFiles.size();
        final int progressStep = totalFiles <= 50 ? 5 : Math.max(10, totalFiles / 10);

        for (File mcaFile : allMcaFiles) {
            processed++;
            int chunks = cleanRegionFileNMS(mcaFile);
            if (chunks > 0) {
                totalMcaCleaned++;
                totalChunksCleaned += chunks;
                sendConsole(messageService.message("command.uninstall.mca-file-cleaned", Map.of(
                        "file", mcaFile.getName(),
                        "chunks", String.valueOf(chunks)
                )));
            }

            if (totalFiles > 0 && (processed % progressStep == 0 || processed == totalFiles)) {
                int percent = (int) Math.round(((double) processed / totalFiles) * 100.0);
                final Map<String, String> progressMap = Map.of(
                        "current", String.valueOf(processed),
                        "total", String.valueOf(totalFiles),
                        "percent", String.valueOf(percent),
                        "chunks", String.valueOf(totalChunksCleaned)
                );
                Component progressComp = messageService.message("command.uninstall.mca-progress", progressMap);
                sendConsole(progressComp);
                if (sender instanceof Player p && p.isOnline()) {
                    p.sendMessage(progressComp);
                }
            }
        }
        sendConsole(messageService.message("command.uninstall.mca-summary", Map.of(
                "files", String.valueOf(totalMcaCleaned),
                "chunks", String.valueOf(totalChunksCleaned)
        )));
    }

    private void runShutdownNbtCleanup(List<File> levelDatFiles, List<File> regionDirs) {
        for (File levelDatFile : levelDatFiles) {
            try {
                if (levelDatFile.exists()) {
                    fixLevelDatNMS(levelDatFile);
                    sendConsole(messageService.message("command.uninstall.level-dat-cleaned", Map.of("world", levelDatFile.getParentFile().getName())));
                }
            } catch (Exception e) {
                sendConsole(messageService.message("command.uninstall.level-dat-error", Map.of("file", levelDatFile.getName(), "error", e.getMessage())));
            }
        }

        resetEndMainIsland(regionDirs);

        int totalMcaCleaned = 0;
        int totalChunksCleaned = 0;
        for (File regionDir : regionDirs) {
            File[] mcaFiles = regionDir.listFiles((dir, name) -> name.endsWith(".mca"));
            if (mcaFiles != null) {
                for (File mcaFile : mcaFiles) {
                    int chunks = cleanRegionFileNMS(mcaFile);
                    if (chunks > 0) {
                        totalMcaCleaned++;
                        totalChunksCleaned += chunks;
                        sendConsole(messageService.message("command.uninstall.mca-file-cleaned", Map.of(
                                "file", mcaFile.getName(),
                                "chunks", String.valueOf(chunks)
                        )));
                    }
                }
            }
        }
        if (totalMcaCleaned > 0) {
            sendConsole(messageService.message("command.uninstall.mca-summary", Map.of(
                    "files", String.valueOf(totalMcaCleaned),
                    "chunks", String.valueOf(totalChunksCleaned)
            )));
        }
    }

    private void registerShutdownHook(List<File> levelDatFiles, List<File> regionDirs, CommandSender sender) {
        if (shutdownHookRegistered.compareAndSet(false, true)) {
            try {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    runShutdownHookTasks(levelDatFiles, regionDirs);
                }, "Stellarity-Uninstall-ShutdownHook"));
            } catch (Exception e) {
                notifySenderAndConsole(sender, messageService.message("command.uninstall.hook-error", Map.of("error", e.getMessage())));
            }
        }
    }

    private void runShutdownHookTasks(List<File> levelDatFiles, List<File> regionDirs) {
        if (!shutdownHookExecuted.compareAndSet(false, true)) return;
        sendConsole(messageService.message("command.uninstall.console-recheck"));
        deletePendingDatapacks();
        deletePluginJar(this.pluginJarFile);
        runShutdownNbtCleanup(levelDatFiles, regionDirs);
        if (asciiPrinted.compareAndSet(false, true)) {
            printAsciiFarewell();
        }
    }

    private void deletePendingDatapacks() {
        try {
            List<File> freshPacks = findNullscapeDatapackFiles();
            pendingDatapacksToDelete.addAll(freshPacks);
        } catch (Throwable ignored) {}

        for (File pack : pendingDatapacksToDelete) {
            deletePhysically(pack, false);
        }
    }

    private void deletePluginJar(File jarFile) {
        deletePhysically(jarFile, true);
    }

    private void deletePhysically(File target, boolean isPluginJar) {
        if (target == null || !target.exists()) return;

        final String fileName = target.getName();
        final String absPath = target.getAbsolutePath();

        boolean deleted = false;
        if (target.isDirectory()) {
            deleted = deleteDirectoryRecursively(target);
        } else {
            try {
                deleted = target.delete();
            } catch (Throwable ignored) {}

            if (!deleted && isPluginJar) {
                try {
                    ClassLoader cl = plugin.getClass().getClassLoader();
                    if (cl instanceof Closeable closeable) {
                        closeable.close();
                    }
                } catch (Throwable ignored) {}

                try {
                    deleted = target.delete();
                } catch (Throwable ignored) {}
            }
        }

        if (deleted) {
            if (isPluginJar) {
                sendConsole(messageService.message("command.uninstall.jar-deleted", Map.of("file", fileName)));
            } else if (fileName.endsWith(".dat") || fileName.endsWith(".dat_old")) {
                sendConsole(messageService.message("command.uninstall.end-dragonfight-reset", Map.of("file", fileName)));
            } else {
                if (messageService.hasKey("command.uninstall.datapack-deleted")) {
                    sendConsole(messageService.message("command.uninstall.datapack-deleted", Map.of("file", fileName)));
                } else {
                    sendConsole(messageService.message("command.uninstall.jar-deleted", Map.of("file", fileName)));
                }
            }
            return;
        }

        try {
            target.deleteOnExit();
        } catch (Throwable ignored) {}

        if (scheduledDeletions.add(absPath)) {
            try {
                if (isWindows()) {
                    if (target.isDirectory()) {
                        new ProcessBuilder("cmd.exe", "/c", "timeout /t 3 /nobreak >nul & rmdir /s /q \"" + absPath + "\"").start();
                    } else {
                        new ProcessBuilder("cmd.exe", "/c", "timeout /t 3 /nobreak >nul & del /f /q \"" + absPath + "\"").start();
                    }
                } else {
                    new ProcessBuilder("sh", "-c", "sleep 3 && rm -rf '" + absPath + "'").start();
                }
            } catch (Throwable ignored) {}
        }

        if (isPluginJar) {
            sendConsole(messageService.message("command.uninstall.jar-delete-exit", Map.of("file", fileName)));
        } else if (fileName.endsWith(".dat") || fileName.endsWith(".dat_old")) {
            sendConsole(messageService.message("command.uninstall.end-dragonfight-reset", Map.of("file", fileName)));
        } else {
            if (messageService.hasKey("command.uninstall.datapack-delete-exit")) {
                sendConsole(messageService.message("command.uninstall.datapack-delete-exit", Map.of("file", fileName)));
            } else {
                sendConsole(messageService.message("command.uninstall.jar-delete-exit", Map.of("file", fileName)));
            }
        }
    }

    private static boolean deleteDirectoryRecursively(File file) {
        if (file == null || !file.exists()) return true;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectoryRecursively(child);
                }
            }
        }
        boolean deleted = false;
        try {
            deleted = file.delete();
        } catch (Throwable ignored) {}
        if (!deleted) {
            try {
                file.deleteOnExit();
            } catch (Throwable ignored) {}
        }
        return deleted;
    }

    private List<File> findAllDatapacksFolders() {
        Set<File> datapacksFolders = new LinkedHashSet<>();

        try {
            File serverProps = new File("server.properties");
            if (serverProps.exists()) {
                Properties properties = new Properties();
                try (FileInputStream fis = new FileInputStream(serverProps)) {
                    properties.load(fis);
                    String levelName = properties.getProperty("level-name", "world");
                    File mainWorldDatapacks = new File(levelName, "datapacks");
                    if (mainWorldDatapacks.exists() && mainWorldDatapacks.isDirectory()) {
                        datapacksFolders.add(mainWorldDatapacks);
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            for (World w : Bukkit.getWorlds()) {
                File folder = new File(w.getWorldFolder(), "datapacks");
                if (folder.exists() && folder.isDirectory()) {
                    datapacksFolders.add(folder);
                }
            }
        } catch (Throwable ignored) {}

        try {
            File container = Bukkit.getWorldContainer();
            if (container != null && container.exists() && container.isDirectory()) {
                File rootDatapacks = new File(container, "datapacks");
                if (rootDatapacks.exists() && rootDatapacks.isDirectory()) {
                    datapacksFolders.add(rootDatapacks);
                }
                File[] subDirs = container.listFiles(File::isDirectory);
                if (subDirs != null) {
                    for (File subDir : subDirs) {
                        File subDatapacks = new File(subDir, "datapacks");
                        if (subDatapacks.exists() && subDatapacks.isDirectory()) {
                            datapacksFolders.add(subDatapacks);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        File defaultDatapacks = new File("world", "datapacks");
        if (defaultDatapacks.exists() && defaultDatapacks.isDirectory()) {
            datapacksFolders.add(defaultDatapacks);
        }
        File currentDirDatapacks = new File("datapacks");
        if (currentDirDatapacks.exists() && currentDirDatapacks.isDirectory()) {
            datapacksFolders.add(currentDirDatapacks);
        }

        return new ArrayList<>(datapacksFolders);
    }

    private List<File> findNullscapeDatapackFiles() {
        Set<File> found = new LinkedHashSet<>();
        List<File> datapacksFolders = findAllDatapacksFolders();
        for (File dpFolder : datapacksFolders) {
            File[] files = dpFolder.listFiles();
            if (files == null) continue;
            for (File file : files) {
                String nameLower = file.getName().toLowerCase(Locale.ROOT);
                if (nameLower.contains("nullscape") || nameLower.contains("stellarity_x_nullscape")) {
                    found.add(file);
                }
            }
        }
        return new ArrayList<>(found);
    }

    private void resetEndMainIsland(List<File> regionDirs) {
        for (File regionDir : regionDirs) {
            if (!isEndRegionFolder(regionDir)) continue;

            File[] targetMcas = regionDir.listFiles((dir, name) ->
                    "r.0.0.mca".equalsIgnoreCase(name) || "r.-1.0.mca".equalsIgnoreCase(name) ||
                            "r.0.-1.mca".equalsIgnoreCase(name) || "r.-1.-1.mca".equalsIgnoreCase(name)
            );

            if (targetMcas != null) {
                for (File mcaFile : targetMcas) {
                    deleteMainIslandChunksFromMca(mcaFile);
                }
            }
        }
    }

    private static boolean isEndRegionFolder(File regionDir) {
        if (regionDir == null) return false;
        String path = regionDir.getAbsolutePath().toLowerCase();
        return path.contains("the_end") || path.contains("dim1") || path.contains("end");
    }

    private void deleteMainIslandChunksFromMca(File mcaFile) {
        if (!mcaFile.exists() || mcaFile.length() < 8192) return;
        int deletedCount = 0;

        RandomAccessFile raf = null;
        for (int retry = 0; retry < 5; retry++) {
            try {
                raf = new RandomAccessFile(mcaFile, "rw");
                break;
            } catch (IOException e) {
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
        }
        if (raf == null) return;

        try {
            byte[] header = new byte[4096];
            raf.readFully(header);

            String name = mcaFile.getName();
            String[] parts = name.split("\\.");
            int regX = Integer.parseInt(parts[1]);
            int regZ = Integer.parseInt(parts[2]);

            for (int cx = -16; cx <= 16; cx++) {
                for (int cz = -16; cz <= 16; cz++) {
                    int rX = cx >> 5;
                    int rZ = cz >> 5;
                    if (rX == regX && rZ == regZ) {
                        int localCx = cx & 31;
                        int localCz = cz & 31;
                        int idx = localCx + localCz * 32;

                        int offsetVal = ((header[idx * 4] & 0xFF) << 16) |
                                ((header[idx * 4 + 1] & 0xFF) << 8) |
                                (header[idx * 4 + 2] & 0xFF);

                        if (offsetVal != 0) {
                            header[idx * 4] = 0;
                            header[idx * 4 + 1] = 0;
                            header[idx * 4 + 2] = 0;
                            header[idx * 4 + 3] = 0;

                            raf.seek(idx * 4);
                            raf.write(new byte[]{0, 0, 0, 0});
                            deletedCount++;
                        }
                    }
                }
            }
            if (deletedCount > 0) {
                sendConsole(messageService.message("command.uninstall.end-island-reset", Map.of("file", mcaFile.getName(), "chunks", String.valueOf(deletedCount))));
            }
        } catch (Exception e) {
            sendConsole(messageService.message("command.uninstall.mca-error", Map.of("file", mcaFile.getName(), "error", e.getMessage())));
        } finally {
            try { raf.close(); } catch (IOException ignored) {}
        }
    }

    private static void scanWorldFolder(File dir, List<File> levelDatFiles, List<File> regionDirs, int depth) {
        if (dir == null || !dir.exists() || !dir.isDirectory() || depth > 4) return;

        File levelDat = new File(dir, "level.dat");
        if (levelDat.exists() && !levelDatFiles.contains(levelDat)) {
            levelDatFiles.add(levelDat);
        }

        String dirName = dir.getName();
        if (("region".equalsIgnoreCase(dirName) || "entities".equalsIgnoreCase(dirName)) && !regionDirs.contains(dir)) {
            regionDirs.add(dir);
            return;
        }

        File[] subFiles = dir.listFiles();
        if (subFiles != null) {
            for (File f : subFiles) {
                if (f.isDirectory()) {
                    String name = f.getName();
                    if ("plugins".equalsIgnoreCase(name) || "logs".equalsIgnoreCase(name)
                            || "crash-reports".equalsIgnoreCase(name) || "cache".equalsIgnoreCase(name)) {
                        continue;
                    }
                    scanWorldFolder(f, levelDatFiles, regionDirs, depth + 1);
                }
            }
        }
    }

    private void fixLevelDatNMS(File levelDatFile) throws Exception {
        if (dev.cosmojar.stellaritypaper.api.ServerVersion.getCurrent() == dev.cosmojar.stellaritypaper.api.ServerVersion.V1_21_4) {
            dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().fixLevelDat(levelDatFile);
            return;
        }
        Path path = levelDatFile.toPath();
        CompoundTag rootTag = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
        if (rootTag == null || !rootTag.contains("Data")) {
            return;
        }

        CompoundTag dataTag = rootTag.getCompound("Data").orElse(null);
        if (dataTag == null) return;

        if (dataTag.contains("DragonFight")) {
            CompoundTag dfTag = new CompoundTag();
            dfTag.putBoolean("DragonKilled", false);
            dfTag.putBoolean("PreviouslyKilled", false);
            dfTag.putBoolean("NeedsStateFix", false);
            dataTag.put("DragonFight", dfTag);
            sendConsole(messageService.message("command.uninstall.end-dragonfight-reset", Map.of("file", levelDatFile.getName())));
        }

        if (dataTag.contains("WorldGenSettings")) {
            CompoundTag worldGenTag = dataTag.getCompound("WorldGenSettings").orElse(null);
            if (worldGenTag != null && worldGenTag.contains("dimensions")) {
                CompoundTag dimensionsTag = worldGenTag.getCompound("dimensions").orElse(null);
                if (dimensionsTag != null && dimensionsTag.contains("minecraft:the_end")) {
                    CompoundTag endTag = dimensionsTag.getCompound("minecraft:the_end").orElse(null);
                    if (endTag != null && endTag.contains("generator")) {
                        CompoundTag generatorTag = endTag.getCompound("generator").orElse(null);
                        if (generatorTag != null && generatorTag.contains("biome_source")) {
                            CompoundTag biomeSourceTag = generatorTag.getCompound("biome_source").orElse(null);
                            if (biomeSourceTag != null) {
                                biomeSourceTag.putString("type", "minecraft:the_end");
                                biomeSourceTag.remove("biomes");
                            }
                        }
                    }
                }
            }
        }

        if (dataTag.contains("DataPacks")) {
            CompoundTag datapacksTag = dataTag.getCompound("DataPacks").orElse(null);

            if (datapacksTag != null && datapacksTag.contains("Enabled")) {
                ListTag enabledList = datapacksTag.getList("Enabled").orElse(null);
                if (enabledList != null) {
                    for (int i = enabledList.size() - 1; i >= 0; i--) {
                        String packName = enabledList.getString(i).orElse("");
                        if (isCustomNamespace(packName)) {
                            enabledList.remove(i);
                        }
                    }
                }
            }

            if (datapacksTag != null && datapacksTag.contains("Disabled")) {
                ListTag disabledList = datapacksTag.getList("Disabled").orElse(null);
                if (disabledList != null) {
                    for (int i = disabledList.size() - 1; i >= 0; i--) {
                        String packName = disabledList.getString(i).orElse("");
                        if (isCustomNamespace(packName)) {
                            disabledList.remove(i);
                        }
                    }
                }
            }
        }

        NbtIo.writeCompressed(rootTag, path);
    }

    private int cleanRegionFileNMS(File mcaFile) {
        if (!mcaFile.exists() || mcaFile.length() < 8192) return 0;
        int cleanedChunks = 0;

        RandomAccessFile raf = null;
        for (int retry = 0; retry < 5; retry++) {
            try {
                raf = new RandomAccessFile(mcaFile, "rw");
                break;
            } catch (IOException e) {
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
        }

        if (raf == null) {
            sendConsole(messageService.message("command.uninstall.mca-locked", Map.of("file", mcaFile.getName())));
            return 0;
        }

        try {
            byte[] header = new byte[4096];
            raf.readFully(header);

            for (int i = 0; i < 1024; i++) {
                int offsetVal = ((header[i * 4] & 0xFF) << 16) |
                        ((header[i * 4 + 1] & 0xFF) << 8) |
                        (header[i * 4 + 2] & 0xFF);
                int sectorCount = header[i * 4 + 3] & 0xFF;

                if (offsetVal == 0) continue;

                long chunkOffset = (long) offsetVal * 4096;
                raf.seek(chunkOffset);

                int length = raf.readInt();
                if (length <= 1) continue;

                byte compressionType = raf.readByte();
                byte[] compressedData = new byte[length - 1];
                raf.readFully(compressedData);

                ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
                InputStream is = (compressionType == 2) ? new InflaterInputStream(bais) : bais;
                DataInputStream dis = new DataInputStream(is);

                CompoundTag chunkTag = NbtIo.read(dis);
                dis.close();

                if (chunkTag != null && cleanChunkTagNMS(chunkTag)) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DeflaterOutputStream dos = new DeflaterOutputStream(baos);
                    DataOutputStream dosData = new DataOutputStream(dos);
                    NbtIo.writeUnnamedTag(chunkTag, dosData);
                    dosData.flush();
                    dos.finish();

                    byte[] newCompressedData = baos.toByteArray();
                    int newLength = newCompressedData.length + 1;
                    int newSectors = (newLength + 4 + 4095) / 4096;

                    if (newSectors <= sectorCount) {
                        raf.seek(chunkOffset);
                        raf.writeInt(newLength);
                        raf.writeByte(compressionType);
                        raf.write(newCompressedData);
                        cleanedChunks++;
                    } else {
                        long fileLength = raf.length();
                        int newSectorOffset = (int) ((fileLength + 4095) / 4096);
                        long newByteOffset = (long) newSectorOffset * 4096;

                        raf.seek(newByteOffset);
                        raf.writeInt(newLength);
                        raf.writeByte(compressionType);
                        raf.write(newCompressedData);

                        header[i * 4] = (byte) ((newSectorOffset >> 16) & 0xFF);
                        header[i * 4 + 1] = (byte) ((newSectorOffset >> 8) & 0xFF);
                        header[i * 4 + 2] = (byte) (newSectorOffset & 0xFF);
                        header[i * 4 + 3] = (byte) (newSectors & 0xFF);

                        raf.seek(i * 4);
                        raf.write(header, i * 4, 4);

                        cleanedChunks++;
                    }
                }
            }
        } catch (Exception e) {
            sendConsole(messageService.message("command.uninstall.mca-error", Map.of("file", mcaFile.getName(), "error", e.getMessage())));
        } finally {
            try {
                raf.close();
            } catch (IOException ignored) {}
        }

        return cleanedChunks;
    }

    private static boolean cleanChunkTagNMS(CompoundTag chunkTag) {
        if (dev.cosmojar.stellaritypaper.api.ServerVersion.getCurrent() == dev.cosmojar.stellaritypaper.api.ServerVersion.V1_21_4) {
            return dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().cleanChunkTag(chunkTag);
        }
        boolean modified = false;

        if (chunkTag.contains("sections")) {
            ListTag sections = chunkTag.getList("sections").orElse(null);
            if (sections != null) {
                for (int i = 0; i < sections.size(); i++) {
                    CompoundTag sectionCompound = sections.getCompound(i).orElse(null);
                    if (sectionCompound == null) continue;

                    if (sectionCompound.contains("biomes")) {
                        CompoundTag biomesTag = sectionCompound.getCompound("biomes").orElse(null);
                        if (biomesTag != null && biomesTag.contains("palette")) {
                            ListTag paletteList = biomesTag.getList("palette").orElse(null);
                            if (paletteList != null) {
                                for (int p = 0; p < paletteList.size(); p++) {
                                    String val = paletteList.getString(p).orElse("");
                                    if (isCustomNamespace(val)) {
                                        paletteList.set(p, StringTag.valueOf("minecraft:the_end"));
                                        modified = true;
                                    }
                                }
                            }
                        }
                    }

                    if (sectionCompound.contains("block_states")) {
                        CompoundTag blockStatesTag = sectionCompound.getCompound("block_states").orElse(null);
                        if (blockStatesTag != null && blockStatesTag.contains("palette")) {
                            ListTag paletteList = blockStatesTag.getList("palette").orElse(null);
                            if (paletteList != null) {
                                for (int p = 0; p < paletteList.size(); p++) {
                                    CompoundTag blockCompound = paletteList.getCompound(p).orElse(null);
                                    if (blockCompound != null && blockCompound.contains("Name")) {
                                        String nameVal = blockCompound.getString("Name").orElse("");
                                        if ("minecraft:structure_block".equals(nameVal) || "structure_block".equals(nameVal) || isCustomNamespace(nameVal)) {
                                            blockCompound.putString("Name", "minecraft:air");
                                            blockCompound.remove("Properties");
                                            modified = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (chunkTag.contains("structures")) {
            CompoundTag structuresTag = chunkTag.getCompound("structures").orElse(null);
            if (structuresTag != null) {
                if (structuresTag.contains("starts")) {
                    CompoundTag startsTag = structuresTag.getCompound("starts").orElse(null);
                    if (startsTag != null) {
                        List<String> keysToRemove = new ArrayList<>();
                        for (String key : startsTag.keySet()) {
                            if (isCustomNamespace(key)) {
                                keysToRemove.add(key);
                            } else {
                                CompoundTag startCompound = startsTag.getCompound(key).orElse(null);
                                if (startCompound != null && cleanCompoundRecursive(startCompound)) {
                                    modified = true;
                                }
                            }
                        }
                        for (String key : keysToRemove) {
                            startsTag.remove(key);
                            modified = true;
                        }
                    }
                }

                if (structuresTag.contains("References")) {
                    CompoundTag referencesTag = structuresTag.getCompound("References").orElse(null);
                    if (referencesTag != null) {
                        List<String> keysToRemove = new ArrayList<>();
                        for (String key : referencesTag.keySet()) {
                            if (isCustomNamespace(key)) {
                                keysToRemove.add(key);
                            }
                        }
                        for (String key : keysToRemove) {
                            referencesTag.remove(key);
                            modified = true;
                        }
                    }
                }
            }
        }

        if (chunkTag.contains("block_entities")) {
            ListTag blockEntitiesList = chunkTag.getList("block_entities").orElse(null);
            if (blockEntitiesList != null) {
                for (int i = blockEntitiesList.size() - 1; i >= 0; i--) {
                    CompoundTag beCompound = blockEntitiesList.getCompound(i).orElse(null);
                    if (beCompound != null) {
                        if (beCompound.contains("id")) {
                            String idVal = beCompound.getString("id").orElse("");
                            if ("minecraft:structure_block".equals(idVal) || "structure_block".equals(idVal) || isCustomNamespace(idVal)) {
                                blockEntitiesList.remove(i);
                                modified = true;
                                continue;
                            }
                        }
                        if (beCompound.contains("Items")) {
                            ListTag itemsList = beCompound.getList("Items").orElse(null);
                            if (itemsList != null) {
                                for (int it = itemsList.size() - 1; it >= 0; it--) {
                                    CompoundTag itemCompound = itemsList.getCompound(it).orElse(null);
                                    if (itemCompound != null) {
                                        String itemId = itemCompound.getString("id").orElse("");
                                        if (isCustomNamespace(itemId)) {
                                            itemsList.remove(it);
                                            modified = true;
                                            continue;
                                        }
                                        if (itemCompound.contains("components")) {
                                            CompoundTag comp = itemCompound.getCompound("components").orElse(null);
                                            if (comp != null && cleanItemComponents(comp)) {
                                                modified = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (chunkTag.contains("entities")) {
            ListTag entitiesList = chunkTag.getList("entities").orElse(null);
            if (entitiesList != null) {
                for (int i = entitiesList.size() - 1; i >= 0; i--) {
                    CompoundTag entCompound = entitiesList.getCompound(i).orElse(null);
                    if (entCompound != null && entCompound.contains("id")) {
                        String idVal = entCompound.getString("id").orElse("");
                        if (isCustomNamespace(idVal)) {
                            entitiesList.remove(i);
                            modified = true;
                        }
                    }
                }
            }
        }

        return modified;
    }

    private static boolean cleanCompoundRecursive(CompoundTag compound) {
        boolean modified = false;
        List<String> keys = new ArrayList<>(compound.keySet());
        for (String key : keys) {
            Tag tag = compound.get(key);
            if (tag instanceof StringTag stringTag) {
                String val = stringTag.value();
                if (isCustomNamespace(val)) {
                    if ("processors".equalsIgnoreCase(key) || "location".equalsIgnoreCase(key) || "pool".equalsIgnoreCase(key)) {
                        compound.putString(key, "minecraft:empty");
                    } else if ("element_type".equalsIgnoreCase(key)) {
                        compound.putString(key, "minecraft:single_pool_element");
                    } else {
                        compound.putString(key, "minecraft:empty");
                    }
                    modified = true;
                }
            } else if (tag instanceof CompoundTag childCompound) {
                if (cleanCompoundRecursive(childCompound)) {
                    modified = true;
                }
            } else if (tag instanceof ListTag listTag) {
                for (int i = 0; i < listTag.size(); i++) {
                    Tag elem = listTag.get(i);
                    if (elem instanceof CompoundTag elemCompound) {
                        if (cleanCompoundRecursive(elemCompound)) {
                            modified = true;
                        }
                    } else if (elem instanceof StringTag strElem) {
                        String val = strElem.value();
                        if (isCustomNamespace(val)) {
                            listTag.set(i, StringTag.valueOf("minecraft:empty"));
                            modified = true;
                        }
                    }
                }
            }
        }
        return modified;
    }

    private static boolean cleanItemComponents(CompoundTag componentsTag) {
        boolean modified = false;
        List<String> keys = new ArrayList<>(componentsTag.keySet());
        for (String key : keys) {
            if (isCustomNamespace(key)) {
                componentsTag.remove(key);
                modified = true;
            } else if ("minecraft:stored_enchantments".equals(key) || "minecraft:enchantments".equals(key)) {
                CompoundTag enchants = componentsTag.getCompound(key).orElse(null);
                if (enchants != null) {
                    List<String> enchKeys = new ArrayList<>(enchants.keySet());
                    for (String enchKey : enchKeys) {
                        if (isCustomNamespace(enchKey)) {
                            enchants.remove(enchKey);
                            modified = true;
                        }
                    }
                }
            }
        }
        return modified;
    }

    private static boolean isCustomNamespace(String id) {
        if (id == null || id.isEmpty()) return false;
        String lower = id.toLowerCase(Locale.ROOT);
        int colonIdx = lower.indexOf(':');
        if (colonIdx != -1) {
            String ns = lower.substring(0, colonIdx);
            if (CUSTOM_NAMESPACES.contains(ns)) return true;
        }
        for (String ns : CUSTOM_NAMESPACES) {
            if (lower.contains(ns)) return true;
        }
        return false;
    }
}
