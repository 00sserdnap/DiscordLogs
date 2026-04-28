package cl.pandress.listeners;

import cl.pandress.DiscordLogsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SecurityListener implements Listener {

    private final DiscordLogsPlugin plugin;

    public SecurityListener(DiscordLogsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        handleCommand(event.getPlayer().getName(), event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsoleCommand(ServerCommandEvent event) {
        handleCommand("Consola", "/" + event.getCommand());
    }

    private void handleCommand(String sender, String fullCommand) {
        String baseCommand = fullCommand.split(" ")[0].toLowerCase();
        
        if (baseCommand.equals("/op") || baseCommand.equals("/deop") || baseCommand.equals("/stop")
                || baseCommand.equals("/reload") || baseCommand.equals("/rl")) {
            
            final String path = "webhook-logs.security";
            if (!plugin.getConfig().getBoolean(path + ".Enable")) return;

            String webhookUrl = plugin.getConfig().getString(path + ".webhook-url");
            if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK_HERE")) return;

            String title = plugin.getConfig().getString(path + ".title", "Comando Peligroso");
            int color = plugin.getConfig().getInt(path + ".color", 16711680);

            List<Map<?, ?>> rawFields = plugin.getConfig().getMapList(path + ".fields");
            List<String[]> fieldList = new ArrayList<>();

            for (Map<?, ?> map : rawFields) {
                String name = String.valueOf(map.get("name"));
                String value = String.valueOf(map.get("value"))
                        .replace("{player}", sender)
                        .replace("{command}", fullCommand);
                String inline = String.valueOf(map.get("inline"));
                fieldList.add(new String[]{name, value, inline});
            }

            final String[][] finalFields = fieldList.toArray(new String[0][0]);

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    DiscordWebhook.sendEmbed(webhookUrl, title, "", color, finalFields);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error enviando log de seguridad: " + e.getMessage());
                }
            });
        }
    }
}