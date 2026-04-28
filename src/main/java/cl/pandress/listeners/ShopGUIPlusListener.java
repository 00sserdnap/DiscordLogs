package cl.pandress.listeners;

import cl.pandress.DiscordLogsPlugin;
import net.brcdev.shopgui.event.ShopPostTransactionEvent;
import net.brcdev.shopgui.shop.ShopManager.ShopAction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopGUIPlusListener implements Listener {

    private final DiscordLogsPlugin plugin;

    public ShopGUIPlusListener(DiscordLogsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onShopTransaction(ShopPostTransactionEvent event) {
        if (event.getResult().getResult() != net.brcdev.shopgui.shop.ShopTransactionResult.ShopTransactionResultType.SUCCESS) {
            return;
        }

        ShopAction action = event.getResult().getShopAction();
        String path = (action == ShopAction.BUY) ? "webhook-logs.shop-buy" : "webhook-logs.shop-sell";

        if (!plugin.getConfig().getBoolean(path + ".Enable")) {
            return;
        }
        
        String webhookUrl = plugin.getConfig().getString(path + ".webhook-url");
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("TU_WEBHOOK_AQUI")) {
            return;
        }

        String player = event.getResult().getPlayer().getName(); 
        String item = event.getResult().getShopItem().getItem().getType().name(); 
        String amount = String.valueOf(event.getResult().getAmount());
        
        String price = DiscordWebhook.formatMoney(event.getResult().getPrice());

        String title = plugin.getConfig().getString(path + ".title", "Transacción");
        int color = plugin.getConfig().getInt(path + ".color", 15105570);

        List<Map<?, ?>> rawFields = plugin.getConfig().getMapList(path + ".fields");
        List<String[]> fieldList = new ArrayList<>();

        for (Map<?, ?> map : rawFields) {
            String name = String.valueOf(map.get("name"));
            String value = String.valueOf(map.get("value"))
                    .replace("{player}", player)
                    .replace("{item}", item)
                    .replace("{amount}", amount)
                    .replace("{price}", price);
            String inline = String.valueOf(map.get("inline"));
            
            fieldList.add(new String[]{name, value, inline});
        }

        final String[][] finalFields = fieldList.toArray(new String[0][0]);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DiscordWebhook.sendEmbed(webhookUrl, title, "", color, finalFields);
            } catch (Exception e) {
                plugin.getLogger().warning("Error enviando log de ShopGUI+: " + e.getMessage());
            }
        });
    }
}