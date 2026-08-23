package cn.kynarain.commandwhitelist;

public class CommandContext {
    private static final ThreadLocal<Boolean> FULL_COMMAND_ALLOWED = ThreadLocal.withInitial(() -> false);

    public static void setFullCommandAllowed(boolean allowed) {
        FULL_COMMAND_ALLOWED.set(allowed);
    }

    public static boolean isFullCommandAllowed() {
        return FULL_COMMAND_ALLOWED.get();
    }

    public static void clear() {
        FULL_COMMAND_ALLOWED.remove();
    }
}