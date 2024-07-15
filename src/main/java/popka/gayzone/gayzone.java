package popka.gayzone;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public final class gayzone extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getPluginManager().registerEvents(this, this);
        scheduleAutoMessages();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent ev) {
        Player player = ev.getPlayer();
        String playerName = player.getName();
        Integer playerCount = Bukkit.getOnlinePlayers().size();
        player.sendMessage(String.format("Прив, %s)", playerName));
        if (Bukkit.getOnlinePlayers().size() > 4) {
            player.sendMessage(String.format("Текущее кол-во игроков = %d", playerCount));
            player.sendMessage("Проксимити чат - включен.");
        }
        else {
            player.sendMessage(String.format("Текущее кол-во игроков = %d", playerCount));
            player.sendMessage("Проксимити чат - выключен.");
        }
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
        }, 0L, 6000L);
    }

    private static final double CHAT_RADIUS = 50.0;

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String message = event.getMessage();
        Integer playerCount = Bukkit.getOnlinePlayers().size();
        if (message.startsWith("!") && playerCount > 4) {
            event.setMessage(message.substring(1));
            return;
        }

        if (playerCount < 4) {
            event.getRecipients().addAll(Bukkit.getOnlinePlayers());
            return;
        }

        double chatRadiusSquared = CHAT_RADIUS * CHAT_RADIUS;


        event.getRecipients().clear();
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (recipient.getWorld().equals(sender.getWorld()) &&
                    recipient.getLocation().distanceSquared(sender.getLocation()) <= chatRadiusSquared) {
                event.getRecipients().add(recipient);
            }
        }
    }
}