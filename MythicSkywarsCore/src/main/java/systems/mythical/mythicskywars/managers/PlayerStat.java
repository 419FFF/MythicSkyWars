package systems.mythical.mythicskywars.managers;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.database.DataStorage;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.matchevents.MatchEvent;
import systems.mythical.mythicskywars.utilities.LevelManager;
import systems.mythical.mythicskywars.utilities.PrestigeManager;
import systems.mythical.mythicskywars.utilities.Util;
import systems.mythical.mythicskywars.utilities.VaultUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerStat {

    private static ArrayList<PlayerStat> players;
    private static HashMap<Player, MythicSkywarsBoard> scoreboards = new HashMap<>();

    static {
        PlayerStat.players = new ArrayList<>();
    }

    private final String uuid;
    private String playername;
    private int wins =0 ;
    private int losts =0 ;
    private int kills =0 ;
    private int deaths =0 ;
    private int xp =0 ;
    private String particleEffect = "none";
    private String projectileEffect = "none";
    private String glassColor = "none";
    private String killSound = "none";
    private String winSound = "none";
    private String taunt = "none";
    /** Cosmetic prestige id from {@code levels.yml}; displayed with level prefix when enabled. */
    private String prestigeIcon = "icon1";
    /** Banked Soul Well currency (separate from kill counter). */
    private int souls;
    private int soulWellUsages;
    private int soulWellLegendaries;
    private int soulWellRares;
    private int soulWellSoulsGathered;
    private int soulWellSoulsPurchased;
    /** One free Soul Well roll after a win with Xezbeth Luck (not persisted). */
    private transient boolean soulWellXezbethFreeRollPending;
    private volatile boolean initialized;
    private PermissionAttachment perms;

    public PlayerStat(UUID uuid, String name) {
        this.initialized = false;
        this.uuid = uuid.toString();
        this.playername = name;
        this.perms = null;
        this.souls = 0;
        this.soulWellUsages = 0;
        this.soulWellLegendaries = 0;
        this.soulWellRares = 0;
        this.soulWellSoulsGathered = 0;
        this.soulWellSoulsPurchased = 0;
    }

    public PlayerStat(final Player player) {
        this.initialized = false;
        this.uuid = player.getUniqueId().toString();
        this.playername = player.getName();
        this.souls = 0;
        this.soulWellUsages = 0;
        this.soulWellLegendaries = 0;
        this.soulWellRares = 0;
        this.soulWellSoulsGathered = 0;
        this.soulWellSoulsPurchased = 0;
        this.perms = player.addAttachment(MythicSkywars.get());
        if (MythicSkywars.getCfg().economyEnabled()) {
            DataStorage.get().loadperms(this);
        }
    }

    public void updatePlayerIfInLobby(Player player) {
        if (player == null || MythicSkywars.getCfg().getSpawn() == null) {
            return;
        }
        if (!player.getWorld().equals(MythicSkywars.getCfg().getSpawn().getWorld())) {
            return;
        }
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(MythicSkywars.get(), () -> updatePlayerIfInLobby(player));
            return;
        }
        if (isInitialized()) {
            applyLobbyJoinState(player);
        } else {
            updatePlayer(uuid);
        }
    }

    public void loadStats(Runnable postLoadStatsTask) {
        DataStorage.get().loadStats(this, () -> {
            this.setInitialized(true);
            Player player = null;
            try {
                player = Bukkit.getPlayer(UUID.fromString(this.uuid));
            } catch (IllegalArgumentException ignored) {
            }
            if (player != null && player.isOnline()) {
                updatePlayerIfInLobby(player);
            }
            try {
                MatchManager.get().onStatsReady(UUID.fromString(this.uuid));
            } catch (Exception ex) {
                MythicSkywars.get().getLogger().warning("Could not finish a queued SkyWars join for " + playername + ": " + ex.getMessage());
            }
            if (postLoadStatsTask != null) postLoadStatsTask.run();
        });
    }

    public static void updatePlayer(final String uuid) {
        if (MythicSkywars.get() == null || !MythicSkywars.get().isEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(MythicSkywars.get(), () -> {
            if (!MythicSkywars.get().isEnabled()) {
                return;
            }
            PlayerStat ps = PlayerStat.getPlayerStats(uuid);
            if (ps == null) {
                return;
            }
            Player player;
            try {
                player = Bukkit.getPlayer(UUID.fromString(uuid));
            } catch (IllegalArgumentException ex) {
                return;
            }
            if (player == null || !player.isOnline()) {
                return;
            }
            if (!ps.isInitialized()) {
                updatePlayer(uuid);
                return;
            }
            applyLobbyJoinState(player);
        }, 10L);
    }

    private static void applyLobbyJoinState(Player player) {
        if (player == null || !player.isOnline() || !Util.get().isSpawnWorld(player.getWorld())) {
            return;
        }
        MatchManager mm = MatchManager.get();
        if (mm.getPlayerMap(player) != null || mm.getSpectatorMap(player) != null) {
            return;
        }
        if (MythicSkywars.getCfg().isClearInventoryOnLobbyJoin()) {
            player.getInventory().clear();
        }
        if (MythicSkywars.getCfg().protectLobby()) {
            player.setGameMode(GameMode.ADVENTURE);
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(20);
            player.setFireTicks(0);
            player.resetPlayerTime();
            player.resetPlayerWeather();
        }
        PlayerStat pStats = PlayerStat.getPlayerStats(player);
        if (pStats != null && MythicSkywars.getCfg().displayPlayerExeperience()) {
            Util.get().setPlayerExperience(player, pStats.getXp());
        }
        if (MythicSkywars.get().isEnabled() && MythicSkywars.getCfg().lobbyBoardEnabled()) {
            updateScoreboard(player, "lobbyboard");
        }
        if (MythicSkywars.getCfg().optionsMenuEnabled() && MythicSkywars.getCfg().isOptionsItemEnabled()) {
            int optionSlot = MythicSkywars.getCfg().getOptionsSlot();
            if (MythicSkywars.getCfg().joinMenuEnabled() && MythicSkywars.getCfg().isJoinGameItemEnabled() && player.hasPermission("sw.join")) {
                int preferred = Math.min(8, MythicSkywars.getCfg().getJoinSlot() + 2);
                optionSlot = preferred;
            }
            player.getInventory().setItem(optionSlot, MythicSkywars.getIM().getItem("optionselect"));
        }
        if (MythicSkywars.getCfg().joinMenuEnabled() && MythicSkywars.getCfg().isJoinGameItemEnabled() && player.hasPermission("sw.join")) {
            player.getInventory().setItem(MythicSkywars.getCfg().getJoinSlot(), MythicSkywars.getIM().getItem("joinselect"));
        }
        if (MythicSkywars.getCfg().isStatsItemEnabled() && player.hasPermission("sw.stats")) {
            player.getInventory().setItem(MythicSkywars.getCfg().getStatsItemPos(), MythicSkywars.getIM().getItem("statsitem"));
        }
        if (MythicSkywars.getCfg().isBungeeEnabled() && MythicSkywars.getCfg().isBackToLobbyItemEnabled()) {
            player.getInventory().setItem(MythicSkywars.getCfg().getBackToLobbyPos(), MythicSkywars.getIM().getItem("backlobbyitem"));
        }
        if (!MythicSkywars.getCfg().bungeeMode() && MythicSkywars.getCfg().isSpectateGameItemEnabled() && MythicSkywars.getCfg().spectateMenuEnabled() && player.hasPermission("sw.spectate")) {
            player.getInventory().setItem(MythicSkywars.getCfg().getSpectateSlot(), MythicSkywars.getIM().getItem("spectateselect"));
        }
        player.updateInventory();
    }

    public static ArrayList<PlayerStat> getPlayers() {
        return PlayerStat.players;
    }

    public static void setPlayers(final ArrayList<PlayerStat> playerData) {
        PlayerStat.players = playerData;
    }

    public static PlayerStat getPlayerStats(final String playerData) {
        PlayerStat fallback = null;
        for (final PlayerStat pData : getPlayers()) {
            if (pData.getId().equals(playerData)) {
                if (pData.isInitialized()) {
                    return pData;
                }
                if (fallback == null) {
                    fallback = pData;
                }
            }
        }
        return fallback;
    }

    public static PlayerStat getPlayerStats(final Player player) {
        return getPlayerStats(player.getUniqueId());
    }

    public static PlayerStat getPlayerStats(final UUID uuid) {
        PlayerStat fallback = null;
        for (final PlayerStat pData : getPlayers()) {
            if (pData.getId().equals(uuid.toString())) {
                if (pData.isInitialized()) {
                    return pData;
                }
                if (fallback == null) {
                    fallback = pData;
                }
            }
        }
        return fallback;
    }

    public static void updateScoreboard(Player player, String identifier) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (identifier == null || identifier.isEmpty()) {
            identifier = "lobbyboard";
        }
        List<String> lines;
        try {
            lines = MythicSkywars.getMessaging().getFile().getStringList("scoreboards." + identifier + ".lines");
        } catch (Exception ex) {
            return;
        }
        if (lines == null || lines.isEmpty()) {
            return;
        }

        ArrayList<String> scores = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw == null) {
                continue;
            }
            String formatted;
            try {
                formatted = ChatColor.translateAlternateColorCodes('&', getScoreboardLine(raw, player, identifier));
            } catch (Exception ex) {
                formatted = "";
            }
            if (formatted == null) {
                formatted = "";
            }
            if (!ChatColor.stripColor(formatted).contains("remove")) {
                scores.add(formatted);
            }
        }
        if (scores.size() > 15) {
            scores.subList(15, scores.size()).clear();
        }
        if (scores.isEmpty()) {
            return;
        }

        MythicSkywarsBoard existing = scoreboards.get(player);
        String title = safeTitle(lines.get(0));
        if (existing != null && existing.getLinecount() == scores.size()) {
            try {
                existing.setTitle(title);
                for (int i = 0; i < scores.size(); i++) {
                    String line = scores.get(i);
                    if (!existing.getLine(i).equals(line)) {
                        existing.setLine(i, line);
                    }
                }
                return;
            } catch (Exception ex) {
                MythicSkywars.get().getLogger().warning("Rebuilding scoreboard for " + player.getName() + ": " + ex.getMessage());
            }
        }

        MythicSkywarsBoard next;
        try {
            next = new MythicSkywarsBoard(player, scores.size());
            next.setTitle(title);
            for (int i = 0; i < scores.size(); i++) {
                next.setLine(i, scores.get(i));
            }
        } catch (Exception ex) {
            MythicSkywars.get().getLogger().warning("Could not update scoreboard for " + player.getName() + ": " + ex.getMessage());
            if (existing != null && existing.board != null) {
                player.setScoreboard(existing.board);
            }
            return;
        }
        resetScoreboard(player);
        scoreboards.put(player, next);
    }

    private static String safeTitle(String raw) {
        String title = ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }
        return title;
    }

    private static String getScoreboardLine(String line, Player player, String identifier) {
        PlayerStat ps = PlayerStat.getPlayerStats(player);
        String killdeath;
        String winloss;
        if (ps != null) {
            winloss = formatRatio(ps.getWins(), ps.getLosses());
            killdeath = formatRatio(ps.getKills(), ps.getDeaths());

            GameMap gMap = MatchManager.get().getPlayerMap(player);
            if (identifier.equals("lobbyboard") || gMap == null) {
                int xp = ps.getXp();
                int level = LevelManager.get().getLevelForXp(xp);
                int nextLevel = LevelManager.get().getNextLevel(level);
                int xpToNext = LevelManager.get().getXpToNextLevel(xp);
                int xpIntoLevel = LevelManager.get().getXpIntoCurrentLevel(xp);
                int xpNeedForLevel = LevelManager.get().getXpRequiredForCurrentLevel(xp);
                int progressPct = LevelManager.get().getProgressPercent(xp);
                String formatted = line
                        .replace("{wins}", Integer.toString(ps.getWins()))
                        .replace("{losses}", Integer.toString(ps.getLosses()))
                        .replace("{kills}", Integer.toString(ps.getKills()))
                        .replace("{deaths}", Integer.toString(ps.getDeaths()))
                        .replace("{souls}", Integer.toString(ps.getSouls()))
                        .replace("{soulwell_usages}", Integer.toString(ps.getSoulWellUsages()))
                        .replace("{soulwell_legendaries}", Integer.toString(ps.getSoulWellLegendaries()))
                        .replace("{soulwell_rares}", Integer.toString(ps.getSoulWellRares()))
                        .replace("{soulwell_souls_gathered}", Integer.toString(ps.getSoulWellSoulsGathered()))
                        .replace("{soulwell_souls_purchased}", Integer.toString(ps.getSoulWellSoulsPurchased()))
                        .replace("{xp}", Integer.toString(xp))
                        .replace("{level}", Integer.toString(level))
                        .replace("{next_level}", Integer.toString(nextLevel))
                        .replace("{xp_to_next}", Integer.toString(xpToNext))
                        .replace("{xp_current_level}", Integer.toString(xpIntoLevel))
                        .replace("{xp_next_level}", Integer.toString(xpNeedForLevel))
                        .replace("{level_progress}", Integer.toString(progressPct))
                        .replace("{killdeath}", killdeath)
                        .replace("{winloss}", winloss)
                        .replace("{balance}", "" + getBalance(player))
                        .replace("{level_prefix}", LevelManager.get().getPrefixForLevel(level))
                        .replace("{prestige_prefix}", PrestigeManager.get().isEnabled()
                                ? PrestigeManager.get().translatePrefixForStat(ps, level, ps.getPrestigeIcon()) : "")
                        .replace("{level_display_prefix}", LevelManager.get().getDisplayPrefixForPlayer(ps, player, level))
                        .replace("{level_progress_bar}", LevelManager.get().getProgressBar(xp));
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    formatted = PlaceholderAPI.setPlaceholders(player, formatted);
                }
                return formatted;
            }
            else {
                int currentPlayers;
                if (gMap.getMatchState()== MatchState.WAITINGLOBBY) currentPlayers = gMap.getWaitingPlayers().size();
                else if (gMap.getMatchState() == MatchState.ENDING) currentPlayers = gMap.getAllPlayers().size();
                else currentPlayers = gMap.getAlivePlayers().size();

                MatchEvent nextEvent = gMap.getNextEvent();
                String eventTime = "";
                String eventName = ChatColor.RED + "No events";
                if (nextEvent != null) {
                    eventName = nextEvent.getTitle();
                    eventTime = Util.get().secondsToTimeString(nextEvent.getStartTime() - gMap.getTimer());
                }

                String formatted = line
                        .replace("{players_needed}", (gMap.getAllPlayers().size() >= gMap.getMinTeams() ? "0" : gMap.getMinTeams()-gMap.getAllPlayers().size()) + "")
                        .replace("{waitingtimer}", Util.get().getFormattedTime(gMap.getTimer()))
                        .replace("{nextevent_time}", eventTime)
                        .replace("{nextevent_name}", eventName)
                        .replace("{kills}", gMap.getPlayerKills(player) + "")
                        .replace("{mapname}", gMap.getDisplayName())
                        .replace("{time}", "" + Util.get().getFormattedTime(gMap.getTimer()))
                        .replace("{aliveplayers}", "" + gMap.getAlivePlayers().size())
                        .replace("{players}", "" + currentPlayers)
                        .replace("{maxplayers}", "" + gMap.getMaxPlayers())
                        .replace("{winner}", MythicSkywars.getCfg().usePlayerNames() ? getWinnerName(gMap,0) : getWinningTeamName(gMap))
                        .replace("{winner1}", MythicSkywars.getCfg().usePlayerNames() ? getWinnerName(gMap,0) : getWinningTeamName(gMap))
                        .replace("{winner2}", MythicSkywars.getCfg().usePlayerNames() ? getWinnerName(gMap,1) : "remove")
                        .replace("{winner3}", MythicSkywars.getCfg().usePlayerNames() ? getWinnerName(gMap,2) : "remove")
                        .replace("{winner4}", MythicSkywars.getCfg().usePlayerNames() ? getWinnerName(gMap,3) : "remove")
                        .replace("{winner5}", MythicSkywars.getCfg().usePlayerNames() ? getWinnerName(gMap,4) : "remove")
                        .replace("{winner6}", MythicSkywars.getCfg().usePlayerNames() ? getWinnerName(gMap,5) : "remove")
                        .replace("{winner7}", MythicSkywars.getCfg().usePlayerNames() ? getWinnerName(gMap,6) : "remove")
                        .replace("{winner8}", MythicSkywars.getCfg().usePlayerNames() ? getWinnerName(gMap,7) : "remove")
                        .replace("{restarttime}", "" + gMap.getGameBoard().getRestartTimer())
                        .replace("{chestvote}", ChatColor.stripColor(safeText(gMap.getCurrentChest())))
                        .replace("{timevote}", ChatColor.stripColor(safeText(gMap.getCurrentTime())))
                        .replace("{healthvote}", ChatColor.stripColor(safeText(gMap.getCurrentHealth())))
                        .replace("{weathervote}", ChatColor.stripColor(safeText(gMap.getCurrentWeather())))
                        .replace("{modifiervote}", ChatColor.stripColor(safeText(gMap.getCurrentModifier())));
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    formatted = PlaceholderAPI.setPlaceholders(player, formatted);
                }
                return formatted;
            }
        }
        return "";
    }

    private static String getWinnerName(GameMap gMap, int i) {
        if (gMap.getWinners().size() > i) {
            return gMap.getWinners().get(i);
        }
        return "remove";
    }
    private static String getWinningTeamName(GameMap gMap) {
        if (gMap.getWinningTeam() != null) {
            return gMap.getWinningTeam().getTeamName();
        }
        return "invalid";
    }

    private static String formatRatio(int numerator, int denominator) {
        if (numerator <= 0 || denominator <= 0) {
            return "0.00";
        }
        return String.format("%1$,.2f", (double) numerator / (double) denominator);
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private static double getBalance(Player player) {
        if (MythicSkywars.getCfg().economyEnabled()) {
            return VaultUtils.get().getBalance(player);
        }
        return 0;
    }

    public static void resetScoreboard(Player player) {
        MythicSkywarsBoard scoreboard = scoreboards.get(player);
        if (scoreboard != null) {
            for (Objective objective : scoreboard.board.getObjectives()) {
                if (objective != null) {
                    objective.unregister();
                }
            }
            for (Team team : scoreboard.board.getTeams()) {
                if (team != null) team.unregister();
            }
        }
        scoreboards.remove(player);
    }

    private static MythicSkywarsBoard getPlayerScoreboard(Player player) {
        return scoreboards.get(player);
    }

    public static void removePlayer(String id) {
        if (id == null) {
            return;
        }
        for (int i = players.size() - 1; i >= 0; i--) {
            PlayerStat ps = players.get(i);
            if (id.equals(ps.getId())) {
                players.remove(i);
            }
        }
    }

    /**
     * Save player data without callback task
     */
    public void saveStats() {
        this.saveStats(null);
    }

    /**
     * Save player data and call runnable when done
     * @param postSaveStatsTask Runnable to run when saving is done
     */
    public void saveStats(Runnable postSaveStatsTask) {
        Player player = MythicSkywars.get().getServer().getPlayer(UUID.fromString(uuid));
        String playerName = player != null ? player.getName() : uuid;
        Bukkit.getLogger().log(Level.INFO, "Now saving stats of player " + playerName);

        saveStatsNow();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (postSaveStatsTask != null) postSaveStatsTask.run();
            }
        }.runTask(MythicSkywars.get());
    }

    private void saveStatsNow() {
        DataStorage.get().saveStats(this);
    }

    public String getId() {
        return this.uuid;
    }

    public int getWins() {
        return this.wins;
    }

    public void setWins(final int a1) {
        this.wins = a1;
    }

    public int getKills() {
        return this.kills;
    }

    public void setKills(final int a1) {
        this.kills = a1;
    }

    public int getXp() {
        return this.xp;
    }

    public void setXp(int x) {
        this.xp = x;
    }

    /**
     * Adds XP and runs {@link LevelManager#grantLevelUpRewards} when {@code onlinePlayer} is non-null
     * (normal gameplay — setXp alone skips automatic rewards for admin edits).
     */
    public void addXp(Player onlinePlayer, int delta) {
        if (delta == 0) {
            return;
        }
        int oldXp = this.xp;
        this.xp = Math.max(0, this.xp + delta);
        if (onlinePlayer != null && this.xp > oldXp) {
            LevelManager.get().grantLevelUpRewards(onlinePlayer, oldXp, this.xp);
            systems.mythical.mythicskywars.addons.SkywarsAchievements.levels(onlinePlayer);
        }
    }

    public String getPrestigeIcon() {
        return prestigeIcon != null && !prestigeIcon.isEmpty() ? prestigeIcon : "icon1";
    }

    public void setPrestigeIcon(String prestigeIcon) {
        this.prestigeIcon = prestigeIcon != null && !prestigeIcon.trim().isEmpty() ? prestigeIcon.trim() : "icon1";
    }

    public int getDeaths() {
        return this.deaths;
    }

    public void setDeaths(final int a1) {
        this.deaths = a1;
    }

    public int getLosses() {
        return this.losts;
    }

    public void setLosts(final int a1) {
        this.losts = a1;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public void setInitialized(final boolean a1) {
        this.initialized = a1;
    }

    public void clear() {
        this.losts = 0;
        this.wins = 0;
        this.kills = 0;
        this.deaths = 0;
        this.souls = 0;
        this.soulWellUsages = 0;
        this.soulWellLegendaries = 0;
        this.soulWellRares = 0;
        this.soulWellSoulsGathered = 0;
        this.soulWellSoulsPurchased = 0;
    }

    public int getSouls() {
        return souls;
    }

    public void setSouls(int souls) {
        this.souls = Math.max(0, souls);
    }

    public void addSouls(int delta) {
        this.souls = Math.max(0, this.souls + delta);
    }

    public int getSoulWellUsages() {
        return soulWellUsages;
    }

    public void setSoulWellUsages(int soulWellUsages) {
        this.soulWellUsages = Math.max(0, soulWellUsages);
    }

    public void addSoulWellUsages(int delta) {
        this.soulWellUsages = Math.max(0, this.soulWellUsages + delta);
    }

    public int getSoulWellLegendaries() {
        return soulWellLegendaries;
    }

    public void setSoulWellLegendaries(int soulWellLegendaries) {
        this.soulWellLegendaries = Math.max(0, soulWellLegendaries);
    }

    public void addSoulWellLegendaries(int delta) {
        this.soulWellLegendaries = Math.max(0, this.soulWellLegendaries + delta);
    }

    public int getSoulWellRares() {
        return soulWellRares;
    }

    public void setSoulWellRares(int soulWellRares) {
        this.soulWellRares = Math.max(0, soulWellRares);
    }

    public void addSoulWellRares(int delta) {
        this.soulWellRares = Math.max(0, this.soulWellRares + delta);
    }

    public int getSoulWellSoulsGathered() {
        return soulWellSoulsGathered;
    }

    public void setSoulWellSoulsGathered(int soulWellSoulsGathered) {
        this.soulWellSoulsGathered = Math.max(0, soulWellSoulsGathered);
    }

    public void addSoulWellSoulsGathered(int delta) {
        this.soulWellSoulsGathered = Math.max(0, this.soulWellSoulsGathered + delta);
    }

    public int getSoulWellSoulsPurchased() {
        return soulWellSoulsPurchased;
    }

    public void setSoulWellSoulsPurchased(int soulWellSoulsPurchased) {
        this.soulWellSoulsPurchased = Math.max(0, soulWellSoulsPurchased);
    }

    public void addSoulWellSoulsPurchased(int delta) {
        this.soulWellSoulsPurchased = Math.max(0, this.soulWellSoulsPurchased + delta);
    }

    public boolean hasSoulWellXezbethFreeRollPending() {
        return soulWellXezbethFreeRollPending;
    }

    public void setSoulWellXezbethFreeRollPending(boolean soulWellXezbethFreeRollPending) {
        this.soulWellXezbethFreeRollPending = soulWellXezbethFreeRollPending;
    }

    public String getParticleEffect() {
        return particleEffect;
    }

    public void setParticleEffect(String effect) {
        this.particleEffect = effect;
    }

    public String getProjectileEffect() {
        return projectileEffect;
    }

    public void setProjectileEffect(String effect) {
        this.projectileEffect = effect;
    }

    public String getGlassColor() {
        return this.glassColor;
    }

    public void setGlassColor(String glassC) {
        this.glassColor = glassC;
    }

    //Scoreboard Methods

    public String getKillSound() {
        return this.killSound;
    }

    public void setKillSound(String glassC) {
        this.killSound = glassC;
    }

    public String getWinSound() {
        return this.winSound;
    }

    public void setWinSound(String string) {
        this.winSound = string;
    }

    public String getPlayerName() {
        return playername;
    }

    public void setPlayerName(String nameIn) {
        this.playername = nameIn;
    }

    public String getTaunt() {
        return taunt;
    }

    public void setTaunt(String string) {
        taunt = string;
    }

    public PermissionAttachment getPerms() {
        return perms;
    }

    public void addPerm(String perm, boolean save) {
        perms.setPermission(perm, true);
        if (save) {
            DataStorage.get().savePerms(this);
        }
    }
}