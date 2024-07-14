package popka.gayzone;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public final class Gayzone extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent ev) {
        Player player = ev.getPlayer();
        String playerName = player.getName();
        player.sendMessage(String.format("Прив, %s)", playerName));
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent ev) {
        Player player = ev.getPlayer();
        String playerName = player.getName();
        double posx = player.getLocation().getX();
        double posy = player.getLocation().getY();
        double posz = player.getLocation().getZ();
        System.out.println(String.format("Игрок %s вышел на координатах (%.2f, %.2f, %.2f)", playerName, posx, posy, posz));
    }

    private void scheduleAutoMessages() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                List<Player> players = Bukkit.getOnlinePlayers().stream().collect(Collectors.toList());
                if (!players.isEmpty()) {
                    Random random = new Random();
                    Player randomPlayer = players.get(random.nextInt(players.size()));
                    String message = String.format("А вы знали, что %s главный гей этого сервера?", randomPlayer.getName());
                    Bukkit.broadcastMessage(message);
                }
            }
        }, 0L, 6000L); // 0L is the delay before the first run, 6000L is the period (6000 ticks = 5 minutes)
    }
}
