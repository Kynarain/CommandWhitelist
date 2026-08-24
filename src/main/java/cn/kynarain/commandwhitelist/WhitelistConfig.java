package cn.kynarain.commandwhitelist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WhitelistConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("cmdwhitelist");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("cmdwhitelist.json");

    private static final Set<String> rootCommands = ConcurrentHashMap.newKeySet();
    private static final Set<String> fullCommands = ConcurrentHashMap.newKeySet();

    public static void load() {
        rootCommands.clear();
        fullCommands.clear();

        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                if (data.rootCommands != null) {
                    for (String cmd : data.rootCommands) {
                        if (cmd != null && !cmd.isEmpty()) {
                            rootCommands.add(cmd.toLowerCase());
                        }
                    }
                }
                if (data.fullCommands != null) {
                    for (String cmd : data.fullCommands) {
                        if (cmd != null && !cmd.isEmpty()) {
                            fullCommands.add(cmd.toLowerCase());
                        }
                    }
                }
                LOGGER.info("[cmdwhitelist] Loaded {} root command(s) and {} full command(s)",
                        rootCommands.size(), fullCommands.size());
            }
        } catch (Exception e) {
            LOGGER.error("[cmdwhitelist] Failed to load config", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            ConfigData data = new ConfigData();
            data.rootCommands = new ArrayList<>(rootCommands);
            data.fullCommands = new ArrayList<>(fullCommands);

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            LOGGER.error("[cmdwhitelist] Failed to save config", e);
        }
    }

    public static boolean isRootCommandAllowed(String rootCommand) {
        if (rootCommand == null) return false;
        return rootCommands.contains(rootCommand.toLowerCase());
    }

    public static boolean isFullCommandAllowed(String fullCommand) {
        if (fullCommand == null) return false;
        String cmd = fullCommand.startsWith("/") ? fullCommand.substring(1) : fullCommand;
        return fullCommands.contains(cmd.toLowerCase());
    }

    /**
     * 检查给定字面量路径是否匹配完整命令白名单中的某个条目，
     * 或者作为某个条目的前缀（用于放行完整命令的子节点）。
     * 此方法在根命令白名单方案下可选，保留以兼容可能的需求。
     */
    public static boolean isFullCommandPrefixAllowed(String prefix) {
        if (prefix == null) return false;
        String p = prefix.toLowerCase();
        for (String fullCmd : fullCommands) {
            if (fullCmd.equals(p) || fullCmd.startsWith(p + " ")) {
                return true;
            }
        }
        return false;
    }

    public static boolean addRootCommand(String command) {
        if (command == null || command.isEmpty()) return false;
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        boolean added = rootCommands.add(cmd.toLowerCase());
        if (added) save();
        return added;
    }

    public static boolean removeRootCommand(String command) {
        if (command == null || command.isEmpty()) return false;
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        boolean removed = rootCommands.remove(cmd.toLowerCase());
        if (removed) save();
        return removed;
    }

    public static boolean addFullCommand(String command) {
        if (command == null || command.isEmpty()) return false;
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        boolean added = fullCommands.add(cmd.toLowerCase());
        if (added) save();
        return added;
    }

    public static boolean removeFullCommand(String command) {
        if (command == null || command.isEmpty()) return false;
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        boolean removed = fullCommands.remove(cmd.toLowerCase());
        if (removed) save();
        return removed;
    }

    public static Set<String> getRootCommands() {
        return Collections.unmodifiableSet(rootCommands);
    }

    public static Set<String> getFullCommands() {
        return Collections.unmodifiableSet(fullCommands);
    }

    private static class ConfigData {
        List<String> rootCommands = new ArrayList<>();
        List<String> fullCommands = new ArrayList<>();
    }
}