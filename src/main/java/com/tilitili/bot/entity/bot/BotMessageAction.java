package com.tilitili.bot.entity.bot;

import com.tilitili.bot.service.BotSessionService;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.utils.StreamUtil;
import com.tilitili.common.utils.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.jsoup.helper.StringUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tilitili.common.utils.StringUtils.isNotBlank;

public class BotMessageAction {
    private final BotSessionService.MiraiSession session;
    private final BotMessage botMessage;
    private final List<String> imageList;
    private final List<Long> atList;
    private final String text;
    private final String body;
    private final String messageId;
    private final Map<String, String> paramMap;
    private final Map<String, String> bodyMap;
    private String keyWithoutPrefix;
    private String key;
    private String value;
    private BotSender botSender;
    private String quoteMessageId;

    public BotMessageAction(BotMessage botMessage, BotSessionService.MiraiSession session) {
        this.botMessage = botMessage;
        this.session = session;
        this.paramMap = new HashMap<>();
        this.bodyMap = new HashMap<>();
        this.messageId = botMessage.getMessageId();

        List<BotMessageChain> botMessageChainList = botMessage.getBotMessageChainList();
        this.imageList = botMessageChainList.stream().filter(StreamUtil.isEqual(BotMessageChain::getType, "Image")).map(BotMessageChain::getUrl).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        this.atList = botMessageChainList.stream().filter(StreamUtil.isEqual(BotMessageChain::getType, "At")).map(BotMessageChain::getTarget).filter(Objects::nonNull).collect(Collectors.toList());
        List<String> textList = botMessageChainList.stream().filter(StreamUtil.isEqual(BotMessageChain::getType, "Plain")).map(BotMessageChain::getText).filter(StringUtils::isNotBlank).collect(Collectors.toList());

        this.text = String.join("", textList).trim();
        this.body = text.contains("\n")? text.substring(text.indexOf("\n") + 1).trim(): null;
        List<String> lineList = body == null? Collections.emptyList(): Arrays.stream(body.split("\n")).filter(StringUtils::isNotBlank).map(String::trim).collect(Collectors.toList());
        String head = text.isEmpty()? null: text.contains("\n")? text.substring(0, text.indexOf("\n")): text;

        key = "";
        if (isNotBlank(head)) {
            if (head.contains(" ")) {
                int splitIndex = head.indexOf(" ");
                key = head.substring(0, splitIndex).trim();
                value = head.substring(splitIndex).trim();
            } else {
                key = head;
            }
            keyWithoutPrefix = Pattern.compile("^[.。]").matcher(key).find()? key.substring(1): key;
        }

        for (String line : lineList) {
            Matcher splitMatcher = Pattern.compile("[=＝]").matcher(line);
            if (splitMatcher.find()) {
                int splitIndex = splitMatcher.end();
                String key = line.substring(0, splitIndex - 1).trim();
                String value = line.substring(splitIndex).trim();
                paramMap.put(key, value);
            }
        }

        if (body != null) {
            String[] bodySplit = body.split("[=＝]");
            String key = bodySplit[0];
            for (int i = 1; i < bodySplit.length; i++) {
                String split = bodySplit[i];
                if (split.contains("\n")) {
                    int index = split.lastIndexOf("\n");
                    bodyMap.put(key, split.substring(0, index));
                    key = split.substring(index + 1);
                } else {
                    bodyMap.put(key, split);
                }
            }
        }

        List<BotMessageChain> quoteMessageList = botMessageChainList.stream().filter(StreamUtil.isEqual(BotMessageChain::getType, "Quote")).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(quoteMessageList)) {
            BotMessageChain quoteMessageChain = quoteMessageList.get(0);
            quoteMessageId = quoteMessageChain.getId();
        }
    }

    public String getParam(String paramKey) {
        return paramMap.get(paramKey);
    }

    public String getParamOrDefault(String paramKey, String defaultValue) {
        return paramMap.getOrDefault(paramKey, defaultValue);
    }

    public String getBody(String bodyKey) {
        return bodyMap.get(bodyKey);
    }

    public String getBodyOrDefault(String bodyKey, String defaultValue) {
        return bodyMap.getOrDefault(bodyKey, defaultValue);
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
        return StringUtil.isBlank(value)? defaultValue: value;
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

    public String getKeyWithoutPrefix() {
        return keyWithoutPrefix;
    }

    public List<Long> getAtList() {
        return atList;
    }

    public BotSender getBotSender() {
        return botSender;
    }

    public BotMessageAction setBotSender(BotSender botSender) {
        this.botSender = botSender;
        return this;
    }

    public String getQuoteMessageId() {
        return quoteMessageId;
    }
}
