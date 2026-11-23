package systems.mythical.mythicskywars.managers;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import systems.mythical.mythicskywars.utilities.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LobbyBypassManager {
    private static final Map<UUID, GameMode> PREVIOUS_MODES = new HashMap<>();

    private LobbyBypassManager() {
    }

    public static boolean toggle(Player player) {
        if (player == null) return false;
        if (PREVIOUS_MODES.containsKey(player.getUniqueId())) {
            disable(player);
            return false;
        }
        PREVIOUS_MODES.put(player.getUniqueId(), player.getGameMode());
        player.setGameMode(GameMode.CREATIVE);
        return true;
    }

    public static void disable(Player player) {
        GameMode previous = PREVIOUS_MODES.remove(player.getUniqueId());
        if (previous != null && player.getGameMode() == GameMode.CREATIVE) {
            player.setGameMode(previous);
        }
    }

    public static boolean hasBypass(Player player) {
        return player != null && PREVIOUS_MODES.containsKey(player.getUniqueId())
                && player.hasPermission("sw.bypasslobbyslots")
                && Util.get().isSpawnWorld(player.getWorld())
                && MatchManager.get().getPlayerMapSafe(player) == null;
    }
}
