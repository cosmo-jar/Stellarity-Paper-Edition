package dev.cosmojar.stellaritypaper.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
public final class ConfigAutoUpdater {

    private ConfigAutoUpdater() {
    }

    public static void update(final Plugin plugin, final String resourceName) {
        if (plugin == null || resourceName == null || resourceName.isBlank()) {
            return;
        }

        final File targetFile = new File(plugin.getDataFolder(), resourceName);
        if (!targetFile.exists()) {
            try {
                plugin.saveResource(resourceName, false);
            } catch (final Throwable ignored) {
            }
            return;
        }

        final InputStream resourceStream = plugin.getResource(resourceName);
        if (resourceStream == null) {
            return;
        }

        try {
            final YamlConfiguration currentConfig = YamlConfiguration.loadConfiguration(targetFile);
            final YamlConfiguration templateConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(resourceStream, StandardCharsets.UTF_8)
            );

            final Set<String> missingKeys = new LinkedHashSet<>();
            for (final String key : templateConfig.getKeys(true)) {
                if (!templateConfig.isConfigurationSection(key) && !currentConfig.contains(key)) {
                    missingKeys.add(key);
                }
            }

            if (missingKeys.isEmpty()) {
                return;
            }

            plugin.getLogger().info("Discovered new settings for " + resourceName
                    + " (" + missingKeys.size() + " entries): " + missingKeys);

            applyUpdates(plugin, resourceName, targetFile, missingKeys, templateConfig);
        } catch (final Throwable t) {
            plugin.getLogger().warning("Failed to check or update " + resourceName
                    + ": " + t.getMessage());
        }
    }

    private static void applyUpdates(
            final Plugin plugin,
            final String resourceName,
            final File targetFile,
            final Set<String> missingKeys,
            final YamlConfiguration templateConfig
    ) {
        try {
            final List<String> templateLines = readResourceLines(plugin, resourceName);
            if (templateLines.isEmpty()) {
                return;
            }

            final List<String> currentLines = Files.readAllLines(targetFile.toPath(), StandardCharsets.UTF_8);
            final List<YamlNode> templateNodes = parseYamlNodes(templateLines, templateConfig);

            final File backupFile = new File(targetFile.getParentFile(), targetFile.getName() + ".bak");
            Files.copy(targetFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            final List<String> updatedLines = new ArrayList<>(currentLines);

            final Set<String> pathsToInsert = new LinkedHashSet<>();
            for (final String missingKey : missingKeys) {
                final String[] parts = missingKey.split("\\.");
                final StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) sb.append('.');
                    sb.append(parts[i]);
                    pathsToInsert.add(sb.toString());
                }
            }

            int addedCount = 0;
            for (final YamlNode node : templateNodes) {
                if (!pathsToInsert.contains(node.path)) {
                    continue;
                }

                if (containsPathInLines(updatedLines, node.path)) {
                    continue;
                }

                insertNode(updatedLines, node);
                addedCount++;
            }

            if (addedCount == 0) {
                backupFile.delete();
                return;
            }

            final String finalYaml = String.join(System.lineSeparator(), updatedLines);
            final YamlConfiguration verify = new YamlConfiguration();
            verify.loadFromString(finalYaml);

            Files.writeString(targetFile.toPath(), finalYaml, StandardCharsets.UTF_8);
            backupFile.delete();

            plugin.getLogger().info("File " + resourceName
                    + " successfully updated with new settings (added nodes: " + addedCount + ").");
        } catch (final Throwable t) {
            plugin.getLogger().warning("Error updating " + resourceName
                    + ", file restored from backup: " + t.getMessage());
            final File backupFile = new File(targetFile.getParentFile(), targetFile.getName() + ".bak");
            if (backupFile.exists()) {
                try {
                    Files.copy(backupFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    backupFile.delete();
                } catch (final IOException ignored) {
                }
            }
        }
    }

    private static void insertNode(final List<String> lines, final YamlNode node) {
        if (node.parentPath.isEmpty()) {
            if (!lines.isEmpty() && !lines.get(lines.size() - 1).isBlank()) {
                lines.add("");
            }
            lines.addAll(node.lines);
            return;
        }

        final int parentIndex = findPathLineIndex(lines, node.parentPath);
        if (parentIndex == -1) {
            if (!lines.isEmpty() && !lines.get(lines.size() - 1).isBlank()) {
                lines.add("");
            }
            lines.addAll(node.lines);
            return;
        }

        final int parentIndent = countLeadingSpaces(lines.get(parentIndex));
        int lastContentLine = parentIndex;
        for (int idx = parentIndex + 1; idx < lines.size(); idx++) {
            final String cur = lines.get(idx);
            final String trimmed = cur.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            final int curIndent = countLeadingSpaces(cur);
            if (curIndent <= parentIndent) {
                break;
            }
            lastContentLine = idx;
        }

        final int insertPos = lastContentLine + 1;
        lines.addAll(insertPos, node.lines);
    }

    private static int findPathLineIndex(final List<String> lines, final String path) {
        final String[] parts = path.split("\\.");
        int searchFrom = 0;
        int currentIndent = -1;

        for (int i = 0; i < parts.length; i++) {
            final String targetPart = parts[i];
            int foundAt = -1;

            for (int lineIdx = searchFrom; lineIdx < lines.size(); lineIdx++) {
                final String line = lines.get(lineIdx);
                final String trimmed = line.trim();
                if (trimmed.startsWith("#") || trimmed.isEmpty() || trimmed.startsWith("-")) {
                    continue;
                }

                final int indent = countLeadingSpaces(line);
                if (i > 0 && indent <= currentIndent) {
                    break;
                }

                if (matchesKey(trimmed, targetPart)) {
                    foundAt = lineIdx;
                    currentIndent = indent;
                    break;
                }
            }

            if (foundAt == -1) {
                return -1;
            }
            searchFrom = foundAt + 1;
        }

        return searchFrom - 1;
    }

    private static boolean matchesKey(final String trimmed, final String key) {
        final int colonIdx = trimmed.indexOf(':');
        if (colonIdx == -1) {
            return false;
        }
        String candidateKey = trimmed.substring(0, colonIdx).trim();
        if ((candidateKey.startsWith("\"") && candidateKey.endsWith("\""))
                || (candidateKey.startsWith("'") && candidateKey.endsWith("'"))) {
            candidateKey = candidateKey.substring(1, candidateKey.length() - 1);
        }
        return candidateKey.equals(key);
    }

    private static boolean containsPathInLines(final List<String> lines, final String path) {
        return findPathLineIndex(lines, path) != -1;
    }

    private static List<String> readResourceLines(final Plugin plugin, final String resourceName) throws IOException {
        final List<String> lines = new ArrayList<>();
        try (final InputStream in = plugin.getResource(resourceName)) {
            if (in == null) return lines;
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    private static List<YamlNode> parseYamlNodes(final List<String> lines, final YamlConfiguration templateConfig) {
        final List<YamlNode> nodes = new ArrayList<>();
        final List<String> currentComments = new ArrayList<>();
        final Deque<IndentKey> stack = new ArrayDeque<>();

        int i = 0;
        while (i < lines.size()) {
            final String line = lines.get(i);
            final String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                currentComments.add(line);
                i++;
                continue;
            }

            if (trimmed.startsWith("-")) {
                currentComments.add(line);
                i++;
                continue;
            }

            final int colonIdx = trimmed.indexOf(':');
            if (colonIdx == -1) {
                currentComments.add(line);
                i++;
                continue;
            }

            final int indent = countLeadingSpaces(line);
            String rawKey = trimmed.substring(0, colonIdx).trim();
            if ((rawKey.startsWith("\"") && rawKey.endsWith("\"")) || (rawKey.startsWith("'") && rawKey.endsWith("'"))) {
                rawKey = rawKey.substring(1, rawKey.length() - 1);
            }

            while (!stack.isEmpty() && stack.peek().indent >= indent) {
                stack.pop();
            }

            final String parentPath = buildPath(stack);
            final String fullPath = parentPath.isEmpty() ? rawKey : parentPath + "." + rawKey;

            final boolean isSection = templateConfig != null && templateConfig.isConfigurationSection(fullPath);

            final List<String> nodeLines = new ArrayList<>(currentComments);
            currentComments.clear();
            nodeLines.add(line);
            i++;

            if (isSection) {
                nodes.add(new YamlNode(fullPath, parentPath, indent, nodeLines, true));
                stack.push(new IndentKey(indent, rawKey));
            } else {
                while (i < lines.size()) {
                    final String nextLine = lines.get(i);
                    final String nextTrimmed = nextLine.trim();
                    if (nextTrimmed.isEmpty()) {
                        int peekIdx = i + 1;
                        while (peekIdx < lines.size() && lines.get(peekIdx).trim().isEmpty()) {
                            peekIdx++;
                        }
                        if (peekIdx < lines.size()) {
                            final String peekLine = lines.get(peekIdx);
                            final int peekIndent = countLeadingSpaces(peekLine);
                            if (peekIndent > indent) {
                                nodeLines.add(nextLine);
                                i++;
                                continue;
                            }
                        }
                        break;
                    }

                    final int nextIndent = countLeadingSpaces(nextLine);
                    if (nextIndent > indent) {
                        nodeLines.add(nextLine);
                        i++;
                    } else {
                        break;
                    }
                }

                nodes.add(new YamlNode(fullPath, parentPath, indent, nodeLines, false));
            }
        }

        return nodes;
    }

    private static String buildPath(final Deque<IndentKey> stack) {
        if (stack.isEmpty()) return "";
        final List<IndentKey> list = new ArrayList<>(stack);
        Collections.reverse(list);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append('.');
            sb.append(list.get(i).key);
        }
        return sb.toString();
    }

    private static int countLeadingSpaces(final String str) {
        int count = 0;
        while (count < str.length() && str.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    private record YamlNode(String path, String parentPath, int indent, List<String> lines, boolean isSection) {
    }

    private record IndentKey(int indent, String key) {
    }
}
