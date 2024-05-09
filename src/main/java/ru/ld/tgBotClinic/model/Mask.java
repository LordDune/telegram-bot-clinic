package ru.ld.tgBotClinic.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity(name = "mask")
public class Mask implements Comparable<Mask> {

    @Id
    private Long id;

    private boolean isWork;

    private int clocks;

    public ArrayList<Integer> getClocks() {
        ArrayList<Integer> list = new ArrayList<>();
        for (int i = this.clocks; i > 0; i/=10) {
            int placeNumber = i%10;
            list.add(0, placeNumber);
        }
        return list;
    }

    public void setClocks(List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (int placeNumber: list) {
            sb.append(placeNumber);
        }
        this.clocks = Integer.valueOf(sb.toString());
    }

    public void setClocks() {
        this.clocks = 1111111111;
    };

    @Override
    public String toString() {
        return "Mask{" +
                "id=" + id +
                ", isWork=" + isWork +
                ", clocks=" + clocks +
                '}';
    }

    @Override
    public int compareTo(Mask o) {
        return id.compareTo(o.id);
    }
}
