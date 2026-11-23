package systems.mythical.mythicskywars.managers.worlds;

import com.google.common.collect.Lists;
import systems.mythical.mythicskywars.MythicSkywars;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;

public class FileWorldManager implements WorldManager {

    public World createEmptyWorld(String name, Environment environment) {
        if (org.bukkit.Bukkit.getWorld(name) == null) {
            loadWorld(name, environment, false);
            return org.bukkit.Bukkit.getWorld(name);
        }
        return null;
    }

    public boolean loadWorld(String worldName, Environment environment, boolean readOnly) {

        // On Paper 26.1+, worlds are stored in world/dimensions/minecraft/<name>
        // We need to delete the migrated folder before re-creating to avoid migration conflicts
        File migratedDir = new File(MythicSkywars.get().getServer().getWorldContainer(), "world/dimensions/minecraft/" + worldName);
        if (migratedDir.exists()) {
            deleteWorld(migratedDir);
        }

        // Same files BedWars deletes before createWorld. A copied uid.dat makes CraftServer
        // refuse the world as a duplicate, and a leftover session.lock blocks the reload.
        deleteWorldTrash(worldName);

        WorldCreator worldCreator = new WorldCreator(worldName);
        worldCreator.environment(environment);
        worldCreator.generateStructures(false);
        worldCreator.generator(MythicSkywars.getNMS().getChunkGenerator());

        World world = worldCreator.createWorld();

        world.setDifficulty(org.bukkit.Difficulty.NORMAL);
        world.setSpawnFlags(true, true);
        world.setPVP(true);
        world.setStorm(false);
        world.setThundering(false);
        world.setWeatherDuration(Integer.MAX_VALUE);
        world.setKeepSpawnInMemory(true);
        world.setTicksPerAnimalSpawns(1);
        world.setTicksPerMonsterSpawns(1);
        world.setAutoSave(!readOnly);

        MythicSkywars.getNMS().setGameRule(world, "doMobSpawning", "false");
        MythicSkywars.getNMS().setGameRule(world, "mobGriefing", "true");
        MythicSkywars.getNMS().setGameRule(world, "doFireTick", "true");
        MythicSkywars.getNMS().setGameRule(world, "showDeathMessages", "false");
        MythicSkywars.getNMS().setGameRule(world, "announceAdvancements", "false");
        MythicSkywars.getNMS().setGameRule(world, "doDaylightCycle", "false");
        MythicSkywars.getNMS().setGameRule(world, "randomTickSpeed", "0");

        boolean loaded = false;
        for (World w : MythicSkywars.get().getServer().getWorlds()) {
            if (w.getName().equals(world.getName())) {
                loaded = true;
                break;
            }
        }
        return loaded;
    }

    public void unloadWorld(String w, boolean save) {
        World world = MythicSkywars.get().getServer().getWorld(w);

        if (world != null) {
            for (Player p : world.getPlayers()) {
                p.teleport(MythicSkywars.getCfg().getSpawn());
            }
            if (MythicSkywars.get().getServer().unloadWorld(world, save)) {
                MythicSkywars.getNMS().finishWorldUnload(world);
            }
        }
    }

    /**
     * Deletes the session files BedWars removes before {@code createWorld}.
     * level.dat is included so a copied map does not keep stale game rules.
     */
    private void deleteWorldTrash(String worldName) {
        File folder = getWorldFolder(worldName);
        String[] names = new String[]{"level.dat", "level.dat_mcr", "level.dat_old", "session.lock", "uid.dat"};
        for (String name : names) {
            File file = new File(folder, name);
            if (file.exists() && !file.delete()) {
                MythicSkywars.get().getLogger().warning("Could not delete " + file.getPath());
            }
        }
    }

    public void copyWorld(File source, File target) {
        try {
            List<String> ignore = Lists.newArrayList("uid.dat", "session.dat", "session.lock");
            if (!ignore.contains(source.getName())) {
                if (source.isDirectory()) {
                    if ((!target.exists()) &&
                            (target.mkdirs())) {
                        String[] files = source.list();
                        if (files != null) {
                            for (String file : files) {
                                File srcFile = new File(source, file);
                                File destFile = new File(target, file);
                                copyWorld(srcFile, destFile);
                            }
                        }
                    }
                } else {
                    java.io.InputStream in = new java.io.FileInputStream(source);
                    OutputStream out = new java.io.FileOutputStream(target);
                    byte[] buffer = new byte['Ѐ'];
                    int length;
                    while ((length = in.read(buffer)) > 0)
                        out.write(buffer, 0, length);
                    in.close();
                    out.close();
                }
            }
        } catch (FileNotFoundException e) {
            MythicSkywars.get().getLogger().log(Level.SEVERE, "Failed to copy world as required! - file not found");
            e.printStackTrace();
        } catch (IOException e) {
            MythicSkywars.get().getLogger().info("Failed to copy world as required!");
            e.printStackTrace();
        }
    }

    public void deleteWorld(String name, boolean removeFile) {
        unloadWorld(name, false);
        File target = getWorldFolder(name);
        deleteWorld(target);
    }

    public void deleteWorld(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteWorld(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        path.delete();
    }

    @Override
    public WorldManagerType getType() {
        return WorldManagerType.FILE;
    }
}
