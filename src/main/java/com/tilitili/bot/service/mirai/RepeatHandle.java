package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.utils.QQUtil;
import com.tilitili.common.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class RepeatHandle extends ExceptionRespMessageHandle {
    private final String KeyKey = "repeat.key";
    private final String recordKey = "repeat.record";
    private final String numberKey = "repeat.number";

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        BotSessionService.MiraiSession session = messageAction.getSession();
        List<BotMessageChain> messageChainList = messageAction.getBotMessage().getBotMessageChainList();
        String key = getKey(messageChainList);
        if (StringUtils.isBlank(key)) return null;

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
            List<BotMessageChain> newMessageChainList = messageChainList.stream().map(messageChain -> {
                if (messageChain.getType().equals("Plain")) {
                    return BotMessageChain.ofPlain(messageChain.getText());
                } else if (messageChain.getType().equals("Image")) {
                    return BotMessageChain.ofImage(QQUtil.getImageUrl(messageChain.getUrl()));
                } else {
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
            return new BotMessage().setBotMessageChainList(newMessageChainList);
        }
        return null;
    }

    public String getKey(List<BotMessageChain> messageChainList) {
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
