package popka.gayzone;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public final class gayzone extends JavaPlugin implements Listener {

    private final Map<String, String> playerPrefixes = new HashMap<>();
    private final Gson gson = new Gson();
    private final File prefixesFile = new File(getDataFolder(), "prefixes.json");

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getPluginManager().registerEvents(this, this);
        scheduleAutoMessages();
        loadPrefixes();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        savePrefixes();
    }

    private void loadPrefixes() {
        if (!prefixesFile.getParentFile().exists()) {
            prefixesFile.getParentFile().mkdirs();
        }

        if (prefixesFile.exists()) {
            try (FileReader reader = new FileReader(prefixesFile)) {
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> loadedPrefixes = gson.fromJson(reader, type);
                playerPrefixes.putAll(loadedPrefixes);
            } catch (IOException e) {
                getLogger().severe("Не удалось загрузить префиксы: " + e.getMessage());
            }
        }
    }

    private void savePrefixes() {
        if (!prefixesFile.getParentFile().exists()) {
            prefixesFile.getParentFile().mkdirs();
        }

        try (FileWriter writer = new FileWriter(prefixesFile)) {
            gson.toJson(playerPrefixes, writer);
        } catch (IOException e) {
            getLogger().severe("Не удалось сохранить префиксы: " + e.getMessage());
        }
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
        } else {
            player.sendMessage(String.format("Текущее кол-во игроков = %d", playerCount));
            player.sendMessage("Проксимити чат - выключен.");
        }

        // Установить префикс при входе
        setPlayerPrefix(player);
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

        // Установить формат сообщений
        String prefix = playerPrefixes.getOrDefault(sender.getName(), "");
        event.setFormat(prefix + " " + sender.getName() + ": " + message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("setprefix")) {
            if (!sender.hasPermission("gayzone.setprefix")) {
                sender.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды.");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Использование: /setprefix <игрок> <префикс> <цвет>");
                return true;
            }

            String targetPlayerName = args[0];
            String prefix = args[1];
            String colorName = args[2].toUpperCase();

            Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
            if (targetPlayer == null) {
                sender.sendMessage(ChatColor.RED + "Игрок не найден.");
                return true;
            }

            ChatColor color;
            try {
                color = ChatColor.valueOf(colorName);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Неверный цвет. Используйте один из следующих цветов: " + getColorList());
                return true;
            }

            String coloredPrefix = color + "[" + prefix + "]" + ChatColor.RESET;
            playerPrefixes.put(targetPlayer.getName(), coloredPrefix);
            setPlayerPrefix(targetPlayer);
            savePrefixes();
            sender.sendMessage(ChatColor.GREEN + "Префикс для " + targetPlayerName + " установлен на: " + coloredPrefix);
            return true;
        }
        return false;
    }

    private void setPlayerPrefix(Player player) {
        String prefix = playerPrefixes.getOrDefault(player.getName(), "");
        if (!prefix.isEmpty()) {
            player.setDisplayName(prefix + " " + player.getName());
            player.setPlayerListName(prefix + " " + player.getName());
        }
    }

    private String getColorList() {
        StringBuilder colors = new StringBuilder();
        for (ChatColor color : ChatColor.values()) {
            if (color.isFormat()) continue; // Пропустить форматы текста (BOLD, ITALIC и т.д.)
            colors.append(color.name()).append(", ");
        }
        return colors.toString().substring(0, colors.length() - 2); // Удалить последнее ", "
    }
}
