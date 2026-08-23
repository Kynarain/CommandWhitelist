package cn.kynarain.commandwhitelist.mixin;

import cn.kynarain.commandwhitelist.WhitelistConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CommandDispatcher.class)
public abstract class CommandDispatcherMixin {

    @ModifyVariable(
            method = "execute(Ljava/lang/String;Ljava/lang/Object;)I",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1
    )
    private Object commandwhitelist$modifySource(String input, Object source) {
        if (!(source instanceof ServerCommandSource serverSource) ||
                !(serverSource.getEntity() instanceof ServerPlayerEntity)) {
            return source;
        }

        String command = input;
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        if (WhitelistConfig.isFullCommandAllowed(command)) {
            return serverSource.withLevel(4);
        }


        String rootCommand = command.split(" ", 2)[0];
        if (WhitelistConfig.isRootCommandAllowed(rootCommand)) {
            return serverSource.withLevel(4);
        }

        return source;
    }
}