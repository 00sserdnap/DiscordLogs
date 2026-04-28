package cl.pandress.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class DiscordWebhook {

    public static void sendEmbed(String webhookUrl, String title, String description, int color, String[][] fields) throws Exception {
        JsonObject json = new JsonObject();
        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();
        embed.addProperty("title", title);
        if (description != null && !description.isEmpty() && !description.equals("null")) {
            embed.addProperty("description", "**" + description + "**");
        }
        embed.addProperty("color", color);
        if (fields != null && fields.length > 0) {
            JsonArray jsonFields = new JsonArray();
            for (String[] fieldData : fields) {
                JsonObject field = new JsonObject();
                field.addProperty("name", fieldData[0]);
                field.addProperty("value", fieldData[1]);
                field.addProperty("inline", Boolean.parseBoolean(fieldData[2]));
                jsonFields.add(field);
            }
            embed.add("fields", jsonFields);
        }
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "Server Logs • Sistema de Seguridad");
        embed.add("footer", footer);
        embeds.add(embed);
        json.add("embeds", embeds);
        sendPostRequest(webhookUrl, json);
    }

    public static void sendMasqueradeMessage(String webhookUrl, String content, String username, String avatarUrl) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("content", content);
        if (username != null) json.addProperty("username", username);
        if (avatarUrl != null) json.addProperty("avatar_url", avatarUrl);
        sendPostRequest(webhookUrl, json);
    }

    private static void sendPostRequest(String webhookUrl, JsonObject json) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.addRequestProperty("Content-Type", "application/json");
        connection.addRequestProperty("User-Agent", "MinecraftServer-DiscordLogs");
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        try (OutputStream stream = connection.getOutputStream()) {
            stream.write(json.toString().getBytes(StandardCharsets.UTF_8));
            stream.flush();
        }
        connection.getInputStream().close();
        connection.disconnect();
    }

    public static String formatMoney(double amount) {
        DecimalFormat df = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
        if (amount < 1_000) return df.format(amount);
        if (amount < 1_000_000) return df.format(amount / 1_000.0) + "k";
        if (amount < 1_000_000_000) return df.format(amount / 1_000_000.0) + "M";
        return df.format(amount / 1_000_000_000.0) + "B";
    }
}