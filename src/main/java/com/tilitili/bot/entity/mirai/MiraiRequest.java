package com.tilitili.bot.entity.mirai;

import com.tilitili.bot.service.MiraiSessionService;
import com.tilitili.common.entity.mirai.MessageChain;
import com.tilitili.common.entity.mirai.MiraiMessageView;
import com.tilitili.common.utils.StreamUtil;
import com.tilitili.common.utils.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MiraiRequest {
    private final MiraiSessionService.MiraiSession session;
    private final MiraiMessageView message;
    private final Map<String, String> params;
    private final String text;
    private final String body;
    private final String url;
    private final String title;
    private final String titleKey;
    private final String titleValue;
    private final String[] textList;
    private final Long messageId;

    public MiraiRequest(MiraiMessageView message, MiraiSessionService.MiraiSession session) {
        this.message = message;
        this.session = session;

        List<MessageChain> messageChain = message.getMessageChain();
        messageId = message.getMessageChain().get(0).getId();
        text = messageChain.stream().filter(StreamUtil.isEqual(MessageChain::getType, "Plain")).map(MessageChain::getText).filter(StringUtils::isNotBlank).collect(Collectors.joining("\n"));
        url = messageChain.stream().filter(StreamUtil.isEqual(MessageChain::getType, "Image")).map(MessageChain::getUrl).filter(StringUtils::isNotBlank).findFirst().orElse("");
        textList = text.split("\n");
        title = textList.length > 0? textList[0]: "";
        titleKey = title.split(" ")[0];
        titleValue = title.contains(" ")? title.substring(title.indexOf(" ") + 1): null;
        body = textList.length > 1? Stream.of(textList).skip(1).collect(Collectors.joining("\n")): "";

        String[] bodyList = body.split("\n");
        params = new HashMap<>();
        for (String line : bodyList) {
            String[] lineSplit = line.split("[=＝]");
            if (lineSplit.length != 2) {
                continue;
            }
            String key = lineSplit[0];
            String value = lineSplit[1];
            params.put(key.trim(), value.trim());
        }
    }
    
    public String getParam(String key) {
        return params.get(key);
    }

    public String getParamOrDefault(String key, String or) {
        return params.getOrDefault(key, or);
    }

    public MiraiMessageView getMessage() {
        return message;
    }

    public MiraiSessionService.MiraiSession getSession() {
        return session;
    }

    public String getText() {
        return text;
    }

    public String getBody() {
        return body;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String[] getTextList() {
        return textList;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public String getTitleValue() {
        return titleValue;
    }

    public String getTitleValueOrDefault(String defaultValue) {
        return titleValue == null ? defaultValue: titleValue;
    }

    public Long getMessageId() {
        return messageId;
    }
}
