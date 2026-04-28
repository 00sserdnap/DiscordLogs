package cl.pandress;

import cl.pandress.listeners.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DiscordLogsPlugin extends JavaPlugin {

    private JDA jdaBot;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandListener(this), this);
        getServer().getPluginManager().registerEvents(new EconomyListener(this), this);
        getServer().getPluginManager().registerEvents(new SecurityListener(this), this);

        // Verificación de ShopGUI+
        if (getServer().getPluginManager().getPlugin("ShopGUIPlus") != null) {
            getServer().getPluginManager().registerEvents(new ShopGUIPlusListener(this), this);
            getLogger().info("Módulo de ShopGUIPlus cargado.");
        }

        // Notificar inicio del servidor en Discord
        getServer().getScheduler().runTaskAsynchronously(this, () -> sendServerStatus("webhook-logs.server-start"));

        getLogger().info("DiscordLogs v1.0 ha sido activado correctamente.");
    }

    @Override
    public void onDisable() {
        // Notificar apagado del servidor (Síncrono para asegurar envío)
        sendServerStatus("webhook-logs.server-stop");

        // Apagar el bot de Discord correctamente
        if (jdaBot != null) {
            jdaBot.shutdownNow();
            getLogger().info("Bot de Discord desconectado.");
        }

        getLogger().info("DiscordLogs desactivado.");
    }

    /**
     * Método para enviar estados del servidor (Start/Stop) vía Webhook
     */
    private void sendServerStatus(String path) {
        if (!getConfig().getBoolean(path + ".Enable")) return;

        String webhookUrl = getConfig().getString(path + ".webhook-url");
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK")) return;

        String title = getConfig().getString(path + ".title", "Server Status");
        int color = getConfig().getInt(path + ".color", 16777215);

        List<Map<?, ?>> rawFields = getConfig().getMapList(path + ".fields");
        List<String[]> fieldList = new ArrayList<>();

        for (Map<?, ?> map : rawFields) {
            String name = String.valueOf(map.get("name"));
            String value = String.valueOf(map.get("value"));
            String inline = String.valueOf(map.get("inline"));
            fieldList.add(new String[]{name, value, inline});
        }

        final String[][] finalFields = fieldList.toArray(new String[0][0]);

        try {
            DiscordWebhook.sendEmbed(webhookUrl, title, "", color, finalFields);
        } catch (Exception e) {
            getLogger().warning("Error al enviar status al Webhook: " + e.getMessage());
        }
    }
}