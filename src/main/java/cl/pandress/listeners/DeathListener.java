package cl.pandress.listeners;

import cl.pandress.DiscordLogsPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeathListener implements Listener {

    private final DiscordLogsPlugin plugin;

    public DeathListener(DiscordLogsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        final boolean isKill = (killer != null);
        final String configPath = isKill ? "webhook-logs.Kills" : "webhook-logs.death";

        if (!plugin.getConfig().getBoolean(configPath + ".Enable")) {
            return;
        }

        final String webhookUrl = plugin.getConfig().getString(configPath + ".webhook-url");
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("TU_WEBHOOK_AQUI")) {
            return;
        }

        final String victimName = victim.getName();
        final String killerName = isKill ? killer.getName() : "Entorno/Mob";
        final String cause = (victim.getLastDamageCause() != null) ? victim.getLastDamageCause().getCause().name() : "DESCONOCIDA";
        final String deathMessage = event.getDeathMessage() != null ? event.getDeathMessage() : "";

        Location loc = victim.getLocation();
        final String locationStr = String.format("Mundo: %s | X: %d, Y: %d, Z: %d", loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        final String title = plugin.getConfig().getString(configPath + ".title", "Muerte");
        final int colorCode = plugin.getConfig().getInt(configPath + ".color", 16753920);

        List<Map<?, ?>> rawFields = plugin.getConfig().getMapList(configPath + ".fields");
        List<String[]> fieldList = new ArrayList<>();

        for (Map<?, ?> map : rawFields) {
            if (map.containsKey("name") && map.containsKey("value") && map.containsKey("inline")) {
                String name = String.valueOf(map.get("name"));
                String value = String.valueOf(map.get("value"))
                        .replace("{victim}", victimName)
                        .replace("{killer}", killerName)
                        .replace("{cause}", cause)
                        .replace("{location}", locationStr);
                String inline = String.valueOf(map.get("inline"));

                fieldList.add(new String[]{name, value, inline});
            }
        }

        final String[][] finalFields = fieldList.toArray(new String[0][0]);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DiscordWebhook.sendEmbed(webhookUrl, title, deathMessage, colorCode, finalFields);
            } catch (Exception e) {
                plugin.getLogger().severe("Error al enviar log de muerte: " + e.getMessage());
            }
        });
    }
}