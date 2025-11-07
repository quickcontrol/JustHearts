package me.havethacourage.justhearts;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener, TabExecutor {

    private FileConfiguration config;
    private FileConfiguration data;
    private File dataFile;
    private final Map<UUID, Integer> permanentHearts = new HashMap<>();
    private final Map<UUID, Integer> levelBonusCache = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadData();

        Bukkit.getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("hearts")).setExecutor(this);
        getCommand("hearts").setTabCompleter(this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            updateLevelBonus(player);
            updateHearts(player, false);
        }
        sendUsageStats();
        checkForUpdates();
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "JustHearts enabled!");
    }

    @Override
    public void onDisable() {
        saveData();
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "JustHearts disabled!");
    }

    // ---------------- Events ----------------

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!permanentHearts.containsKey(uuid)) {
            permanentHearts.put(uuid, 10);
            saveData(); // зберігаємо в data.yml
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            updateLevelBonus(player);
            updateHearts(player, false);
        }, 5L);
    }


    @EventHandler
    public void onLevelChange(PlayerLevelChangeEvent event) {
        updateLevelBonus(event.getPlayer());
        updateHearts(event.getPlayer(), true);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        updateHearts(event.getPlayer(), false);
    }
    private void sendUsageStats() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                java.net.URL url = new java.net.URL("https://api.havethacourage.me/justhearts/stats"); // твой URL
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String serverId = Bukkit.getServer().getIp() + ":" + Bukkit.getServer().getPort();
                String body = "server=" + serverId + "&version=" + getDescription().getVersion();

                conn.getOutputStream().write(body.getBytes());
                conn.getInputStream().close();
                conn.disconnect();

                Bukkit.getLogger().info("[JustHearts] Sent usage stats ✅");
            } catch (Exception e) {
                Bukkit.getLogger().warning("[JustHearts] Failed to send usage stats ❌");
            }
        });
    }
    private void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                java.net.URL url = new java.net.URL("https://raw.githubusercontent.com/havethacourage/JustHearts/main/version.txt");
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(url.openStream()));
                String latestVersion = reader.readLine().trim();
                reader.close();

                String current = getDescription().getVersion();
                if (!current.equalsIgnoreCase(latestVersion)) {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[JustHearts] Доступно оновлення: "
                            + ChatColor.AQUA + latestVersion + ChatColor.YELLOW + " (ваша версія: " + current + ")");
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "→ Завантажте нову з GitHub: https://github.com/havethacourage/JustHearts/releases");
                } else {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[JustHearts] Ви використовуєте останню версію.");
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[JustHearts] Неможливо перевірити оновлення.");
            }
        });
    }

    // ---------------- Heart Logic ----------------

    private void updateLevelBonus(Player player) {
        int bonus = 0;
        ConfigurationSection levelHearts = config.getConfigurationSection("level-hearts");
        if (levelHearts != null) {
            for (String lvlStr : levelHearts.getKeys(false)) {
                int lvl = Integer.parseInt(lvlStr);
                if (player.getLevel() >= lvl) {
                    bonus += levelHearts.getInt(lvlStr, 0);
                }
            }
        }
        levelBonusCache.put(player.getUniqueId(), bonus);
    }

    private void updateHearts(Player player, boolean showMessage) {
        UUID uuid = player.getUniqueId();
        int permHearts = permanentHearts.getOrDefault(uuid, 0);
        int levelBonus = levelBonusCache.getOrDefault(uuid, 0);
        int totalHearts = permHearts + levelBonus;
        double newMax = Math.min(totalHearts * 2.0, config.getInt("max-hearts", 40));

        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            double current = attr.getBaseValue();
            if (current != newMax) {
                attr.setBaseValue(newMax);
                if (player.getHealth() > newMax) player.setHealth(newMax);

                if (showMessage) {
                    if (newMax > current) {
                        sendMessage(player, config.getString("messages.gain"), config.getString("message-type.gain"));
                        playSound(player, "gain");
                        spawnParticle(player, "gain");
                    } else if (newMax < current) {
                        sendMessage(player, config.getString("messages.lose"), config.getString("message-type.lose"));
                        playSound(player, "lose");
                        spawnParticle(player, "lose");
                    }
                }
            }
        }
    }

    // ---------------- Particles & Sounds ----------------

    private void spawnParticle(Player player, String key) {
        if (!config.getBoolean("particles." + key + ".enabled", true)) return;

        String name = config.getString("particles." + key + ".name", "HEART").toUpperCase();
        int count = config.getInt("particles." + key + ".count", 10);
        double spread = config.getDouble("particles." + key + ".spread", 0.5);

        try {
            Particle particle = Particle.valueOf(name);
            player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count, spread, spread, spread, 0);
        } catch (IllegalArgumentException e) {
            Bukkit.getConsoleSender().sendMessage(getPrefix() + ChatColor.DARK_RED + "Invalid particle name: " + name);
        }
    }

    private void playSound(Player player, String key) {
        if (!config.getBoolean("sounds." + key + ".enabled", true)) return;

        String name = config.getString("sounds." + key + ".name", "");
        float volume = (float) config.getDouble("sounds." + key + ".volume", 1.0);
        float pitch = (float) config.getDouble("sounds." + key + ".pitch", 1.0);

        try {
            Sound sound = Sound.valueOf(name.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            Bukkit.getConsoleSender().sendMessage(getPrefix() + ChatColor.DARK_RED + "Invalid sound name: " + name);
        }
    }

    private void sendMessage(Player player, String message, String type) {
        if (message == null) return;
        message = ChatColor.translateAlternateColorCodes('&', message);
        if (type == null) type = "chat";

        switch (type.toLowerCase()) {
            case "actionbar" -> player.sendActionBar(message);
            case "title" -> player.sendTitle(message, "", 10, 40, 10);
            default -> player.sendMessage(message);
        }
    }

    private String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("prefix", "&fᴊᴜꜱᴛ &cʜᴇᴀʀᴛꜱ &7| &f"));
    }

    private void sendPrefixedMessage(CommandSender sender, String message) {
        sender.sendMessage(getPrefix() + ChatColor.translateAlternateColorCodes('&', message));
    }
    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    // ---------------- Data Handling ----------------

    private void loadData() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) saveResource("data.yml", false);

        data = YamlConfiguration.loadConfiguration(dataFile);
        if (data.contains("hearts")) {
            ConfigurationSection heartsSection = data.getConfigurationSection("hearts");
            for (String uuidStr : heartsSection.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                int value = heartsSection.getConfigurationSection(uuidStr).getInt("amount");
                permanentHearts.put(uuid, value);
            }
        }
    }

    private void saveData() {
        for (UUID uuid : permanentHearts.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            ConfigurationSection section = data.createSection("hearts." + uuid.toString());
            section.set("amount", permanentHearts.get(uuid));
        }
        try { data.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void changeHearts(Player player, int amount, boolean add) {
        UUID uuid = player.getUniqueId();
        int current = permanentHearts.getOrDefault(uuid, 0);
        int maxHearts = config.getInt("max-hearts", 40) / 2;

        int newAmount = add ? Math.min(current + amount, maxHearts) : Math.max(current - amount, 0);
        permanentHearts.put(uuid, newAmount);
        updateHearts(player, true);
    }

    // ---------------- Commands ----------------

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                reloadConfig();
                config = getConfig();
                sendPrefixedMessage(sender, config.getString("messages.reload-message"));
            }

            case "add" -> {
                if (args.length < 3) { sendPrefixedMessage(sender, config.getString("messages.usage-add")); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sendPrefixedMessage(sender, config.getString("messages.player-not-found")); return true; }
                try {
                    int amount = Integer.parseInt(args[2]);
                    changeHearts(target, amount, true);
                    sendPrefixedMessage(sender, config.getString("messages.add-heart-admin-message").replace("%player%", target.getName()).replace("%amount%", String.valueOf(amount)));
                    sendPrefixedMessage(target, config.getString("messages.add-heart-admin-message").replace("%player%", target.getName()).replace("%amount%", String.valueOf(amount)));
                } catch (NumberFormatException e) { sendPrefixedMessage(sender, config.getString("messages.invalid-amount")); }
            }

            case "remove" -> {
                if (args.length < 3) { sendPrefixedMessage(sender, config.getString("messages.usage-remove")); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sendPrefixedMessage(sender, config.getString("messages.player-not-found")); return true; }
                try {
                    int amount = Integer.parseInt(args[2]);
                    changeHearts(target, amount, false);
                    sendPrefixedMessage(sender, config.getString("messages.remove-heart-admin-message").replace("%player%", target.getName()).replace("%amount%", String.valueOf(amount)));
                    sendPrefixedMessage(target, config.getString("messages.remove-heart-admin-message").replace("%player%", target.getName()).replace("%amount%", String.valueOf(amount)));
                } catch (NumberFormatException e) { sendPrefixedMessage(sender, config.getString("messages.invalid-amount")); }
            }

            case "help" -> sendHelpMessage(sender);

            default -> sendHelpMessage(sender);
        }
        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sendMessage(sender, ChatColor.WHITE + " — ᴊᴜꜱᴛ" + ChatColor.RED + "ʜᴇᴀʀᴛꜱ " + ChatColor.WHITE + "Commands — ");
        sendMessage(sender, ChatColor.RED + "/hearts reload" + ChatColor.WHITE + " — Reloads the plugin config");
        sendMessage(sender, ChatColor.RED + "/hearts add <player> <amount>" + ChatColor.WHITE + " — Adds hearts to a player");
        sendMessage(sender, ChatColor.RED + "/hearts remove <player> <amount>" + ChatColor.WHITE + " — Removes hearts from a player");
        sendMessage(sender, ChatColor.RED + "/hearts help" + ChatColor.WHITE + " — Shows this help message");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return Arrays.asList("reload", "add", "remove", "help");

        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            String partial = args[1].toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase().startsWith(partial)) names.add(p.getName());
            return names;
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            return Arrays.asList("1", "2", "5", "10");
        }

        return Collections.emptyList();
    }
}
