package org.example.nagievtgbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;

public class JsonToDbParser {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/nagievdb";  // Из вашего Docker
    private static final String DB_USER = "user";
    private static final String DB_PASSWORD = "password";

    public static void main(String[] args) {
        String jsonFilePath = "D:/tgcusinbd/result.json";  // Укажите путь

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(new File(jsonFilePath));

            // Гибкое извлечение messages
            JsonNode messagesNode;
            if (root.isArray()) {
                messagesNode = root;  // Root — массив сообщений
            } else if (root.has("messages")) {
                messagesNode = root.path("messages");  // Прямой "messages"
            } else if (root.has("chats") && root.path("chats").isArray() && root.path("chats").size() > 0) {
                messagesNode = root.path("chats").get(0).path("messages");  // Стандартный экспорт
            } else {
                System.out.println("Не найдена структура с сообщениями (chats или messages).");
                return;
            }

            if (!messagesNode.isArray()) {
                System.out.println("Messages не является массивом.");
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "INSERT INTO messages (id, content) VALUES (?, ?) ON CONFLICT (id) DO NOTHING";
                PreparedStatement pstmt = conn.prepareStatement(sql);

                Iterator<JsonNode> messages = messagesNode.elements();
                int count = 0;
                while (messages.hasNext()) {
                    JsonNode msg = messages.next();
                    if ("message".equals(msg.path("type").asText())) {  // Только type: "message"
                        long id = msg.path("id").asLong();
                        String text = extractText(msg);

                        if (text != null && !text.isEmpty()) {
                            pstmt.setLong(1, id);
                            pstmt.setString(2, text);
                            pstmt.executeUpdate();
                            count++;
                        }
                    }
                }
                System.out.println("Вставлено " + count + " сообщений (дубликаты пропущены).");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Извлечение текста: предпочтительно из text_entities; иначе из text
    private static String extractText(JsonNode msg) {
        JsonNode entities = msg.path("text_entities");
        if (entities.isArray()) {
            StringBuilder sb = new StringBuilder();
            Iterator<JsonNode> parts = entities.elements();
            while (parts.hasNext()) {
                JsonNode part = parts.next();
                if (part.has("text")) {
                    sb.append(part.path("text").asText());
                }
            }
            if (sb.length() > 0) {
                return sb.toString();
            }
        }

        JsonNode textNode = msg.path("text");
        if (textNode.isTextual()) {
            return textNode.asText();
        } else if (textNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            Iterator<JsonNode> parts = textNode.elements();
            while (parts.hasNext()) {
                JsonNode part = parts.next();
                if (part.has("text")) {
                    sb.append(part.path("text").asText());
                }
            }
            return sb.toString();
        }
        return null;
    }
}