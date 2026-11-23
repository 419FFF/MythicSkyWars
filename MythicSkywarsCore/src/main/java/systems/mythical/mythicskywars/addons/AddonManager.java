package systems.mythical.mythicskywars.addons;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class AddonManager {
    private final JavaPlugin plugin;
    private final List<Addon> addons = new ArrayList<Addon>();

    public AddonManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(Addon addon) {
        addons.add(addon);
        addon.onEnable(plugin);
    }

    public void loadJars() {
        File dir = new File(plugin.getDataFolder(), "Addons");
        if (!dir.exists() && !dir.mkdirs()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.getName().endsWith(".jar")) {
                continue;
            }
            load(file);
        }
    }

    private void load(File file) {
        JarFile jar = null;
        try {
            jar = new JarFile(file);
            JarEntry entry = jar.getJarEntry("addon.yml");
            if (entry == null) {
                plugin.getLogger().warning(file.getName() + " has no addon.yml.");
                return;
            }
            InputStream in = jar.getInputStream(entry);
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            String main = yml.getString("main");
            in.close();
            if (main == null || main.isEmpty()) {
                plugin.getLogger().warning(file.getName() + " addon.yml is missing main.");
                return;
            }
            URLClassLoader loader = new URLClassLoader(new URL[]{file.toURI().toURL()}, plugin.getClass().getClassLoader());
            Class<?> type = Class.forName(main, true, loader);
            Addon addon = (Addon) type.getConstructor().newInstance();
            register(addon);
            plugin.getLogger().info("Loaded addon " + file.getName());
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not load addon " + file.getName() + ": " + ex.getMessage());
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
