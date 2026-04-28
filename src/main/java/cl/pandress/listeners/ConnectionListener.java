package cl.pandress.listeners;

import cl.pandress.DiscordLogsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConnectionListener implements Listener {

    private final DiscordLogsPlugin plugin;

    public ConnectionListener(DiscordLogsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        processConnection("webhook-logs.join", event.getPlayer().getName(), 0);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        processConnection("webhook-logs.quit", event.getPlayer().getName(), -1);
    }

    private void processConnection(String path, String playerName, int offset) {
        if (!plugin.getConfig().getBoolean(path + ".Enable")) return;

        String webhookUrl = plugin.getConfig().getString(path + ".webhook-url");
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK_HERE")) return;

        int onlineCount = plugin.getServer().getOnlinePlayers().size() + offset;
        int maxPlayers = plugin.getServer().getMaxPlayers();
        final String onlineStr = onlineCount + "/" + maxPlayers;

        String title = plugin.getConfig().getString(path + ".title");
        int color = plugin.getConfig().getInt(path + ".color");

        List<Map<?, ?>> rawFields = plugin.getConfig().getMapList(path + ".fields");
        List<String[]> fieldList = new ArrayList<>();

        for (Map<?, ?> map : rawFields) {
            String name = String.valueOf(map.get("name")).replace("{player}", playerName);
            String value = String.valueOf(map.get("value"))
                    .replace("{player}", playerName)
                    .replace("{online}/{max}", onlineStr);
            String inline = String.valueOf(map.get("inline"));
            
            fieldList.add(new String[]{name, value, inline});
        }

        final String[][] finalFields = fieldList.toArray(new String[0][0]);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DiscordWebhook.sendEmbed(webhookUrl, title, "", color, finalFields);
            } catch (Exception e) {
                plugin.getLogger().warning("Error enviando log de conexión: " + e.getMessage());
            }
        });
    }
}