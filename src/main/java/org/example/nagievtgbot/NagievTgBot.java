package org.example.nagievtgbot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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

            if (senderUsername != null && senderUsername.equals(getBotUsername())) {
                return;
            }

            if (text.toLowerCase().startsWith("нагиев шанс ")) {
                try {
                    String chanceStr = text.substring("нагиев шанс ".toLowerCase().length()).trim();
                    int chancePercent = Integer.parseInt(chanceStr);
                    if (chancePercent < 0 || chancePercent > 100) {
                        throw new NumberFormatException("Шанс должен быть от 0 до 100");
                    }
                    replyChance = chancePercent / 100.0;
                    sendResponse(chatId, "Шанс ответа установлен на " + chancePercent + "%");
                } catch (NumberFormatException e) {
                    sendResponse(chatId, "Ошибка: укажите число от 0 до 100, например 'нагиев шанс 67'");
                }
                return;  // Выходим, чтобы не обрабатывать как обычное сообщение
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
}