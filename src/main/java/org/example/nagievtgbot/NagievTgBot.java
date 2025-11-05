package org.example.nagievtgbot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Random;

@Component  // Делаем бота Spring-компонентом
public class NagievTgBot extends TelegramLongPollingBot {

    @Autowired
    private NagievRepository nagievRepository;

    private final Random random = new Random();
    private double replyChance = 1.0;

    @Override
    public String getBotUsername() {
        return "NagievTgbot";
    }

    @Override
    public String getBotToken() {
        return "8265611439:AAGYWVAIYQD11OWFhJOracgtlsdjtYqdWDU";
    }

    private void sendResponse(long chatId, String responseText) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(responseText);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            String senderUsername = update.getMessage().getFrom().getUserName();
            Integer messageId = update.getMessage().getMessageId(); // ID сообщения пользователя

            if (senderUsername != null && senderUsername.equals(getBotUsername())) {
                return;
            }

            // === ОБРАБОТКА КОМАНДЫ "нагиев шанс X" ===
            if (text.toLowerCase().startsWith("нагиев шанс ")) {
                try {
                    String chanceStr = text.substring("нагиев шанс ".length()).trim();
                    int chancePercent = Integer.parseInt(chanceStr);
                    if (chancePercent < 0 || chancePercent > 100) {
                        throw new NumberFormatException("Шанс должен быть от 0 до 100");
                    }

                    replyChance = chancePercent / 100.0;

                    // Отправляем ответ
                    SendMessage response = new SendMessage();
                    response.setChatId(String.valueOf(chatId));
                    response.setText("Шанс ответа установлен на " + chancePercent + "%");
                    Message sentMessage = execute(response); // Получаем объект отправленного сообщения

                    // Удаляем сообщение пользователя
                    deleteMessage(chatId, messageId);

                    // Удаляем своё сообщение через 3 секунды
                    deleteMessageLater(chatId, sentMessage.getMessageId(), 3000);

                } catch (NumberFormatException e) {
                    sendResponse(chatId, "Ошибка: укажите число от 0 до 100, например 'нагиев шанс 67'");
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                return;
            }

            Messages entity = new Messages();
            entity.setContent(text);  // Или setContent
            nagievRepository.save(entity);

            if (random.nextDouble() < replyChance && nagievRepository.count() > 0) {
                String randomMsg = nagievRepository.findRandomMessage().getContent();
                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(chatId));
                message.setText(randomMsg);
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    // Удаление сообщения по chatId и messageId
    private void deleteMessage(long chatId, int messageId) {
        try {
            DeleteMessage delete = new DeleteMessage();
            delete.setChatId(String.valueOf(chatId));
            delete.setMessageId(messageId);
            execute(delete);
        } catch (TelegramApiException e) {
            // Игнорируем, если не удалось удалить (например, сообщение уже удалено)
        }
    }

    // Удаление сообщения с задержкой
    private void deleteMessageLater(long chatId, int messageId, long delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                deleteMessage(chatId, messageId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}