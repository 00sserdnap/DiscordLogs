package cl.pandress.listeners;

import cl.pandress.DiscordLogsPlugin;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EconomyListener implements Listener {

    private final DiscordLogsPlugin plugin;

    public EconomyListener(DiscordLogsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEconomyCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase();
        String[] args = message.split(" ");
        String command = args[0];

        // --- LÓGICA PARA /PAY ---
        if (command.equals("/pay")) {
            handlePayCommand(event, args);
        } 
        // --- LÓGICA PARA /SELLALL ---
        else if (command.equals("/sellall")) {
            handleSellAllCommand(event);
        }
    }

    private void handlePayCommand(PlayerCommandPreprocessEvent event, String[] args) {
        final String path = "webhook-logs.economy";
        if (!plugin.getConfig().getBoolean(path + ".Enable")) return;

        if (args.length < 3) return; // /pay <jugador> <monto>

        String sender = event.getPlayer().getName();
        String receiver = args[1];
        String amount = args[2];
        String webhookUrl = plugin.getConfig().getString(path + ".webhook-url");

        processAndSend(path, webhookUrl, (line) -> line
                .replace("{sender}", sender)
                .replace("{receiver}", receiver)
                .replace("{amount}", amount));
    }

    private void handleSellAllCommand(PlayerCommandPreprocessEvent event) {
        final String path = "webhook-logs.sell";
        if (!plugin.getConfig().getBoolean(path + ".Enable")) return;

        String player = event.getPlayer().getName();
        Location loc = event.getPlayer().getLocation();
        String locStr = String.format("%s | %d, %d, %d", loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        String webhookUrl = plugin.getConfig().getString(path + ".webhook-url");

        processAndSend(path, webhookUrl, (line) -> line
                .replace("{player}", player)
                .replace("{location}", locStr));
    }

    private void processAndSend(String path, String url, java.util.function.Function<String, String> replacer) {
        if (url == null || url.isEmpty() || url.contains("TU_WEBHOOK_AQUI")) return;

        String title = plugin.getConfig().getString(path + ".title");
        int color = plugin.getConfig().getInt(path + ".color");
        List<Map<?, ?>> rawFields = plugin.getConfig().getMapList(path + ".fields");
        List<String[]> fieldList = new ArrayList<>();

        for (Map<?, ?> map : rawFields) {
            String name = String.valueOf(map.get("name"));
            String value = replacer.apply(String.valueOf(map.get("value")));
            String inline = String.valueOf(map.get("inline"));
            fieldList.add(new String[]{name, value, inline});
        }

        final String[][] finalFields = fieldList.toArray(new String[0][0]);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DiscordWebhook.sendEmbed(url, title, "", color, finalFields);
            } catch (Exception e) {
                plugin.getLogger().warning("Error enviando log de economía: " + e.getMessage());
            }
        });
    }
}