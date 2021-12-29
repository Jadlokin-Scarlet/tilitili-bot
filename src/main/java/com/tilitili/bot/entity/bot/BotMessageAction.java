package com.tilitili.bot.entity.bot;

import com.tilitili.bot.service.BotSessionService;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.utils.StreamUtil;
import com.tilitili.common.utils.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tilitili.common.utils.StringUtils.isNotBlank;

public class BotMessageAction {
    private final BotSessionService.MiraiSession session;
    private final BotMessage botMessage;
    private final List<String> imageList;
    private final String text;
    private final String body;
    private final String messageId;
    private final Map<String, String> paramMap;
    private String key;
    private String value;

    public BotMessageAction(BotMessage botMessage, BotSessionService.MiraiSession session) {
        this.botMessage = botMessage;
        this.session = session;
        this.paramMap = new HashMap<>();
        this.messageId = botMessage.getMessageId();

        List<BotMessageChain> botMessageChainList = botMessage.getBotMessageChainList();
        this.imageList = botMessageChainList.stream().filter(StreamUtil.isEqual(BotMessageChain::getType, "Image")).map(BotMessageChain::getUrl).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        List<String> textList = botMessageChainList.stream().filter(StreamUtil.isEqual(BotMessageChain::getType, "Plain")).map(BotMessageChain::getText).filter(StringUtils::isNotBlank).collect(Collectors.toList());

        this.text = String.join("", textList).trim();
        this.body = text.contains("\n")? text.substring(text.indexOf("\n")).trim(): null;
        List<String> lineList = Arrays.stream(text.split("\n")).filter(StringUtils::isNotBlank).map(String::trim).collect(Collectors.toList());
        String head = lineList.isEmpty()? null: lineList.get(0);

        if (isNotBlank(head)) {
            if (head.contains(" ")) {
                int splitIndex = head.indexOf(" ");
                key = head.substring(0, splitIndex).trim();
                value = head.substring(splitIndex).trim();
            } else {
                key = head;
            }
        }

        if (lineList.size() > 1) {
            for (String line : lineList.subList(1, lineList.size())) {
                Matcher splitMatcher = Pattern.compile("[=＝]").matcher(line);
                if (splitMatcher.find()) {
                    int splitIndex = splitMatcher.end();
                    String key = line.substring(0, splitIndex - 1).trim();
                    String value = line.substring(splitIndex).trim();
                    paramMap.put(key, value);
                }
            }
        }
    }

    public String getParam(String paramKey) {
        return paramMap.get(paramKey);
    }

    public String getParamOrDefault(String paramKey, String defaultValue) {
        return paramMap.getOrDefault(paramKey, defaultValue);
    }

    public BotMessage getBotMessage() {
        return botMessage;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getValueOrDefault(String defaultValue) {
        return value == null? defaultValue: value;
    }

    public BotSessionService.MiraiSession getSession() {
        return session;
    }

    public List<String> getImageList() {
        return imageList;
    }

    public String getBody() {
        return body;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getText() {
        return text;
    }

}
