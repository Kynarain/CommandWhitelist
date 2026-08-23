package cn.kynarain.commandwhitelist.mixin;

import cn.kynarain.commandwhitelist.CommandContext;
import cn.kynarain.commandwhitelist.WhitelistConfig;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Inject(method = "onChatMessage", at = @At("HEAD"))
    private void cmdwhitelist$checkCommand(ChatMessageC2SPacket packet, CallbackInfo ci) {
        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
        ServerPlayerEntity player = handler.player;
        if (player == null) return;

        String message = packet.chatMessage();
        if (!message.startsWith("/")) return;

        String command = message.substring(1);
        String rootCommand = command.split(" ")[0];


        if (WhitelistConfig.isRootCommandAllowed(rootCommand)) {
            CommandContext.setFullCommandAllowed(true);
            return;
        }


        if (WhitelistConfig.isFullCommandAllowed(command)) {
            CommandContext.setFullCommandAllowed(true);
        }
    }

    @Inject(method = "onChatMessage", at = @At("RETURN"))
    private void cmdwhitelist$clearCommand(ChatMessageC2SPacket packet, CallbackInfo ci) {
        CommandContext.clear();
    }
}