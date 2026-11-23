package systems.mythical.mythicskywars.addons;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * A MythicSkywars addon. External jars live in the Addons folder and name this class in addon.yml.
 */
public interface Addon {
    void onEnable(JavaPlugin plugin);
}
