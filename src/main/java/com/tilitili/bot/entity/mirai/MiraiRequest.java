package com.tilitili.bot.entity.mirai;

import com.tilitili.bot.service.BotSessionService;
import com.tilitili.common.entity.view.bot.gocqhttp.GoCqhttpWsMessage;
import com.tilitili.common.entity.view.bot.mirai.MessageChain;
import com.tilitili.common.entity.view.bot.mirai.MiraiMessageView;
import com.tilitili.common.utils.StreamUtil;
import com.tilitili.common.utils.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MiraiRequest {
    private final BotSessionService.MiraiSession session;
    private final GoCqhttpWsMessage goCqhttpWsMessage;
    private final MiraiMessageView miraiMessageView;
    private final String messageId;
    private final String text;
    private final Map<String, String> params;
    private final String body;
    private final String url;
    private final String title;
    private final String titleKey;
    private final String titleValue;
    private final String[] textList;

    public MiraiRequest(GoCqhttpWsMessage wsMessage, BotSessionService.MiraiSession session) {
        this.miraiMessageView = null;
        this.goCqhttpWsMessage = wsMessage;
        this.session = session;

        messageId = wsMessage.getMessageId();
        text = wsMessage.getMessage();

        // \[CQ:([a-zA-Z0-9-_.]+),([^\[\]]+)\]
        // 图片CQ码
        Matcher matcher = Pattern.compile("\\[CQ:image,([^\\[\\]]+)]").matcher(text);
        if (matcher.find()) {
            String paramsStr = matcher.group();
            String urlParamStr = Arrays.stream(paramsStr.split(",")).filter(param -> param.startsWith("url")).findFirst().orElse(null);
            if (urlParamStr != null && urlParamStr.contains("=")) {
                this.url = urlParamStr.split("=")[1];
            } else {
                this.url = null;
            }
        } else {
            this.url = null;
        }


        this.params = new HashMap<>();
        this.body = null;

        this.title = text.replaceAll("\\[CQ:[^\\[\\]]+]", "").trim();
        this.titleKey = title.split(" +")[0].trim();
        this.titleValue = title.contains(" ")? title.split(" +")[1].trim(): null;
        this.textList = null;
    }

    public MiraiRequest(MiraiMessageView message, BotSessionService.MiraiSession session) {
        this.miraiMessageView = message;
        this.goCqhttpWsMessage = null;
        this.session = session;

        List<MessageChain> messageChain = message.getMessageChain();
        messageId = String.valueOf(message.getMessageChain().get(0).getId());
        text = messageChain.stream().filter(StreamUtil.isEqual(MessageChain::getType, "Plain")).map(MessageChain::getText).filter(StringUtils::isNotBlank).collect(Collectors.joining("\n"));
        url = messageChain.stream().filter(StreamUtil.isEqual(MessageChain::getType, "Image")).map(MessageChain::getUrl).filter(StringUtils::isNotBlank).findFirst().orElse("");
        textList = text.split("\n");
        title = textList.length > 0? textList[0].trim(): "";
        titleKey = title.split(" +")[0].trim();
        titleValue = title.contains(" ")? title.split(" +")[1].trim(): null;
        body = textList.length > 1? Stream.of(textList).skip(1).collect(Collectors.joining("\n")): "";

        String[] bodyList = body.split("\n");
        params = new HashMap<>();
        for (String line : bodyList) {
            String[] lineSplit = line.split("[=＝]");
            if (lineSplit.length != 2) {
                continue;
            }
            String key = lineSplit[0].trim();
            String value = lineSplit[1].trim();
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
        return miraiMessageView;
    }

    public BotSessionService.MiraiSession getSession() {
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

    public String getMessageId() {
        return messageId;
    }

    public GoCqhttpWsMessage getGoCqhttpWsMessage() {
        return goCqhttpWsMessage;
    }
}
