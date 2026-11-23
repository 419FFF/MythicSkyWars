package systems.mythical.mythicskywars.commands.player;

import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.managers.LobbyBypassManager;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Util;
import systems.mythical.mythicskywars.managers.MatchManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SWBypassLobbySlotsCmd extends BaseCmd {
    public SWBypassLobbySlotsCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "bypasslobbyslots";
        alias = new String[]{"lobbybuild", "build"};
        argLength = 1;
        maxArgs = 1;
    }

    @Override
    public boolean run(CommandSender sender, Player player, String[] args) {
        if (!Util.get().isSpawnWorld(player.getWorld()) || MatchManager.get().getPlayerMapSafe(player) != null) {
            player.sendMessage(new Messaging.MessageFormatter().format("command.lobby-build-lobby-only"));
            return true;
        }
        boolean enabled = LobbyBypassManager.toggle(player);
        if (enabled) {
            player.sendMessage(new Messaging.MessageFormatter().format("command.bypass-lobby-slots-enabled"));
        } else {
            player.sendMessage(new Messaging.MessageFormatter().format("command.bypass-lobby-slots-disabled"));
        }
        return true;
    }
}
