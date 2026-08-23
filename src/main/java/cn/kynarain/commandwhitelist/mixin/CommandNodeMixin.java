package cn.kynarain.commandwhitelist.mixin;

import cn.kynarain.commandwhitelist.WhitelistConfig;
import com.mojang.brigadier.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(CommandNode.class)
public abstract class CommandNodeMixin {

    @Inject(method = "canUse(Ljava/lang/Object;)Z", at = @At("HEAD"), cancellable = true)
    private void cmdwhitelist$bypassPermission(Object source, CallbackInfoReturnable<Boolean> cir) {
        if (!(source instanceof ServerCommandSource serverSource)) return;
        if (!(serverSource.getEntity() instanceof ServerPlayerEntity)) return;

        CommandNode<?> node = (CommandNode<?>) (Object) this;
        String rootName = getRootName(node);

        // 根命令白名单
        if (WhitelistConfig.isRootCommandAllowed(rootName)) {
            cir.setReturnValue(true);
            return;
        }

        // 完整命令白名单（仅限全字面量命令）
        String literalPath = buildLiteralPath(node);
        if (literalPath != null && WhitelistConfig.isFullCommandPrefixAllowed(literalPath)) {
            cir.setReturnValue(true);
        }
    }

    private static String getRootName(CommandNode<?> node) {
        CommandNode<?> current = node;
        while (current.getParent() != null) {
            current = current.getParent();
        }
        return current.getName();
    }

    private static String buildLiteralPath(CommandNode<?> node) {
        List<String> parts = new ArrayList<>();
        CommandNode<?> current = node;
        while (current != null) {
            if (current instanceof LiteralCommandNode && !current.getName().isEmpty()) {
                parts.add(current.getName());
            } else if (current.getParent() != null) {
                return null; // 遇到非字面量节点（且不是根节点）则无法匹配
            }
            current = current.getParent();
        }
        Collections.reverse(parts);
        return String.join(" ", parts);
    }
}