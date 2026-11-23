package systems.mythical.mythicskywars.managers;

import systems.mythical.mythicskywars.MythicSkywars;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;

public class MythicSkywarsBoard {

    private final Player player;
    private final Objective objective;
    private final int lineCount;
    private final HashMap<Integer, String> cache = new HashMap<>();
    public Scoreboard board;

    public MythicSkywarsBoard(Player player, int lineCount) {
        this.player = player;
        // 1.8 hides a score of 0, and only the 16 real colors are safe team entries.
        // Format codes after those colors make the client drop the sidebar.
        if (lineCount < 1) {
            lineCount = 1;
        }
        if (lineCount > 15) {
            lineCount = 15;
        }
        this.lineCount = lineCount;
        this.board = MythicSkywars.get().getServer().getScoreboardManager().getNewScoreboard();
        this.objective = this.board.registerNewObjective("sb1", "dummy");
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.objective.setDisplayName("...");

        int score = lineCount;
        for (int i = 0; i < lineCount; i++) {
            Team t = this.board.registerNewTeam(Integer.toString(i));
            String entry = ChatColor.values()[i].toString();
            t.addEntry(entry);
            this.objective.getScore(entry).setScore(score);
            score--;
        }
        this.player.setScoreboard(this.board);
    }

    public void setTitle(String title) {
        if (title == null) {
            title = "";
        }
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }
        title = stripTrailingSection(title);

        if (cache.containsKey(-1) && cache.get(-1).equals(title)) return;
        cache.put(-1, title);
        objective.setDisplayName(title);
    }

    public void setLine(int line, String text) {
        Team team = board.getTeam(Integer.toString(line));
        if (team == null) {
            return;
        }
        if (text == null) {
            text = "";
        }

        if (text.contains("%") && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        if (cache.containsKey(line) && cache.get(line).equals(text)) return;
        cache.put(line, text);

        int limit = MythicSkywars.getNMS().getVersion() > 12 ? 64 : 16;
        ArrayList<String> pieces = convertIntoPieces(text, limit);
        team.setPrefix(pieces.get(0));
        team.setSuffix(pieces.get(1));
    }

    /**
     * Splits a sidebar line into a prefix and suffix. A piece that ends in a lone
     * section sign makes the 1.8 client drop the whole sidebar.
     */
    private ArrayList<String> convertIntoPieces(String text, int limit) {
        ArrayList<String> pieces = new ArrayList<String>();
        if (text == null) {
            text = "";
        }
        if (limit < 1) {
            limit = 16;
        }
        if (text.length() <= limit) {
            pieces.add(stripTrailingSection(text));
            pieces.add("");
            return pieces;
        }

        String prefix = stripTrailingSection(text.substring(0, limit));
        String colors = ChatColor.getLastColors(prefix);
        String suffix = colors + text.substring(prefix.length());
        if (suffix.length() > limit) {
            suffix = suffix.substring(0, limit);
        }
        pieces.add(prefix);
        pieces.add(stripTrailingSection(suffix));
        return pieces;
    }

    private static String stripTrailingSection(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        while (text.endsWith("\u00A7")) {
            text = text.substring(0, text.length() - 1);
            if (text.isEmpty()) {
                return "";
            }
        }
        return text;
    }

    public int getLinecount() {
        return lineCount;
    }

    public String getLine(int number) {
        return cache.getOrDefault(number, "");
    }
}
