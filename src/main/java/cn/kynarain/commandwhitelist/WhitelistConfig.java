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
    private static final Logger LOGGER = LoggerFactory.getLogger("CommandWhitelist");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("commandwhitelist.json");

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
                    rootCommands.addAll(data.rootCommands);
                }
                if (data.fullCommands != null) {
                    fullCommands.addAll(data.fullCommands);
                }
                LOGGER.info("[CommandWhitelist] Loaded {} root command(s) and {} full command(s)",
                        rootCommands.size(), fullCommands.size());
            }
        } catch (Exception e) {
            LOGGER.error("[CommandWhitelist] Failed to load config", e);
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
            LOGGER.error("[CommandWhitelist] Failed to save config", e);
        }
    }

    public static boolean isRootCommandAllowed(String rootCommand) {
        return rootCommands.contains(rootCommand);
    }

    public static boolean isFullCommandAllowed(String fullCommand) {
        if (fullCommand.startsWith("/")) {
            fullCommand = fullCommand.substring(1);
        }
        return fullCommands.contains(fullCommand);
    }

    public static boolean addRootCommand(String command) {
        boolean added = rootCommands.add(command);
        if (added) save();
        return added;
    }

    public static boolean removeRootCommand(String command) {
        boolean removed = rootCommands.remove(command);
        if (removed) save();
        return removed;
    }

    public static boolean addFullCommand(String command) {
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        boolean added = fullCommands.add(command);
        if (added) save();
        return added;
    }

    public static boolean removeFullCommand(String command) {
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        boolean removed = fullCommands.remove(command);
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