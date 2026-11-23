package systems.mythical.mythicskywars.addons;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.events.MythicSkywarsDeathEvent;
import systems.mythical.mythicskywars.events.MythicSkywarsKillEvent;
import systems.mythical.mythicskywars.events.MythicSkywarsMatchStateChangeEvent;
import systems.mythical.mythicskywars.events.MythicSkywarsWinEvent;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.managers.PlayerStat;
import systems.mythical.mythicskywars.menus.gameoptions.objects.CoordLoc;
import systems.mythical.mythicskywars.utilities.LevelManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SkywarsAchievements implements Addon, Listener {
    private JavaPlugin plugin;
    private final Map<GameMap, Long> started = new HashMap<GameMap, Long>();
    private final Map<GameMap, Integer> startedWith = new HashMap<GameMap, Integer>();
    private final Map<GameMap, Boolean> firstBlood = new HashMap<GameMap, Boolean>();
    private final Map<UUID, Integer> kills = new HashMap<UUID, Integer>();
    private final Map<UUID, Boolean> attacked = new HashMap<UUID, Boolean>();
    private final Map<UUID, Boolean> woreArmor = new HashMap<UUID, Boolean>();
    private final Map<UUID, Set<String>> looted = new HashMap<UUID, Set<String>>();

    @Override
    public void onEnable(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onState(MythicSkywarsMatchStateChangeEvent event) {
        if (event.isBungeecord() || event.getState() != MatchState.PLAYING) {
            return;
        }
        GameMap map;
        try {
            map = event.getGameMap();
        } catch (Exception ex) {
            return;
        }
        if (map == null) {
            return;
        }
        started.put(map, Long.valueOf(System.currentTimeMillis()));
        int count = map.getAlivePlayers() == null ? 0 : map.getAlivePlayers().size();
        startedWith.put(map, Integer.valueOf(count));
        firstBlood.put(map, Boolean.FALSE);
        if (map.getAlivePlayers() == null) {
            return;
        }
        for (Player player : map.getAlivePlayers()) {
            looted.remove(player.getUniqueId());
            Slop.play(player);
            levels(player);
        }
    }

    @EventHandler
    public void onKill(MythicSkywarsKillEvent event) {
        Player killer = event.getKiller();
        if (killer == null) {
            return;
        }
        GameMap map = event.getGame();
        if (!Boolean.TRUE.equals(firstBlood.get(map))) {
            firstBlood.put(map, Boolean.TRUE);
            Slop.unlock(killer, "sw.first-blood");
        }
        Integer soFar = kills.get(killer.getUniqueId());
        kills.put(killer.getUniqueId(), Integer.valueOf((soFar == null ? 0 : soFar.intValue()) + 1));
        long start = started.containsKey(map) ? started.get(map).longValue() : 0L;
        if (start > 0L && System.currentTimeMillis() - start <= 10000L) {
            Slop.unlock(killer, "sw.manhunt");
        }
        int total = Slop.add(killer, "sw.kills", 1);
        if (total >= 25) Slop.unlock(killer, "sw.warrior.25");
        if (total >= 100) Slop.unlock(killer, "sw.warrior.100");
        if (total >= 250) Slop.unlock(killer, "sw.warrior.250");
        if (total >= 500) Slop.unlock(killer, "sw.warrior.500");
        if (total >= 1000) Slop.unlock(killer, "sw.warrior.1000");
        armor(killer);
    }

    @EventHandler
    public void onDeath(MythicSkywarsDeathEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        GameMap map = event.getGame();
        long start = started.containsKey(map) ? started.get(map).longValue() : 0L;
        if (event.getTaggerPlayer() != null && start > 0L && System.currentTimeMillis() - start <= 10000L) {
            Slop.unlock(player, "sw.hunted");
        }
        if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.VOID
                && event.getTaggerPlayer() == null) {
            double health = player.getHealth();
            if (health >= 1.0D && health <= 2.0D) {
                Slop.unlock(player, "sw.fallacy");
            }
        }
    }

    @EventHandler
    public void onWin(MythicSkywarsWinEvent event) {
        final Player player = event.getPlayer();
        final GameMap map = event.getGame();
        if (player == null || map == null) {
            return;
        }
        Slop.unlock(player, "sw.gg");
        Slop.win(player);
        int wins = Slop.add(player, "sw.wins", 1);
        if (wins >= 10) Slop.unlock(player, "sw.warlord.10");
        if (wins >= 50) Slop.unlock(player, "sw.warlord.50");
        if (wins >= 100) Slop.unlock(player, "sw.warlord.100");
        if (wins >= 250) Slop.unlock(player, "sw.warlord.250");
        if (wins >= 500) Slop.unlock(player, "sw.warlord.500");
        int players = startedWith.containsKey(map) ? startedWith.get(map).intValue() : 0;
        double health = player.getHealth();
        if (health < 4.0D) {
            Slop.unlock(player, "sw.not-close");
        }
        if (health <= 1.0D) {
            Slop.unlock(player, "sw.unstoppable");
        }
        long start = started.containsKey(map) ? started.get(map).longValue() : 0L;
        if (players >= 4 && start > 0L && System.currentTimeMillis() - start < 60000L) {
            Slop.unlock(player, "sw.speed");
        }
        if (players >= 4 && !Boolean.TRUE.equals(woreArmor.get(player.getUniqueId())) && !wearingArmor(player)) {
            Slop.unlock(player, "sw.untouchable");
        }
        if (players >= 4 && !Boolean.TRUE.equals(attacked.get(player.getUniqueId()))) {
            Slop.unlock(player, "sw.pacifist");
        }
        Integer got = kills.get(player.getUniqueId());
        if (players >= 4 && got != null && got.intValue() >= players - 1) {
            Slop.unlock(player, "sw.genocide");
        }
        int best = 0;
        UUID bestId = null;
        boolean tie = false;
        if (map.getAlivePlayers() != null) {
            for (Player alive : map.getAlivePlayers()) {
                Integer count = kills.get(alive.getUniqueId());
                int value = count == null ? 0 : count.intValue();
                if (value > best) {
                    best = value;
                    bestId = alive.getUniqueId();
                    tie = false;
                } else if (value == best && value > 0) {
                    tie = true;
                }
            }
        }
        if (!tie && player.getUniqueId().equals(bestId) && best > 0) {
            Slop.unlock(player, "sw.most-kills");
        }
        armor(player);
        kills.remove(player.getUniqueId());
        attacked.remove(player.getUniqueId());
        woreArmor.remove(player.getUniqueId());
        looted.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPearl(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            Slop.unlock(event.getPlayer(), "sw.ender");
        }
    }

    @EventHandler
    public void onPearlThrow(org.bukkit.event.entity.ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl)) {
            return;
        }
        ProjectileSource source = event.getEntity().getShooter();
        if (source instanceof Player) {
            Slop.unlock((Player) source, "sw.ender");
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile) {
            ProjectileSource source = ((org.bukkit.entity.Projectile) event.getDamager()).getShooter();
            if (source instanceof Player) {
                attacker = (Player) source;
            }
        }
        if (attacker != null && event.getEntity() instanceof Player && !attacker.equals(event.getEntity())) {
            attacked.put(attacker.getUniqueId(), Boolean.TRUE);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChestClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        final Player player = (Player) event.getWhoClicked();
        final GameMap map = playing(player);
        if (map == null) {
            return;
        }
        final Inventory top = event.getView().getTopInventory();
        final String key = centerKey(map, top);
        if (key == null) {
            return;
        }
        final int before = amount(top);
        final boolean swap = tookBySwap(event, top);
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                if (swap || amount(top) < before) {
                    markLooted(player, map, key);
                }
            }
        });
    }

    @EventHandler
    public void onChestClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        if (playing(player) != null) {
            armor(player);
        }
    }

    /**
     * Level thresholds. Called when XP is added and when a match starts, so the chat line
     * is not held until the winner screen.
     */
    public static void levels(Player player) {
        if (player == null) {
            return;
        }
        PlayerStat stat = PlayerStat.getPlayerStats(player.getUniqueId());
        if (stat == null) {
            return;
        }
        int level = LevelManager.get().getLevelForXp(stat.getXp());
        if (level >= 2) Slop.unlock(player, "sw.level.2");
        if (level >= 5) Slop.unlock(player, "sw.level.5");
        if (level >= 10) Slop.unlock(player, "sw.level.10");
        if (level >= 25) Slop.unlock(player, "sw.level.25");
        if (level >= 50) Slop.unlock(player, "sw.level.50");
    }

    private void markLooted(Player player, GameMap map, String key) {
        Set<String> opened = looted.get(player.getUniqueId());
        if (opened == null) {
            opened = new HashSet<String>();
            looted.put(player.getUniqueId(), opened);
        }
        opened.add(key);
        int mid = map.getCenterChests() == null ? 0 : map.getCenterChests().size();
        if (mid > 0 && opened.size() >= mid) {
            Slop.unlock(player, "sw.looter");
        }
    }

    private static GameMap playing(Player player) {
        GameMap map = MatchManager.get().getPlayerMap(player);
        if (map == null || map.getMatchState() != MatchState.PLAYING) {
            return null;
        }
        if (map.getAlivePlayers() == null || !map.getAlivePlayers().contains(player)) {
            return null;
        }
        return map;
    }

    private static String centerKey(GameMap map, Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof Chest) {
            return centerKey(map, (Chest) holder);
        }
        if (holder instanceof DoubleChest) {
            DoubleChest chest = (DoubleChest) holder;
            String left = centerKey(map, asChest(chest.getLeftSide()));
            if (left != null) {
                return left;
            }
            return centerKey(map, asChest(chest.getRightSide()));
        }
        return null;
    }

    private static Chest asChest(Object side) {
        if (side instanceof Chest) {
            return (Chest) side;
        }
        if (side instanceof Inventory) {
            InventoryHolder holder = ((Inventory) side).getHolder();
            if (holder instanceof Chest) {
                return (Chest) holder;
            }
        }
        return null;
    }

    private static String centerKey(GameMap map, Chest chest) {
        if (chest == null || map.getCenterChests() == null) {
            return null;
        }
        String key = chest.getX() + ":" + chest.getY() + ":" + chest.getZ();
        for (CoordLoc loc : map.getCenterChests()) {
            if (loc.getLocationString().equals(key)) {
                return key;
            }
        }
        return null;
    }

    private static boolean tookBySwap(InventoryClickEvent event, Inventory top) {
        if (event.getRawSlot() >= top.getSize()) {
            return false;
        }
        ItemStack current = event.getCurrentItem();
        if (!real(current)) {
            return false;
        }
        if (event.getClick() == ClickType.NUMBER_KEY) {
            return true;
        }
        ItemStack cursor = event.getCursor();
        return real(cursor) && cursor.getType() != current.getType();
    }

    private static int amount(Inventory inventory) {
        int total = 0;
        ItemStack[] contents = inventory.getContents();
        if (contents == null) {
            return 0;
        }
        for (ItemStack stack : contents) {
            if (real(stack)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private void armor(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack helm = inventory.getHelmet();
        ItemStack chest = inventory.getChestplate();
        ItemStack legs = inventory.getLeggings();
        ItemStack boots = inventory.getBoots();
        if (wearingArmor(player)) {
            woreArmor.put(player.getUniqueId(), Boolean.TRUE);
        }
        if (!diamond(helm) || !diamond(chest) || !diamond(legs) || !diamond(boots)) {
            return;
        }
        Slop.unlock(player, "sw.shiny");
        if (enchanted(helm) && enchanted(chest) && enchanted(legs) && enchanted(boots)) {
            Slop.unlock(player, "sw.extra-shiny");
        }
    }

    private static boolean wearingArmor(Player player) {
        PlayerInventory inventory = player.getInventory();
        return real(inventory.getHelmet()) || real(inventory.getChestplate())
                || real(inventory.getLeggings()) || real(inventory.getBoots());
    }

    private static boolean diamond(ItemStack stack) {
        return stack != null && stack.getType() != null && stack.getType().name().startsWith("DIAMOND_");
    }

    private static boolean enchanted(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        Map<Enchantment, Integer> enchants = stack.getEnchantments();
        return enchants != null && !enchants.isEmpty();
    }

    private static boolean real(ItemStack stack) {
        return stack != null && stack.getType() != Material.AIR;
    }
}
