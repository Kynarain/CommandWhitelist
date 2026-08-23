package cn.kynarain.commandwhitelist.mixin;

import cn.kynarain.commandwhitelist.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerCommandSource.class)
public abstract class ServerCommandSourceMixin {

    @Inject(method = "hasPermissionLevel", at = @At("HEAD"), cancellable = true)
    private void cmdwhitelist$bypassPermission(int level, CallbackInfoReturnable<Boolean> cir) {
        if (CommandContext.isFullCommandAllowed()) {
            cir.setReturnValue(true);
        }
    }
}