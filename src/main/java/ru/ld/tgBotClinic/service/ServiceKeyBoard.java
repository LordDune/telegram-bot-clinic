package ru.ld.tgBotClinic.service;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.ld.tgBotClinic.config.BotConfig;
import ru.ld.tgBotClinic.model.Mask;
import ru.ld.tgBotClinic.model.Record;
import java.util.*;

@Data
@Component
public class ServiceKeyBoard {


    static final String[] months = new String[] {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
    public static final String[] months1 = new String[] {"января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"};
    public static final String[] weeksDays = new String[] {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
    public static final String[] weeksDays1 = new String[] {"Понедельник", "Вторник", "Среда",
            "Четверг", "Пятница", "Суббота", "Воскресенье"};

    @Autowired
    public static BotConfig botConfig;

    public List<List<InlineKeyboardButton>> prepareReplyKeyboardDay(Calendar calendar, List<Mask> masks, List<Long> times) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Yekaterinburg")); // Europe/Moscow
        int monthNow = calendar.get(Calendar.MONTH);
        int yearNow = calendar.get(Calendar.YEAR);
        int dayNow = calendar.get(Calendar.DATE);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 2;
        if (dayOfWeek == -1) dayOfWeek = 6;

        var button = new InlineKeyboardButton();
        button.setText(weeksDays[dayOfWeek] + " " + dayNow + " " + months1[monthNow]);
        button.setCallbackData(" ");
        row.add(button);

        rows.add(row);

        row = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            button = new InlineKeyboardButton();
            Long time = new GregorianCalendar(yearNow, monthNow, dayNow, i+9, 0).getTimeInMillis();
            if (masks.get(dayOfWeek).getClocks().get(i+1) == 1 && !times.contains(time)) {

                button.setText((i+9) + ":00");
                button.setCallbackData("/record " + time);

            } else {
                button.setText(" ");
                button.setCallbackData(" ");
            }
            row.add(button);
        }
        rows.add(row);
        row = new ArrayList<>();
        for (int i = 3; i < 6; i++) {
            button = new InlineKeyboardButton();
            Long time = new GregorianCalendar(yearNow, monthNow, dayNow, i+9, 0).getTimeInMillis();
            if (masks.get(dayOfWeek).getClocks().get(i+1) == 1 && !times.contains(time)) {
                button.setText((i+9) + ":00");
                button.setCallbackData("/record " + time);

            } else {
                button.setText(" ");
                button.setCallbackData(" ");
            }
            row.add(button);
        }
        rows.add(row);
        row = new ArrayList<>();
        for (int i = 6; i < 9; i++) {
            button = new InlineKeyboardButton();
            Long time = new GregorianCalendar(yearNow, monthNow, dayNow, i+9, 0).getTimeInMillis();
            if (masks.get(dayOfWeek).getClocks().get(i+1) == 1 && !times.contains(time)) {
                button.setText((i+9) + ":00");
                button.setCallbackData("/record " + time);

            } else {
                button.setText(" ");
                button.setCallbackData(" ");
            }
            row.add(button);
        }
        rows.add(row);
        return rows;
    }

    public InlineKeyboardMarkup prepareReplyKeyboard(Calendar calendar, List<Mask> masks) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Yekaterinburg")); // Europe/Moscow
        int monthNow = calendar.get(Calendar.MONTH);
        int yearNow = calendar.get(Calendar.YEAR);
        calendar = new GregorianCalendar(yearNow, monthNow, 1);
        int countDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 2;
        if (dayOfWeek == -1) dayOfWeek = 6;

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        var button = new InlineKeyboardButton();
        button.setText("<");
        button.setCallbackData("/next " + (monthNow - 1) + " " + yearNow);
        row.add(button);

        button = new InlineKeyboardButton();
        button.setText(months[monthNow] + "\n" + yearNow);
        button.setCallbackData(" ");
        row.add(button);

        button = new InlineKeyboardButton();
        button.setText(">");
        button.setCallbackData("/next " + (monthNow + 1) + " " + yearNow);
        row.add(button);
        rows.add(row);

        row = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            button = new InlineKeyboardButton();
            button.setText(weeksDays[i]);
            button.setCallbackData(" ");
            row.add(button);
        }
        rows.add(row);

        row = new ArrayList<>();
        for (int i = 0; i < dayOfWeek; i++) {
            button = new InlineKeyboardButton();
            button.setText(String.valueOf(" "));
            button.setCallbackData(" ");
            row.add(button);
        }
        int day = 1;
        for (int i = dayOfWeek; i < 7; i++) {
            button = new InlineKeyboardButton();
            if (masks.get(i).isWork()) {
                button.setText(String.valueOf(day++));
                button.setCallbackData("/day " + (day-1) + " " + monthNow+ " " + yearNow);
            } else {
                day++;
                button.setText(" ");
                button.setCallbackData(" ");
            }
            row.add(button);
        }
        rows.add(row);

        while (day <= countDaysInMonth) {
            row = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                if (day <= countDaysInMonth) {
                    button = new InlineKeyboardButton();
                    if (masks.get(i).isWork()) {
                        button.setText(String.valueOf(day++));
                        button.setCallbackData("/day " + (day-1) + " " + monthNow+ " " + yearNow);
                    } else {
                        day++;
                        button.setText(" ");
                        button.setCallbackData(" ");
                    }
                    row.add(button);
                } else {
                    button = new InlineKeyboardButton();
                    button.setText(" ");
                    button.setCallbackData(" ");
                    row.add(button);
                }
            }
            rows.add(row);
        }

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    public InlineKeyboardMarkup prepareReplyMask (List<Mask> listMask) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        var button = new InlineKeyboardButton();
        for (int i = 0; i < 7; i++) {
            button = new InlineKeyboardButton();
            if (listMask.get(i).isWork() == true) {
                button.setText(weeksDays[i]);
                button.setCallbackData("/off_day " + i);
                row.add(button);
            } else {
                button.setText(" ");
                button.setCallbackData("/on_day " + i);
                row.add(button);
            }
        }
        rows.add(row);

        for (int i = 0; i < 9; i++) {
            row = new ArrayList<>();
            for (int j = 0; j < 7; j++) {
                button = new InlineKeyboardButton();
                if (listMask.get(j).getClocks().get(i+1) == 1 && listMask.get(j).isWork() == true) {
                    button.setText((9+i) + ":00");
                    button.setCallbackData("/off_clock " + j + " " + i);
                } else {
                    button.setText(" ");
                    button.setCallbackData("/on_clock " + j + " " + i);
                }
                row.add(button);
            }
            rows.add(row);
        }
        keyboard.setKeyboard(rows);
        return keyboard;
    }

    public InlineKeyboardMarkup prepareTransferRecord (Record recordOld, Long timeNew) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        var button = new InlineKeyboardButton();
        button.setText("Да");
        button.setCallbackData("/transferRecord " + recordOld.getTime() + " " + timeNew);
        row.add(button);
        button = new InlineKeyboardButton();
        button.setText("Нет");
        button.setCallbackData("/cansel");
        row.add(button);
        rows.add(row);
        keyboard.setKeyboard(rows);
        return keyboard;
    }

    public InlineKeyboardMarkup prepareDeleteRecord (Record record) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        var button = new InlineKeyboardButton();
        button.setText("Да");
        button.setCallbackData("/deleteRecord " + record.getTime());
        row.add(button);
        button = new InlineKeyboardButton();
        button.setText("Нет");
        button.setCallbackData("/cansel");
        row.add(button);
        rows.add(row);
        keyboard.setKeyboard(rows);
        return keyboard;
    }

    public InlineKeyboardMarkup prepareRecordsToClient (List<Record> records) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Record r: records) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            var button = new InlineKeyboardButton();
            button.setText(r.getTimeInfo());
            button.setCallbackData("/prepareDeleteRecord " + r.getTime());
            row.add(button);
            rows.add(row);
        }
        keyboard.setKeyboard(rows);
        return keyboard;
    }

    public StringBuilder prepareRecordsToOwner (List<Record> records) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < records.size(); i++) {
            sb.append(i+1).append(". ").append(records.get(i).getTimeInfo()).append(".\nКлиент: ").append(records.get(i).getClientInfo()).append("\n\n");
        }
        return sb;
    }

    public ReplyKeyboardMarkup prepareReplyKeyboardOwner () {
        // создаем экранную клавиатуру
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        // изменяем размер кнопок клавиатуры под размер текста
        replyKeyboardMarkup.setResizeKeyboard(true);
        // создаем список рядов кнопок
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        //Создаем ряд
        KeyboardRow row = new KeyboardRow();
        // добавляем кнопки в ряд
        row.add("Настроить расписание");
        // Добавляем ряд в список рядов
        keyboardRows.add(row);
        row = new KeyboardRow();
        keyboardRows.add(row);
        // Добавляем список рядов в клавиатуру
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;
    }

    public ReplyKeyboardMarkup prepareReplyKeyboardClient () {
        // создаем экранную клавиатуру
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        // изменяем размер кнопок клавиатуры под размер текста
        replyKeyboardMarkup.setResizeKeyboard(true);
        // создаем список рядов кнопок
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        //Создаем ряд
        KeyboardRow row = new KeyboardRow();
        // добавляем кнопки в ряд
        row.add("Записаться на прием");
        row.add("Мои записи");
        // Добавляем ряд в список рядов
        keyboardRows.add(row);
        row = new KeyboardRow();
        keyboardRows.add(row);
        // Добавляем список рядов в клавиатуру
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;
    }
}
