package bot;

import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

import bot.commands.*;
import bot.keyboards.Keyboards;
import utils.Utils;

public final class TelegramBot extends TelegramLongPollingCommandBot {
    private final String BOT_NAME;
    private final String BOT_TOKEN;

    StartCommand startCommand = new StartCommand("start", "Старт");
    ChooseCityCommand chooseCityCommand = new ChooseCityCommand("city", "Город");
    ChooseShopsCommand chooseShopsCommand = new ChooseShopsCommand("shops", "Выбрать магазины");
    FindItemCommand findItemCommand = new FindItemCommand("finditem", "Найти товар");
    ShowItemsCommand showItemsCommand = new ShowItemsCommand("showitems", "Отобразить товары");

    //Класс для обработки сообщений, не являющихся командой
    private final NonCommand nonCommand;

    /**
     * Настройки для разных пользователей. Ключ - уникальный id чата, значение - имя пользователя
     */
    private static Map<Long, Command> userCommand;
    private static Map<Long, ArrayList<Integer>> userNumbers;

    public TelegramBot(String botName, String botToken) {
        super();
        this.BOT_NAME = botName;
        this.BOT_TOKEN = botToken;
        this.nonCommand = new NonCommand();
        userCommand = new HashMap<>();
        userNumbers = new HashMap<>();
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }

    /**
     * Ответ на запрос, не являющийся командой
     */
    @Override
    public void processNonCommandUpdate(Update update) {
        Message msg = update.getMessage();
        Long chatId = msg.getChatId();
        String userName = Utils.getUserName(msg);

        if (msg.getText() == null || msg.getText().isEmpty()) {
            return;
        }
        switch (msg.getText().trim()) {
            case "/start":
                userCommand.put(chatId, Command.START);
                userNumbers.put(chatId, new ArrayList<>());
                startCommand.execute(this, update.getMessage().getFrom(), update.getMessage().getChat(), null);
                break;
            case "/city":
            case "Выбрать город":
                userCommand.put(chatId, Command.CITY);
                chooseCityCommand.execute(this, update.getMessage().getFrom(), update.getMessage().getChat(), null);
                break;
            case "/shops":
            case "Выбрать магазины":
                userCommand.put(chatId, Command.CHOSE_SHOPS);
                chooseShopsCommand.execute(this, update.getMessage().getFrom(), update.getMessage().getChat(), null);
                break;
            case "/finditem":
            case "Найти товар":
                userCommand.put(chatId, Command.FIND_ITEM);
                findItemCommand.execute(this, update.getMessage().getFrom(), update.getMessage().getChat(), null);
                break;
            case "/showitems":
            case "Отобразить товары":
                userCommand.put(chatId, Command.SHOW_ITEMS);
                showItemsCommand.execute(this, update.getMessage().getFrom(), update.getMessage().getChat(), null);
                break;
            default: //получили текст
                if (userCommand.get(chatId) == null) {
                    break;
                }
                if (userCommand.get(chatId).equals(Command.CITY)) { // получили город
                    chooseCityCommand.setMessage(msg.getText());
                    // Если город успешно записан в БД - текущая команда пользователя обнуляется
                    if (chooseCityCommand.executePart2(this, update.getMessage().getFrom(), update.getMessage().getChat(), null)) {
                        userCommand.put(chatId, null);
                    }
                } else if (userCommand.get(chatId).equals(Command.FIND_ITEM)) { // получили название товара
                    findItemCommand.setMessage(msg.getText());
                    findItemCommand.executePart2(this, update.getMessage().getFrom(), update.getMessage().getChat(), null);
                    userCommand.put(chatId, null);
                } else if (userCommand.get(chatId).equals(Command.CHOSE_SHOPS)) { // получили результат нажатия кнопки клавиатуры
                    if (isNumeric(msg.getText())) {
                        userNumbers.get(chatId).add(Integer.valueOf(msg.getText()));
                    } else if (msg.getText().equals(",")) {
                        break;
                    } else if (msg.getText().equals("Стереть") && userNumbers.get(chatId).size() > 0) {
                        userNumbers.get(chatId).remove(userNumbers.get(chatId).size() - 1);
                    } else if (msg.getText().contains(",")) {
                        NonCommand nonComand = new NonCommand();
                        if (nonComand.checkValid(msg.getText())) {
                            int[] array = nonComand.getNumbers(msg.getText());
                            for (int elem : array) {
                                userNumbers.get(chatId).add(elem);
                            }
                        } else {
                            SendMessage sendMessage = new SendMessage();
                            sendMessage.setChatId(String.valueOf(chatId));
                            sendMessage.setText("Пожалуйста, введите номера торговых сетей через запятую" +
                                    " или воспользуйтесь клавиатурой для ввода чисел");
                            try {
                                execute(sendMessage);
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (msg.getText().equals("Далее")) {
                        chooseShopsCommand.setNumbers(userNumbers.get(chatId));
                        userNumbers.put(chatId, new ArrayList<>()); // передали числа команде и очищаем мапу для пользователя
                        if (chooseShopsCommand.execute2(this, update.getMessage().getFrom(), update.getMessage().getChat(), null)) {
                            userCommand.put(chatId, null);
                        }
                    } else if (msg.getText().equals("Назад")) {
                        userNumbers.put(chatId, new ArrayList<>()); // передали числа команде и очищаем мапу для пользователя
                        userCommand.put(chatId, null);
                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(String.valueOf(chatId));
                        sendMessage.setText("Выберите интересующие магазины");
                        Keyboards keyboards = new Keyboards();
                        List<String> commands = new ArrayList<>();
                        commands.add("Выбрать магазины");
                        keyboards.setButtonToCallCommand(sendMessage, commands);
                        try {
                            execute(sendMessage);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (userCommand.get(chatId).equals(Command.SHOW_ITEMS)) { // получили список номеров категорий (он запрашивается в команде showItems)
                    if (isNumeric(msg.getText())) {
                        userNumbers.get(chatId).add(Integer.valueOf(msg.getText()));
                    } else if (msg.getText().equals(",")) {
                        break;
                    } else if (msg.getText().contains(",")) {
                        NonCommand nonComand = new NonCommand();
                        if (nonComand.checkValid(msg.getText())) {
                            int[] array = nonComand.getNumbers(msg.getText());
                            for (int elem : array) {
                                userNumbers.get(chatId).add(elem);
                            }
                        } else {
                            SendMessage sendMessage = new SendMessage();
                            sendMessage.setChatId(String.valueOf(chatId));
                            sendMessage.setText("Пожалуйста, введите номера категорий товаров через запятую " +
                                    "или воспользуйтесь клавиатурой для ввода чисел");
                            try {
                                execute(sendMessage);
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (msg.getText().equals("Стереть") && userNumbers.get(chatId).size() > 0) {
                        userNumbers.get(chatId).remove(userNumbers.get(chatId).size() - 1);
                    } else if (msg.getText().equals("Далее")) {
                        showItemsCommand.setNumbers(userNumbers.get(chatId));
                        userNumbers.put(chatId, new ArrayList<>()); // передали числа команде и очищаем мапу для пользователя
                        if (showItemsCommand.execute2(this, update.getMessage().getFrom(), update.getMessage().getChat(), null)) {
                            userCommand.put(chatId, null);
                        }
                    } else if (msg.getText().equals("Назад")) {
                        userNumbers.put(chatId, new ArrayList<>()); // передали числа команде и очищаем мапу для пользователя
                        userCommand.put(chatId, null);
                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(String.valueOf(chatId));
                        sendMessage.setText("Хотите выполнить повторный поиск?\n" +
                                "Найти товар - поиск акционных товаров по названию" +
                                "Отобразить товары - поиск акционных товаров по категориям\n" +
                                "/shops - вернуться к выбору магазинов");
                        Keyboards keyboards = new Keyboards();
                        List<String> commands = new ArrayList<>();
                        commands.add("Найти товар");
                        commands.add("Отобразить товары");
                        keyboards.setButtonToCallCommand(sendMessage, commands);
                        try {
                            execute(sendMessage);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                }
        }
    }

    private boolean isNumeric(String s) {
        char[] chars = s.toCharArray();
        for (char c : chars) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    public static Map<Long, Command> getUserCommand() {
        return userCommand;
    }
}
