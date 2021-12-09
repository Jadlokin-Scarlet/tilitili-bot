package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.mirai.MiraiRequest;
import com.tilitili.bot.service.MiraiSessionService;
import com.tilitili.common.entity.mirai.MessageChain;
import com.tilitili.common.entity.mirai.MiraiMessage;
import com.tilitili.common.entity.mirai.MiraiMessageView;
import com.tilitili.common.manager.MiraiManager;
import com.tilitili.common.utils.QQUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class RepeatHandle implements BaseMessageHandle {
    private final String KeyKey = "repeat.key";
    private final String valueKey = "repeat.value";
    private final String numberKey = "repeat.number";

    private final MiraiManager miraiManager;

    @Autowired
    public RepeatHandle(MiraiManager miraiManager) {
        this.miraiManager = miraiManager;
    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.RepeatHandle;
    }

    @Override
    public MiraiMessage handleMessage(MiraiRequest request) {
        MiraiSessionService.MiraiSession session = request.getSession();
        MiraiMessageView message = request.getMessage();
        List<MessageChain> messageChainList = message.getMessageChain();
        String key = getKey(messageChainList);

        String oldKey = session.getOrDefault(KeyKey, "");
        int oldNumber = Integer.parseInt(session.getOrDefault(numberKey, "0"));
        if (oldKey.equals(key)) {
            session.put(numberKey, String.valueOf(oldNumber + 1));
        } else {
            session.put(numberKey, "1");
            session.put(KeyKey, key);
        }

        String newNumber = session.get(numberKey);
        if (Objects.equals(newNumber, "3")) {
            List<MessageChain> newMessageChainList = messageChainList.stream().map(messageChain -> {
                if (messageChain.getType().equals("Plain")) {
                    return new MessageChain().setType("Plain").setText(messageChain.getText());
                } else if (messageChain.getType().equals("Image")) {
                    return new MessageChain().setType("Image").setUrl(QQUtil.getImageUrl(messageChain.getUrl()));
                } else {
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
            miraiManager.sendMessage(new MiraiMessage().setMessageType("List").setMessageChainList(newMessageChainList).setSendType("GroupMessage").setGroup(message.getSender().getGroup().getId()));
        }
        return null;
    }

    public String getKey(List<MessageChain> messageChainList) {
        return messageChainList.stream().map(messageChain -> {
            if (messageChain.getType().equals("Plain")) {
                return messageChain.getText();
            } else if (messageChain.getType().equals("Image")) {
                return messageChain.getImageId();
            } else {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.joining(","));
    }
}
