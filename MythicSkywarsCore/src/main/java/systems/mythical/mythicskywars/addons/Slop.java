package systems.mythical.mythicskywars.addons;

import org.bukkit.entity.Player;

/** Calls slopCore without compiling against its Java 17 classes. */
public final class Slop {
    private Slop() {
    }

    public static void unlock(Player player, String id) {
        invoke("unlockAchievement", new Class[]{Player.class, String.class}, new Object[]{player, id});
    }

    public static int add(Player player, String id, int amount) {
        Object value = invoke("addProgress", new Class[]{Player.class, String.class, int.class}, new Object[]{player, id, Integer.valueOf(amount)});
        return value instanceof Integer ? ((Integer) value).intValue() : 0;
    }

    public static void play(Player player) {
        invoke("noteMinigameStart", new Class[]{Player.class}, new Object[]{player});
    }

    public static void win(Player player) {
        invoke("noteMinigameWin", new Class[]{Player.class}, new Object[]{player});
    }

    private static Object invoke(String name, Class[] types, Object[] args) {
        try {
            Class apiClass = Class.forName("com.slop.core.api.NetworkAPI");
            Object api = apiClass.getMethod("get").invoke(null);
            return apiClass.getMethod(name, types).invoke(api, args);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
