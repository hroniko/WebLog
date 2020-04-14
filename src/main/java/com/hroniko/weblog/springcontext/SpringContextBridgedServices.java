package com.hroniko.weblog.springcontext;

import com.hroniko.weblog.sender.MessageSender;

public interface SpringContextBridgedServices {
    MessageSender getMessageSender();
}
