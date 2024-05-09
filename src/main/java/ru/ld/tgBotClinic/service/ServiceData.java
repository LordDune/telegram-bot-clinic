package ru.ld.tgBotClinic.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.ld.tgBotClinic.model.*;
import ru.ld.tgBotClinic.model.Record;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@Data
public class ServiceData {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MaskRepository maskRepository;
    @Autowired
    private RecordRepository recordRepository;

    public List<Record> records() {
        return recordRepository.findAll();
    };

    public Iterable<User> findAllUsers() {
        return userRepository.findAll();
    }

    public List<Mask> findAllMasks() {
        List<Mask> maskList = maskRepository.findAll();
        if (!maskList.iterator().hasNext()) {
            for (long i = 0; i < 7; i++) {
                Mask mask = new Mask();
                mask.setId(i);
                mask.setWork(true);
                mask.setClocks();
                maskRepository.save(mask);
            }
        }
        maskList = maskRepository.findAll();
        maskList.sort(new Comparator<Mask>() {
            @Override
            public int compare(Mask o1, Mask o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
        return maskList;
    }

    public void registerUser(Message message) {
        if (userRepository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();

            User user = new User();
            user.setChatId(chatId);
            user.setUserName(chat.getUserName());
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("Created user: " + user);
        }
    }

    public void saveMask(Mask mask) {
        maskRepository.save(mask);
    }

    public Mask getMaskById(Long id) {
        Mask mask = maskRepository.findAll().stream().filter(m->m.getId().equals(id)).findFirst().get();
        return mask;
    }

    public void saveRecord(Record record) {
        recordRepository.save(record);
    }

    public void deleteRecord(Long id) {
        recordRepository.deleteById(id);
    }

    public Optional<Record> findByTime(Long time) {
        return recordRepository.findById(time);
    }
}
