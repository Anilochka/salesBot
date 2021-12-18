package bot;

import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;

import bot.commands.*;
import database.City;
import database.JDBCConnector;

public final class TelegramBot extends TelegramLongPollingCommandBot {
    private final String BOT_NAME;
    private final String BOT_TOKEN;

    StartCommand startCommand= new StartCommand("start", "Старт");
    ChooseCityCommand chooseCityCommand = new ChooseCityCommand("city", "Город");
    ChooseShopsCommand chooseShopsCommand = new ChooseShopsCommand("shops", "Выбрать магазины");
    FindItemCommand findItemCommand = new FindItemCommand("finditem", "Найти товар");
    ShowItemsCommand showItemsCommand = new ShowItemsCommand("showitems", "Отобразить товары");

    //Класс для обработки сообщений, не являющихся командой
    private final NonCommand nonCommand;

    private String lastMessage;
    private Command lastCommand;

    /**
     * Настройки для разных пользователей. Ключ - уникальный id чата, значение - имя пользователя
     */
    private static Map<Long, String> userSettings;

    public TelegramBot(String botName, String botToken) {
        super();
        this.BOT_NAME = botName;
        this.BOT_TOKEN = botToken;
        //создаём вспомогательный класс для работы с сообщениями, не являющимися командами
        this.nonCommand = new NonCommand();
        //регистрируем команды
//        register(new StartCommand("start", "Старт"));
//        register(new ChooseCityCommand("city", "Город"));
//        register(new ChooseShopsCommand("shops", "Выбрать магазины"));
//        register(new FindItemCommand("finditem", "Найти товар"));
//        register(new ShowItemsCommand("showitems", "Отобразить товары"));
        userSettings = new HashMap<>();
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
        String userName = getUserName(msg);

        switch (msg.getText()) {
            case "/start":
                lastCommand = Command.START;
                startCommand.execute(this, update.getMessage().getFrom(), update.getMessage().getChat(), null);
                break;
            case "/city":
                lastCommand = Command.CITY;
                chooseCityCommand.execute(this, update.getMessage().getFrom(), update.getMessage().getChat(), null);
                break;
            case "/shops":
                lastCommand = Command.CHOSE_SHOPS;
                chooseShopsCommand.execute(this, update.getMessage().getFrom(), update.getMessage().getChat(), null);
                break;
            case "/finditem":
                lastCommand = Command.FIND_ITEM;
                findItemCommand.execute(this, update.getMessage().getFrom(), update.getMessage().getChat(), null);
                break;
            case "/showitems":
                lastCommand = Command.SHOW_ITEMS;
                showItemsCommand.execute(this, update.getMessage().getFrom(), update.getMessage().getChat(), null);
                break;
            default: //получили текст
                if (lastCommand == null) {
                    break; // + какая-то логика
                }
                if (lastCommand == Command.CITY) { // получили город
                    chooseCityCommand.setMessage(msg.getText());
                } else if (lastCommand == Command.FIND_ITEM) { // получили название товара
                    findItemCommand.setMessage(msg.getText());
                } else if (lastCommand == Command.CHOSE_SHOPS) { // получили список номеров магазинов
                    //
                } else if (lastCommand == Command.SHOW_ITEMS) { // получили список номеров категорий (он запрашивается в команде showItems)
                    //
                }
        }

//        String answer = nonCommand.nonCommandExecute(chatId, userName, msg.getText());
//        setAnswer(chatId, userName, answer);
    }

    /**
     * Получение города по id чата. Если ранее для этого чата в ходе сеанса работы бота настройки не были установлены,
     * вызывается команда выбора города
     */
    public City getUserCity(Long chatId) {
        JDBCConnector jdbcConnector = new JDBCConnector();
        City city = jdbcConnector.getUserCity(getUserSettings().get(chatId));
        if (city == null) {
            setAnswer(chatId, "Пожалуйста, выполните команду /city");
        }
        return city;
    }

    /**
     * Формирование имени пользователя
     * @param msg сообщение
     */
    private String getUserName(Message msg) {
        User user = msg.getFrom();
        String userName = user.getUserName();
        return (userName != null) ? userName : String.format("%s %s", user.getLastName(), user.getFirstName());
    }

    /**
     * Отправка ответа
     * @param chatId id чата
     * @param text текст ответа
     */
    private void setAnswer(Long chatId, String text) {
        SendMessage answer = new SendMessage();
        answer.setText(text);
        answer.setChatId(chatId.toString());
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static Map<Long, String> getUserSettings() {
        return userSettings;
    }
}
