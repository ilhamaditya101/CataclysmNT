package com.cataclysm.nt;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NametagManager {
    private final CataclysmNT plugin;
    private final LuckPerms luckPerms;
    private final Map<UUID, TextDisplay> displays = new HashMap<>();
    private final Map<UUID, String> teamNames = new HashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    public NametagManager(CataclysmNT plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
    }

    public void update(Player player) {
        if (!player.isOnline()) return;
        hideVanillaNametag(player);

        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        String prefix = value(user.getCachedData().getMetaData().getPrefix());
        String suffix = value(user.getCachedData().getMetaData().getSuffix());

        String format = plugin.getConfig().getString("nametag.format", "%prefix% %suffix%");
        String text = format.replace("%prefix%", prefix)
                .replace("%suffix%", suffix)
                .replace("%name%", "") // Player name is intentionally never rendered.
                .trim();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        boolean showEmpty = plugin.getConfig().getBoolean("nametag.show-empty", false);
        if (text.isBlank() && !showEmpty) {
            removeDisplay(player);
            return;
        }

        TextDisplay display = displays.get(player.getUniqueId());
        if (display == null || !display.isValid()) {
            removeDisplay(player);
            display = player.getWorld().spawn(player.getLocation(), TextDisplay.class, entity -> {
                entity.setPersistent(false);
                entity.setInvulnerable(true);
                entity.setGravity(false);
                entity.setSilent(true);
                entity.setBillboard(readBillboard());
                entity.setSeeThrough(plugin.getConfig().getBoolean("nametag.see-through", false));
                entity.setShadowed(plugin.getConfig().getBoolean("nametag.shadow", true));
                entity.setAlignment(readAlignment());
                entity.setTextOpacity((byte) 255);
                entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                entity.setDefaultBackground(false);
            });
            displays.put(player.getUniqueId(), display);
        }

        if (!display.getWorld().equals(player.getWorld())) display.teleport(player.getLocation());

        double height = plugin.getConfig().getDouble("nametag.height", 2.35);
        double animatedOffset = animationOffset(player);
        display.teleport(player.getLocation().add(0, height + animatedOffset, 0));

        display.text(parseText(text));
        applyDisplaySettings(display);
    }

    /** Called every tick for smooth animation and accurate following. */
    public void tick(Player player) {
        TextDisplay display = displays.get(player.getUniqueId());
        if (display == null || !display.isValid() || !player.isOnline()) return;

        double height = plugin.getConfig().getDouble("nametag.height", 2.35);
        display.teleport(player.getLocation().add(0, height + animationOffset(player), 0));
        display.setTextOpacity((byte) animationOpacity(player));
    }

    private void applyDisplaySettings(TextDisplay display) {
        display.setBillboard(readBillboard());
        display.setSeeThrough(plugin.getConfig().getBoolean("nametag.see-through", false));
        display.setShadowed(plugin.getConfig().getBoolean("nametag.shadow", true));
        display.setAlignment(readAlignment());
        display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        display.setDefaultBackground(false);

        double scale = plugin.getConfig().getDouble("nametag.scale", 0.9);
        Transformation transformation = display.getTransformation();
        transformation.getScale().set(new Vector3f((float) scale, (float) scale, (float) scale));
        display.setTransformation(transformation);

        float range = getViewRange();
        display.setViewRange(range);
    }

    private float getViewRange() {
        String mode = plugin.getConfig().getString("nametag.distance.mode", "AUTO");
        if ("FIXED".equalsIgnoreCase(mode)) {
            return (float) Math.max(1.0, plugin.getConfig().getDouble("nametag.distance.range", 48.0));
        }
        // AUTO follows the server's configured view distance, measured in blocks.
        double auto = Bukkit.getViewDistance() * 16.0;
        double min = plugin.getConfig().getDouble("nametag.distance.min-range", 16.0);
        double max = plugin.getConfig().getDouble("nametag.distance.max-range", 96.0);
        return (float) Math.max(min, Math.min(max, auto));
    }

    private double animationOffset(Player player) {
        String type = plugin.getConfig().getString("nametag.animation.type", "NONE");
        if ("BOB".equalsIgnoreCase(type)) {
            double amplitude = plugin.getConfig().getDouble("nametag.animation.amplitude", 0.05);
            double speed = plugin.getConfig().getDouble("nametag.animation.speed", 0.08);
            return Math.sin(((System.nanoTime() / 50_000_000.0) * speed) + player.getEntityId()) * amplitude;
        }
        return 0.0;
    }

    private int animationOpacity(Player player) {
        int base = clamp(plugin.getConfig().getInt("nametag.opacity", 255), 0, 255);
        String type = plugin.getConfig().getString("nametag.animation.type", "NONE");
        if (!"PULSE".equalsIgnoreCase(type)) return base;

        double amount = Math.max(0, Math.min(255, plugin.getConfig().getDouble("nametag.animation.opacity-amount", 45)));
        double speed = plugin.getConfig().getDouble("nametag.animation.speed", 0.08);
        double pulse = (Math.sin(((System.nanoTime() / 50_000_000.0) * speed) + player.getEntityId()) + 1.0) / 2.0;
        return clamp((int) Math.round(base - (amount * pulse)), 0, 255);
    }

    private void hideVanillaNametag(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = teamNames.computeIfAbsent(player.getUniqueId(), id ->
                "cnt" + id.toString().replace("-", "").substring(0, 13));
        Team team = scoreboard.getTeam(teamName);
        if (team == null) team = scoreboard.registerNewTeam(teamName);
        if (!team.hasEntry(player.getName())) team.addEntry(player.getName());
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS);
    }

    private Component parseText(String input) {
        if (input.contains("<#") || input.contains("<color:") || input.contains("<gradient:")) {
            try { return miniMessage.deserialize(input); } catch (Exception ignored) { }
        }
        return legacy.deserialize(HexColorParser.convert(input).replace('&', '§'));
    }

    private Display.Billboard readBillboard() {
        try { return Display.Billboard.valueOf(plugin.getConfig().getString("nametag.billboard", "CENTER").toUpperCase()); }
        catch (Exception e) { return Display.Billboard.CENTER; }
    }

    private TextDisplay.TextAlignment readAlignment() {
        try { return TextDisplay.TextAlignment.valueOf(plugin.getConfig().getString("nametag.alignment", "CENTER").toUpperCase()); }
        catch (Exception e) { return TextDisplay.TextAlignment.CENTER; }
    }

    public void scheduleUpdate(Player player) { Bukkit.getScheduler().runTaskLater(plugin, () -> update(player), 2L); }
    public void remove(Player player) {
        removeDisplay(player);
        String teamName = teamNames.remove(player.getUniqueId());
        if (teamName != null) {
            Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamName);
            if (team != null) { team.removeEntry(player.getName()); if (team.getEntries().isEmpty()) team.unregister(); }
        }
    }
    private void removeDisplay(Player player) { TextDisplay d = displays.remove(player.getUniqueId()); if (d != null && d.isValid()) d.remove(); }
    public void removeAll() { displays.values().forEach(d -> { if (d != null && d.isValid()) d.remove(); }); displays.clear(); }
    private static String value(String s) { return s == null ? "" : s; }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
