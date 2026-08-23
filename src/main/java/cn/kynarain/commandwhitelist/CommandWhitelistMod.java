package cn.kynarain.commandwhitelist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandWhitelistMod implements ModInitializer {
    public static final String MOD_ID = "cmdwhitelist";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.warn("==============================================");
        LOGGER.warn("Command Whitelist is a PRE-RELEASE version!");
        LOGGER.warn("This mod is NOT production ready. Use at your own risk.");
        LOGGER.warn("==============================================");

        WhitelistConfig.load();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher);
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
                                    ctx.getSource().sendFeedback(() -> Text.literal("§a配置已重新加载"), false);
                                    return 1;
                                }))

                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("rootCommand", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String command = StringArgumentType.getString(ctx, "rootCommand");
                                            boolean removed = WhitelistConfig.removeRootCommand(command);
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
                                                    ctx.getSource().sendFeedback(() -> Text.literal(
                                                            removed ? "§a已移除完整命令白名单: /" + command : "§e该完整命令不在白名单中: /" + command
                                                    ), false);
                                                    return removed ? 1 : 0;
                                                }))))

                        .then(CommandManager.argument("rootCommand", StringArgumentType.word())
                                .executes(ctx -> {
                                    String command = StringArgumentType.getString(ctx, "rootCommand");
                                    boolean added = WhitelistConfig.addRootCommand(command);
                                    ctx.getSource().sendFeedback(() -> Text.literal(
                                            added ? "§a已添加根命令白名单: /" + command : "§e该根命令已在白名单中: /" + command
                                    ), false);
                                    return added ? 1 : 0;
                                }))
        );
    }
}