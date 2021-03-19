import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.regex.Pattern;

public class MyBot extends TelegramLongPollingBot {
    public static final String BOT_NAME = "YOUR-BOT-NAME-CREATED-WITH-BotFather";
    public static final String BOT_TOKEN = "YOUR-BOT-TOKEN-GIVEN-BY-BotFather";

    static public HashMap<Long, ScannerUser> users_ID = new HashMap<>(); //user ID - user settings & scanner

    private long chat_id;

    /**
     * Global message processor, passes update to specific command processor
     *
     * @param update object of incoming message with information in it
     */

    @Override
    public void onUpdateReceived(Update update) {

        chat_id = update.getMessage().getChatId();

        String text = update.getMessage().getText();

        if (text.startsWith("/start")) start(update);
        if (text.startsWith("/run")) runScanner(update);
        if (text.startsWith("/setPrice")) setPrice(update);
        if (text.startsWith("/setArea")) setArea(update);
        if (text.startsWith("/setPeriod")) setPeriod(update);
        if (text.startsWith("/settings")) getSettings(update);
        if (text.startsWith("/stop")) stopScanner(update);
        if (text.startsWith("/switchMode")) switchBotMode(update);


    }

    //replies to user
    public void sendScannerIsStopped(Long chat_id) {
        try {
            SendMessage sendMessage = new SendMessage().setChatId(chat_id).setText("Сканнер остановлен");
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendNoUser(Long chat_id) {
        try {
            SendMessage sendMessage = new SendMessage().setChatId(chat_id).setText("Мы с тобой не знакомы. Напиши /start для началы работы");
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendNoSuccess(Long chat_id) {
        try {
            SendMessage sendMessage = new SendMessage().setChatId(chat_id).setText("Неудача. Попробуй еще раз");
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendSettingsChanged(Long chat_id) {
        try {
            SendMessage sendMessage = new SendMessage().setChatId(chat_id).setText("Настройки изменены");
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    //command processors
    public void start(Update update) {
        if (!users_ID.containsKey(chat_id)) users_ID.put(chat_id, new ScannerUser(chat_id));
        SendMessage sendMessage = new SendMessage().setChatId(update.getMessage().getChatId());
        try {
            sendMessage.setText("Ты можешь задать свои параметры для сканера или запустить с установленными по умолчанию\n\nСписок комманд:\n\n/run - запустить сканер\n/stop - остановить сканер\n/settings - посмотреть настройки\n" +
                    "|/setPrice 999-999| - установить спектр цен (€/мес. или € при режиме \"Продажа\")\n|/setArea 999| - установить площадь от (кв.м.)\n|/setPeriod 999| - установить время обновления (мин.).\n|/switchMode| - поменять режим бота.");
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void stopScanner(Update update) {
        if (!users_ID.containsKey(chat_id)) sendNoUser(chat_id);
        else {
            boolean timerOn = users_ID.get(chat_id).timerStop();
            if (timerOn) sendScannerIsStopped(chat_id);
            else {
                try {
                    SendMessage sendMessage = new SendMessage().setChatId(update.getMessage().getChatId()).setText("Сканнер не запущен");
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public void runScanner(Update update) {
        if (!users_ID.containsKey(chat_id)) sendNoUser(chat_id);
        else {
            boolean timerWasOff = users_ID.get(chat_id).timerStart();
            if (timerWasOff) {
                try {
                    SendMessage sendMessage = new SendMessage().setChatId(update.getMessage().getChatId()).setText("Сканнер запущен - жди объявлений!");
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    SendMessage sendMessage = new SendMessage().setChatId(update.getMessage().getChatId()).setText("Сканнер уже запущен");
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setPrice(Update update) {
        if (!users_ID.containsKey(chat_id)) sendNoUser(chat_id);
        else {
            String text = update.getMessage().getText();
            boolean correctText = Pattern.compile("^/setPrice \\d+-\\d+$").matcher(text).matches();
            if (!correctText) sendNoSuccess(chat_id);
            else {
                String priceRange = text.substring(text.indexOf(" ") + 1);
                String[] prices = priceRange.split("-");
                int[] intP = new int[2];
                intP[0] = Integer.parseInt(prices[0]);
                intP[1] = Integer.parseInt(prices[1]);
                if (intP[0] > intP[1]) sendNoSuccess(chat_id);

                else {
                    boolean timerWasOn = users_ID.get(chat_id).setUserPrice1(intP[0]);
                    users_ID.get(chat_id).setUserPrice2(intP[1]);

                    if (timerWasOn) sendScannerIsStopped(chat_id);
                    sendSettingsChanged(chat_id);
                }
            }

        }
    }

    public void setArea(Update update) {
        if (!users_ID.containsKey(chat_id)) sendNoUser(chat_id);
        else {
            String text = update.getMessage().getText();
            boolean correctText = Pattern.compile("^/setArea \\d+$").matcher(text).matches();
            if (!correctText) sendNoSuccess(chat_id);
            else {
                String area = text.substring(text.indexOf(" ") + 1);
                boolean timerWasOn = users_ID.get(chat_id).setUserArea(Integer.parseInt(area));

                if (timerWasOn) sendScannerIsStopped(chat_id);
                sendSettingsChanged(chat_id);
            }
        }
    }

    public void setPeriod(Update update) {
        if (!users_ID.containsKey(chat_id)) sendNoUser(chat_id);
        else {
            String text = update.getMessage().getText();
            boolean correctText = Pattern.compile("^/setPeriod \\d+$").matcher(text).matches();
            if (!correctText) sendNoSuccess(chat_id);
            else {
                String period = text.substring(text.indexOf(" ") + 1);
                boolean timerWasOn = users_ID.get(chat_id).setUserPeriod(Integer.parseInt(period));

                if (timerWasOn) sendScannerIsStopped(chat_id);
                sendSettingsChanged(chat_id);
            }
        }
    }

    public void getSettings(Update update) {
        if (!users_ID.containsKey(chat_id)) sendNoUser(chat_id);
        else {
            int price1 = users_ID.get(chat_id).getUserPrice1();
            int price2 = users_ID.get(chat_id).getUserPrice2();
            int area = users_ID.get(chat_id).getUserArea();
            int period = users_ID.get(chat_id).getUserPeriod();
            String mode = users_ID.get(chat_id).getCurrentMode();

            String moneyUnit;
            if (mode.equals("RENT")) {
                mode = "Аренда";
                moneyUnit = " €/мес.\n";
            } else {
                mode = "Продажа";
                moneyUnit = " €.\n";
            }

            try {
                SendMessage sendMessage = new SendMessage().setChatId(update.getMessage().getChatId()).setText("Цена: " + price1 + "-" + price2 + moneyUnit +
                        "Площадь от: " + area + " кв.м.\n" +
                        "Время обновления: " + period + " мин.\n" +
                        "Режим: " + mode);
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void switchBotMode(Update update) {
        if (!users_ID.containsKey(chat_id)) sendNoUser(chat_id);
        else {
            try {
                boolean timerWasOn;
                if (users_ID.get(chat_id).getCurrentMode().equals(users_ID.get(chat_id).MODES[0])) {
                    timerWasOn = users_ID.get(chat_id).setCurrentMode(users_ID.get(chat_id).MODES[1]);
                    SendMessage sendMessage = new SendMessage().setChatId(update.getMessage().getChatId()).setText("Выбран режим \"Продажа\"");
                    execute(sendMessage);
                } else {
                    timerWasOn = users_ID.get(chat_id).setCurrentMode(users_ID.get(chat_id).MODES[0]);
                    SendMessage sendMessage = new SendMessage().setChatId(update.getMessage().getChatId()).setText("Выбран режим \"Аренда\"");
                    execute(sendMessage);
                }

                if (timerWasOn) sendScannerIsStopped(chat_id);
                sendSettingsChanged(chat_id);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @return token given by BotFather
     */
    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    /**
     * @return bot's name
     */
    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }
}
