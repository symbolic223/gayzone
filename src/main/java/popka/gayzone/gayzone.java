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
import java.util.Map;

public final class gayzone extends JavaPlugin implements Listener {

    private final Map<String, String> playerPrefixes = new HashMap<>();
    private final Gson gson = new Gson();
    private final File prefixesFile = new File(getDataFolder(), "prefixes.json");

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getPluginManager().registerEvents(this, this);
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("setprefix")) {
            if (!sender.hasPermission("gayzone.setprefix")) {
                sender.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды.");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Использование: /setprefix <игрок> <префикс> <цвет> [<формат>...]");
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

            StringBuilder formattedPrefix = new StringBuilder(color.toString()).append("[").append(prefix).append("]").append(ChatColor.RESET);

            if (args.length > 3) {
                for (int i = 3; i < args.length; i++) {
                    try {
                        ChatColor format = ChatColor.valueOf(args[i].toUpperCase());
                        if (format.isFormat()) {
                            formattedPrefix.insert(color.toString().length(), format.toString());
                        } else {
                            sender.sendMessage(ChatColor.RED + "Неверный формат. Используйте один из следующих форматов: " + getFormatList());
                            return true;
                        }
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(ChatColor.RED + "Неверный формат. Используйте один из следующих форматов: " + getFormatList());
                        return true;
                    }
                }
            }

            playerPrefixes.put(targetPlayer.getName(), formattedPrefix.toString());
            setPlayerPrefix(targetPlayer);
            savePrefixes();  // Сохранить префиксы сразу после установки
            sender.sendMessage(ChatColor.GREEN + "Префикс для " + targetPlayerName + " установлен на: " + formattedPrefix);
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

    private String getFormatList() {
        StringBuilder formats = new StringBuilder();
        for (ChatColor format : ChatColor.values()) {
            if (!format.isFormat()) continue; // Пропустить цвета текста
            formats.append(format.name()).append(", ");
        }
        return formats.toString().substring(0, formats.length() - 2); // Удалить последнее ", "
    }
}
