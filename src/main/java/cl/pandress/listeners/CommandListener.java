package cl.pandress.listeners;

import cl.pandress.DiscordLogsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommandListener implements Listener {

    private final DiscordLogsPlugin plugin;

    public CommandListener(DiscordLogsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        final String configPath = "webhook-logs.commands";

        if (!plugin.getConfig().getBoolean(configPath + ".Enable")) {
            return;
        }

        final String webhookUrl = plugin.getConfig().getString(configPath + ".webhook-url");
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("TU_WEBHOOK_AQUI")) {
            return;
        }

        // 1. Obtener Datos
        final String playerName = event.getPlayer().getName();
        final String commandExecuted = event.getMessage(); 

        // 2. Leer Configuración Visual
        final String title = plugin.getConfig().getString(configPath + ".title", "Comando Ejecutado");
        final int colorCode = plugin.getConfig().getInt(configPath + ".color", 3447003);

        // 3. Procesar las Columnas
        List<Map<?, ?>> rawFields = plugin.getConfig().getMapList(configPath + ".fields");
        List<String[]> fieldList = new ArrayList<>();
        
        for (Map<?, ?> map : rawFields) {
            if (map.containsKey("name") && map.containsKey("value") && map.containsKey("inline")) {
                String name = String.valueOf(map.get("name"));
                String value = String.valueOf(map.get("value"))
                        .replace("{player}", playerName)
                        .replace("{command}", commandExecuted);
                String inline = String.valueOf(map.get("inline"));
                
                fieldList.add(new String[]{name, value, inline});
            }
        }
        
        final String[][] finalFields = fieldList.toArray(new String[0][0]);

        // 4. Enviar a Discord
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Dejamos la description vacía ("") porque en comandos no suele haber mensaje adicional
                DiscordWebhook.sendEmbed(webhookUrl, title, "", colorCode, finalFields);
            } catch (Exception e) {
                plugin.getLogger().severe("Error al enviar log de comando: " + e.getMessage());
            }
        });
    }
}