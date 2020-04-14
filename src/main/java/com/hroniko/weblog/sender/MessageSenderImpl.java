package com.hroniko.weblog.sender;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageSenderImpl implements MessageSender {


    @Autowired
    SimpMessagingTemplate template;

    @Override
    public void sendMessage(String message) {
        template.convertAndSend("/topic/restlog", message);
    }
}
