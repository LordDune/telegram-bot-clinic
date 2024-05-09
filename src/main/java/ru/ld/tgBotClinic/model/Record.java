package ru.ld.tgBotClinic.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static ru.ld.tgBotClinic.service.ServiceKeyBoard.*;

@Entity
@Data
public class Record implements Comparable<Record> {
    @Id
    Long time;

    Long clientId;

    String description;

    String name;

    String lastName;

    String nick;

    public String getTimeInfo() {
        StringBuilder sb = new StringBuilder();
        Date date = new Date(getTime());
        int year = date.getYear();
        int month = date.getMonth();
        int day = date.getDate();
        int hour = date.getHours();
        int min = date.getMinutes();
        Calendar calendar = new GregorianCalendar(year+1900, month, day, hour, min);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 2;
        if (dayOfWeek == -1) dayOfWeek = 6;
        sb.append(weeksDays1[dayOfWeek])
                .append(" ")
                .append(day)
                .append(" ")
                .append(months1[month])
                .append(" ")
                .append(year+1900)
                .append(" в ")
                .append(hour).append(":").append("00");
        return sb.toString();
    }

      public String getClientInfo() {
        StringBuilder sb = new StringBuilder();
                sb.append(name).append(" ").append(lastName).append(" ").append(nick);
        return sb.toString();
    }

    @Override
    public int compareTo(Record o) {
        return getTime().compareTo(o.getTime());
    }
}
