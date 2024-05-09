package ru.ld.tgBotClinic.controller;


import com.vdurmont.emoji.EmojiParser;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.ld.tgBotClinic.config.BotConfig;
import ru.ld.tgBotClinic.model.*;
import ru.ld.tgBotClinic.model.Record;
import ru.ld.tgBotClinic.service.ServiceData;
import ru.ld.tgBotClinic.service.ServiceKeyBoard;

import java.io.Serializable;
import java.util.*;

// Создание логов. Доступны через объект log
@Data
@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    final BotConfig config;

    @Autowired
    private ServiceData serviceData;

    @Autowired
    private ServiceKeyBoard serviceKeyBoard;

    static final String ERROR_TEXT = "Error occurred: ";

    public List<Record> records = new ArrayList<>();
    public List<Long> times = new ArrayList<>();
    public List<List<Integer>> masks = new ArrayList<>();


    public TelegramBot(BotConfig config) {
        this.config = config;
        // меню бота. перечень команд должен совпадать с пунктами метода onUpdateReceived(Update update)
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Информация о сервисе"));
        listOfCommands.add(new BotCommand("/registration", "Записаться на прием"));
        listOfCommands.add(new BotCommand("/records", "Посмотреть мои записи"));
        // обновление меню бота в телеграм
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            e.printStackTrace();
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        // проверка на полученный текст
        updateRecords();
        updateMask();
        for (Record record : records) {
            System.out.println(record);
        }

        for (long q : times) {
            System.out.println(q);
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId(); // получаем chatId в формате long

            // ОТПРАВКА СООБЩЕНИЯ ВСЕМ ПОЛЬЗОВАТЕЛЯМ
            // Проверка на наличие в сообщении "/send"
            if (messageText.contains("/send") && chatId == config.getOwnerId()) {
                // вычленяем все, что идет после пробела (после команды "/send")
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                var users = serviceData.findAllUsers();
                for (User user : users) {
                    prepareAndSendMessage(user.getChatId(), textToSend);
                }
            } else if (messageText.contains("Настроить расписание") && chatId == config.getOwnerId()) {
                List<Mask> maskList = serviceData.findAllMasks();
                SendMessage message = new SendMessage();
                message.setReplyMarkup(serviceKeyBoard.prepareReplyMask(maskList));
                message.setChatId(String.valueOf(chatId));
                message.setText("Настройте расписание:");
                executeMessage(message);
            } else if (messageText.contains("/start")) {
                registerUser(update.getMessage());
                StartCommandReceived(chatId, update.getMessage().getChat().getFirstName());
            } else if (messageText.contains("/registration") || messageText.contains("Записаться на прием")) {
                registration(chatId);
            } else if (messageText.contains("/records") || messageText.contains("Мои записи")) {
                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(chatId));
                if (config.getOwnerId().equals(chatId)) {
                    if (records.size() == 0) {
                        message.setText("У вас нет доступных записей.");
                    } else {
                        message.setText("Ваши записи: \n" + serviceKeyBoard.prepareRecordsToOwner(records));
                        }
                            executeMessage(message);
                        } else {
                            List<Record> myRecord = records.stream().filter(m -> m.getClientId().equals(chatId)).toList();
                            if (myRecord.size() == 0) {
                                message.setText("У вас нет доступных записей.\nЧтобы записаться на онлайн-прием, выберите в меню пункт /registration.");
                            } else {
                                message.setReplyMarkup(serviceKeyBoard.prepareRecordsToClient(myRecord));
                                message.setText("Ваши записи перечислены ниже.\nДля отмены нажмите на ячейку с датой.\nДля связи с врачом вы можете написать в Telegram @LordDune");
                            }
                            executeMessage(message);
                        }
                }
            }
            // проверка на нажатую кнопку
        else if (update.hasCallbackQuery()) {
                String callBackData = update.getCallbackQuery().getData();
                long messageId = update.getCallbackQuery().getMessage().getMessageId();
                long chatId = update.getCallbackQuery().getMessage().getChatId();
                System.out.println(callBackData);

                if (callBackData.contains("/next")) {
                    List<String> list = Arrays.stream(callBackData.split(" ")).toList();
                    int month = Integer.parseInt(list.get(1));
                    int year = Integer.parseInt(list.get(2));
                    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Yekaterinburg")); // Europe/Moscow
                    Calendar calendar = new GregorianCalendar(year, month, 1);
                    EditMessageReplyMarkup editMessage = new EditMessageReplyMarkup();
                    editMessage.setReplyMarkup(serviceKeyBoard.prepareReplyKeyboard(calendar, serviceData.findAllMasks()));
                    editMessage.setChatId(String.valueOf(chatId));
                    editMessage.setMessageId((int) messageId);
                    executeMessage(editMessage);

                } else if (callBackData.contains("/day")) {
                    List<String> list = Arrays.stream(callBackData.split(" ")).toList();
                    int month = Integer.parseInt(list.get(2));
                    int year = Integer.parseInt(list.get(3));
                    int day = Integer.parseInt(list.get(1));
                    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Yekaterinburg")); // Europe/Moscow
                    Calendar calendar = new GregorianCalendar(year, month, day);
                    Calendar calendar1 = new GregorianCalendar(year, month, 1);

                    EditMessageReplyMarkup message = new EditMessageReplyMarkup();
                    message.setChatId(String.valueOf(chatId));
                    message.setMessageId((int) messageId);
                    message.setReplyMarkup(serviceKeyBoard.prepareReplyKeyboard(calendar1, serviceData.findAllMasks()));
                    for (var c : serviceKeyBoard.prepareReplyKeyboardDay(calendar, serviceData.findAllMasks(), times)) {
                        message.getReplyMarkup().getKeyboard().add(c);
                    }
//                message.setReplyMarkup(prepareReplyKeyboardDay(calendar));
                    executeMessage(message);
                } else if (callBackData.contains("/off_day") && chatId == config.getOwnerId()) {
                    List<String> list = Arrays.stream(callBackData.split(" ")).toList();
                    Long day = Long.parseLong(list.get(1));
                    System.out.println(day);
                    Mask mask = serviceData.getMaskById(day);
                    mask.setWork(false);
                    serviceData.saveMask(mask);


                    List<Mask> maskList = serviceData.findAllMasks();
                    EditMessageReplyMarkup message = new EditMessageReplyMarkup();

                    message.setReplyMarkup(serviceKeyBoard.prepareReplyMask(maskList));
                    message.setChatId(String.valueOf(chatId));
                    message.setMessageId((int) messageId);
                    executeMessage(message);
                    updateMask();
                } else if (callBackData.contains("/on_day") && chatId == config.getOwnerId()) {
                    List<String> list = Arrays.stream(callBackData.split(" ")).toList();
                    Long day = Long.parseLong(list.get(1));
                    Mask mask = serviceData.getMaskById(day);
                    mask.setWork(true);
                    serviceData.saveMask(mask);

                    List<Mask> maskList = serviceData.findAllMasks();
                    EditMessageReplyMarkup message = new EditMessageReplyMarkup();

                    message.setReplyMarkup(serviceKeyBoard.prepareReplyMask(maskList));
                    message.setChatId(String.valueOf(chatId));
                    message.setMessageId((int) messageId);
                    executeMessage(message);
                    updateMask();
                } else if (callBackData.contains("/off_clock") && chatId == config.getOwnerId()) {
                    List<String> list = Arrays.stream(callBackData.split(" ")).toList();
                    Long day = Long.parseLong(list.get(1));
                    Integer clock = Integer.parseInt(list.get(2));
                    Mask mask = serviceData.getMaskById(day);
                    List<Integer> listClock = mask.getClocks();
                    listClock.set(clock + 1, 0);
                    mask.setClocks(listClock);
                    serviceData.saveMask(mask);

                    List<Mask> maskList = serviceData.findAllMasks();
                    EditMessageReplyMarkup message = new EditMessageReplyMarkup();

                    message.setReplyMarkup(serviceKeyBoard.prepareReplyMask(maskList));
                    message.setChatId(String.valueOf(chatId));
                    message.setMessageId((int) messageId);
                    executeMessage(message);
                    updateMask();

                } else if (callBackData.contains("/on_clock") && chatId == config.getOwnerId()) {
                    List<String> list = Arrays.stream(callBackData.split(" ")).toList();
                    Long day = Long.parseLong(list.get(1));
                    Integer clock = Integer.parseInt(list.get(2));
                    Mask mask = serviceData.getMaskById(day);
                    List<Integer> listClock = mask.getClocks();
                    listClock.set(clock + 1, 1);
                    mask.setClocks(listClock);
                    serviceData.saveMask(mask);

                    List<Mask> maskList = serviceData.findAllMasks();
                    EditMessageReplyMarkup message = new EditMessageReplyMarkup();

                    message.setReplyMarkup(serviceKeyBoard.prepareReplyMask(maskList));
                    message.setChatId(String.valueOf(chatId));
                    message.setMessageId((int) messageId);
                    executeMessage(message);
                    updateMask();

                } else if (callBackData.contains("/record")) {
                    List<String> list = Arrays.stream(callBackData.split(" ")).toList();
                    Long time = Long.parseLong(list.get(1));
                    Date date = new Date(time);
                    if (!times.contains(time) && isRecord(chatId).size() == 0) {
                        System.out.println(date);
                        Record record = new Record();
                        record.setTime(time);
                        record.setClientId(chatId);
                        record.setDescription("Запись");
                        record.setName(update.getCallbackQuery().getMessage().getChat().getFirstName());
                        record.setLastName(update.getCallbackQuery().getMessage().getChat().getLastName());
                        record.setNick("@" + update.getCallbackQuery().getMessage().getChat().getUserName());
                        serviceData.saveRecord(record);
                        updateRecords();
                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(String.valueOf(chatId));
                        sendMessage.setText("Вы успешно записаны: " + record.getTimeInfo()
                        + ".\nСвязаться с врачом можно через телеграм @LordDune");
                        executeMessage(sendMessage);
                        // Сообщение врачу
                        sendMessage(config.getOwnerId(), "Новая запись: " + record.getTimeInfo() + ". " + record.getClientInfo());
                    } else if (isRecord(chatId).size() > 0) {

                        String dateClient = isRecord(chatId).get(0).getTimeInfo();
                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(String.valueOf(chatId));
                        sendMessage.setText("Вы уже записаны на другое время: " + dateClient + ".\nВы уверены, что хотите перенести существующую запись?");
                        sendMessage.setReplyMarkup(serviceKeyBoard.prepareTransferRecord(isRecord(chatId).get(0), time));
                        executeMessage(sendMessage);
                    } else {
                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(String.valueOf(chatId));
                        sendMessage.setText("Извините, данное время уже недоступно.");
                        executeMessage(sendMessage);
                    }

                    EditMessageReplyMarkup message = new EditMessageReplyMarkup();
                    message.setChatId(String.valueOf(chatId));
                    message.setMessageId((int) messageId);
                    message.setReplyMarkup(serviceKeyBoard.prepareReplyKeyboard(new GregorianCalendar(date.getYear() + 1900, date.getMonth(), 1), serviceData.findAllMasks()));
                    for (var c : serviceKeyBoard.prepareReplyKeyboardDay(new GregorianCalendar(date.getYear() + 1900, date.getMonth(), date.getDate()), serviceData.findAllMasks(), times)) {
                        message.getReplyMarkup().getKeyboard().add(c);
                    }
                    executeMessage(message);
                    updateRecords();
                } else if (callBackData.contains("/transferRecord")) {
                    List<String> list = Arrays.stream(callBackData.split(" ")).toList();
                    Long timeOld = Long.parseLong(list.get(1));
                    Long timeNew = Long.parseLong(list.get(2));
                    Record record = serviceData.findByTime(timeOld).get();
                    if (record.getClientId().equals(chatId)) {
                        sendMessage(config.getOwnerId(), "Запись отменена: " + record.getTimeInfo() + ". " + record.getClientInfo());
                        serviceData.deleteRecord(timeOld);
                        record.setTime(timeNew);
                        serviceData.saveRecord(record);
                        updateRecords();
                        sendMessage(config.getOwnerId(), "Новая запись: " + record.getTimeInfo() + ". " + record.getClientInfo());
                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(String.valueOf(chatId));
                        sendMessage.setText("Вы успешно записаны: " + record.getTimeInfo()
                                + ".\nСвязаться с врачом можно через телеграм @LordDune");
                        executeMessage(sendMessage);


                    } else {
                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(String.valueOf(chatId));
                        sendMessage.setText("Ошибка. На указанное время записан другой человек.");
                        executeMessage(sendMessage);
                    }

                } else if (callBackData.contains("/prepareDeleteRecord")) {
                    List<String> list = Arrays.stream(callBackData.split(" ")).toList();
                    Long time = Long.parseLong(list.get(1));
                    Record record = serviceData.findByTime(time).get();
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(String.valueOf(chatId));
                    sendMessage.setText("Вы уверены, что хотите удалить существующую запись?");
                    sendMessage.setReplyMarkup(serviceKeyBoard.prepareDeleteRecord(record));
                    executeMessage(sendMessage);

                } else if (callBackData.contains("/deleteRecord")) {
                    List<String> list = Arrays.stream(callBackData.split(" ")).toList();
                    Long time = Long.parseLong(list.get(1));
                    Record record = serviceData.findByTime(time).get();
                    if (record.getClientId().equals(chatId)) {
                        serviceData.deleteRecord(time);
                        updateRecords();
                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(String.valueOf(chatId));
                        sendMessage.setText("Ваша запись удалена.");
                        sendMessage(config.getOwnerId(), "Запись отменена: " + record.getTimeInfo() + ". " + record.getClientInfo());
                        executeMessage(sendMessage);
                    } else {
                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(String.valueOf(chatId));
                        sendMessage.setText("Ошибка. На указанное время записан другой человек.");
                        executeMessage(sendMessage);
                    }

                }   else if (callBackData.contains("/cansel")) {
                    EditMessageReplyMarkup message = new EditMessageReplyMarkup();
                    message.setChatId(String.valueOf(chatId));
                    message.setMessageId((int) messageId);
                    message.setReplyMarkup(null);
                    executeMessage(message);
                }
            }
        }



        private void registration ( long chatId){
            Calendar calendar = Calendar.getInstance();
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Выберите удобную дату и время для записи");
            message.setReplyMarkup(serviceKeyBoard.prepareReplyKeyboard(calendar, serviceData.findAllMasks()));
            executeMessage(message);
        }

        // Метод регистрации пользователя (при первом обращении "/start")
        private void registerUser (Message message){
            serviceData.registerUser(message);
        }

        private void StartCommandReceived ( long chatId, String name){
            // Ответ с использованием смайликов (https://emojipedia.org/)
            String answer = EmojiParser.parseToUnicode("Привет, " + name + ". Рады тебя видеть" + " :blush:" +
                    " Это телеграм бот врача-терапевта Сергея Стяжкина @LordDune. Чтобы записаться на прием, кликните в меню " +
                    "соответствующую команду и выберите удобные дату и время.");
//        String answer = "Hi, " + name + ". Nice to meet you!";
            sendMessage(chatId, answer);
            log.info("Replied to user " + name);
        }

        // метод отправки сообщений
        private void sendMessage ( long chatId, String text){
            // Создаем исходящее сообщение
            SendMessage message = new SendMessage();
            // присваиваем сообщению chatId - кому отправить
            message.setChatId(String.valueOf(chatId)); // отправлять chatId должны в формате String
            // присваиваем сообщению текст
            message.setText(text);

            // Прикрепляем клавиатуру к нашему сообщению
            if (config.getOwnerId().equals(chatId)) {
                message.setReplyMarkup(serviceKeyBoard.prepareReplyKeyboardOwner());
            } else message.setReplyMarkup((serviceKeyBoard.prepareReplyKeyboardClient()));
            executeMessage(message);
        }

        private void executeEditMessageText (String text,long chatId, long messageId){
            EditMessageText message = new EditMessageText();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);
            message.setMessageId((int) messageId);
            executeMessage(message);
        }

        private void executeMessage (SendMessage message){
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
                log.error(ERROR_TEXT + e.getMessage());
            }
        }

        private void executeMessage (BotApiMethod < Serializable > message) {
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
                log.error(ERROR_TEXT + e.getMessage());
            }
        }

        private void prepareAndSendMessage ( long chatId, String text){
            // Создаем исходящее сообщение
            SendMessage message = new SendMessage();
            // присваиваем сообщению chatId - кому отправить
            message.setChatId(String.valueOf(chatId)); // отправлять chatId должны в формате String
            // присваиваем сообщению текст
            message.setText(text);
            executeMessage(message);
        }

//    @Scheduled(cron = "${cron.scheduler}")
//    private void sendAds(){
//        var ads = adsRepository.findAll();
//        var users = userRepository.findAll();
//        for (Ads ad: ads) {
//            for (User user: users) {
//                prepareAndSendMessage(user.getChatId(), ad.getAd());
//            }
//        }
//    }

        private void updateRecords () {
            records = serviceData.records();
            Collections.sort(records);
            for (Record record : records) {
                times.add(record.getTime());
            }
        }

    private void updateMask () {
        masks = new ArrayList<>();
        List<Mask> masksList = serviceData.findAllMasks();
        Collections.sort(masksList);
        for (Mask mask: masksList) {
            masks.add(mask.getClocks());
        }
        System.out.println(masks);
    }

        private List<Record> isRecord (Long chatId){
            List<Record> clientsRecord = new ArrayList<>();
            Long millis = new Date().getTime();
            for (Record record : records) {
                if (record.getClientId().equals(chatId) && record.getTime() > millis) {
                    clientsRecord.add(record);
                }
            }
            return clientsRecord;
        }

    }







