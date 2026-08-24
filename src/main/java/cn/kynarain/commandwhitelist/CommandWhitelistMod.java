package cn.kynarain.commandwhitelist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class CommandWhitelistMod implements ModInitializer {
    public static final String MOD_ID = "cmdwhitelist";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static CommandDispatcher<ServerCommandSource> dispatcher;
    private static final Map<CommandNode<ServerCommandSource>, Predicate<ServerCommandSource>> originalRequirements = new HashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Command Whitelist Mod is initializing...");
        WhitelistConfig.load();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CommandWhitelistMod.dispatcher = dispatcher;
            registerCommands(dispatcher);
            applyWhitelist();
        });
    }

    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("cw")
                        .requires(source -> source.hasPermissionLevel(2))

                        .then(CommandManager.literal("list")
                                .executes(ctx -> {
                                    var roots = WhitelistConfig.getRootCommands();
                                    var fulls = WhitelistConfig.getFullCommands();
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("§a根命令白名单:\n");
                                    if (roots.isEmpty()) {
                                        sb.append("§7  无\n");
                                    } else {
                                        for (String cmd : roots) {
                                            sb.append("§f  /").append(cmd).append("\n");
                                        }
                                    }
                                    sb.append("§a完整命令白名单:\n");
                                    if (fulls.isEmpty()) {
                                        sb.append("§7  无\n");
                                    } else {
                                        for (String cmd : fulls) {
                                            sb.append("§f  /").append(cmd).append("\n");
                                        }
                                    }
                                    ctx.getSource().sendFeedback(() -> Text.literal(sb.toString()), false);
                                    return 1;
                                }))

                        .then(CommandManager.literal("reload")
                                .executes(ctx -> {
                                    WhitelistConfig.load();
                                    applyWhitelist();
                                    ctx.getSource().sendFeedback(() -> Text.literal("§a配置已重新加载"), false);
                                    return 1;
                                }))

                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("rootCommand", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String command = StringArgumentType.getString(ctx, "rootCommand");
                                            boolean removed = WhitelistConfig.removeRootCommand(command);
                                            if (removed) applyWhitelist();
                                            ctx.getSource().sendFeedback(() -> Text.literal(
                                                    removed ? "§a已移除根命令白名单: /" + command : "§e该根命令不在白名单中: /" + command
                                            ), false);
                                            return removed ? 1 : 0;
                                        })))

                        .then(CommandManager.literal("c")
                                .then(CommandManager.argument("fullCommand", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String rawCommand = StringArgumentType.getString(ctx, "fullCommand");
                                            final String command = rawCommand.startsWith("/") ? rawCommand.substring(1) : rawCommand;
                                            boolean added = WhitelistConfig.addFullCommand(command);
                                            if (added) applyWhitelist();
                                            ctx.getSource().sendFeedback(() -> Text.literal(
                                                    added ? "§a已添加完整命令白名单: /" + command : "§e该完整命令已在白名单中: /" + command
                                            ), false);
                                            return added ? 1 : 0;
                                        }))
                                .then(CommandManager.literal("remove")
                                        .then(CommandManager.argument("fullCommand", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    String rawCommand = StringArgumentType.getString(ctx, "fullCommand");
                                                    final String command = rawCommand.startsWith("/") ? rawCommand.substring(1) : rawCommand;
                                                    boolean removed = WhitelistConfig.removeFullCommand(command);
                                                    if (removed) applyWhitelist();
                                                    ctx.getSource().sendFeedback(() -> Text.literal(
                                                            removed ? "§a已移除完整命令白名单: /" + command : "§e该完整命令不在白名单中: /" + command
                                                    ), false);
                                                    return removed ? 1 : 0;
                                                }))))

                        .then(CommandManager.argument("rootCommand", StringArgumentType.word())
                                .executes(ctx -> {
                                    String command = StringArgumentType.getString(ctx, "rootCommand");
                                    boolean added = WhitelistConfig.addRootCommand(command);
                                    if (added) applyWhitelist();
                                    ctx.getSource().sendFeedback(() -> Text.literal(
                                            added ? "§a已添加根命令白名单: /" + command : "§e该根命令已在白名单中: /" + command
                                    ), false);
                                    return added ? 1 : 0;
                                }))
        );
    }


    private static void applyWhitelist() {
        if (dispatcher == null) return;


        restoreOriginalRequirements();


        for (CommandNode<ServerCommandSource> root : dispatcher.getRoot().getChildren()) {
            String rootName = root.getName();
            if (WhitelistConfig.isRootCommandAllowed(rootName)) {
                setAllRequirements(root, source -> true);
            }
        }


        for (String fullCommand : WhitelistConfig.getFullCommands()) {
            String[] parts = fullCommand.split(" ");
            if (parts.length == 0) continue;
            String rootName = parts[0];
            if (WhitelistConfig.isRootCommandAllowed(rootName)) {
                continue;
            }
            CommandNode<ServerCommandSource> node = findNode(dispatcher.getRoot(), parts);
            if (node != null) {
                setAllRequirements(node, source -> true);
            }
        }
    }


    private static void restoreOriginalRequirements() {
        for (Map.Entry<CommandNode<ServerCommandSource>, Predicate<ServerCommandSource>> entry : originalRequirements.entrySet()) {
            setRequirementDirect(entry.getKey(), entry.getValue());
        }
        originalRequirements.clear();
    }


    private static void setAllRequirements(CommandNode<ServerCommandSource> node, Predicate<ServerCommandSource> requirement) {
        setRequirement(node, requirement);
        for (CommandNode<ServerCommandSource> child : node.getChildren()) {
            setAllRequirements(child, requirement);
        }
    }


    private static void setRequirement(CommandNode<ServerCommandSource> node, Predicate<ServerCommandSource> requirement) {
        if (!originalRequirements.containsKey(node)) {
            Predicate<ServerCommandSource> original = getCurrentRequirement(node);
            originalRequirements.put(node, original);
        }
        setRequirementDirect(node, requirement);
    }



    private static void setRequirementDirect(CommandNode<ServerCommandSource> node, Predicate<ServerCommandSource> requirement) {
        try {
            Field field = CommandNode.class.getDeclaredField("requirement");
            field.setAccessible(true);
            field.set(node, requirement);
        } catch (NoSuchFieldException e) {
            LOGGER.error("Could not find 'requirement' field in CommandNode", e);
        } catch (IllegalAccessException e) {
            LOGGER.error("Could not access 'requirement' field", e);
        }
    }


    @SuppressWarnings("unchecked")
    private static Predicate<ServerCommandSource> getCurrentRequirement(CommandNode<ServerCommandSource> node) {
        try {
            Field field = CommandNode.class.getDeclaredField("requirement");
            field.setAccessible(true);
            return (Predicate<ServerCommandSource>) field.get(node);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error("Could not get 'requirement' field", e);
            return source -> false;
        }
    }

    private static CommandNode<ServerCommandSource> findNode(CommandNode<ServerCommandSource> root, String[] pathParts) {
        CommandNode<ServerCommandSource> current = root;
        for (String part : pathParts) {
            CommandNode<ServerCommandSource> next = null;
            for (CommandNode<ServerCommandSource> child : current.getChildren()) {
                if (child instanceof LiteralCommandNode && child.getName().equals(part)) {
                    next = child;
                    break;
                }
            }
            if (next == null) {
                return null;
            }
            current = next;
        }
        return current;
    }
}