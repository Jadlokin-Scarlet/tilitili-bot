package com.tilitili.bot.service;

import com.google.gson.Gson;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.PlayTwentyFourHandle;
import com.tilitili.bot.service.mirai.base.BaseMessageHandle;
import com.tilitili.common.emnus.ChannelEmum;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.BotMessageRecord;
import com.tilitili.common.entity.BotSendMessageRecord;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotTask;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotSenderManager;
import com.tilitili.common.mapper.mysql.BotSendMessageRecordMapper;
import com.tilitili.common.mapper.mysql.BotTaskMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StreamUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class BotService {
    @Value("${mirai.bot-qq}")
    private String BOT_QQ;
    private final Map<String, BaseMessageHandle> messageHandleMap;
    private final BotSessionService botSessionService;
    private final BotManager botManager;
    private final BotSenderManager botSenderManager;
    private final BotTaskMapper botTaskMapper;
    private final PlayTwentyFourHandle playTwentyFourHandle;
    private final BotSendMessageRecordMapper botSendMessageRecordMapper;
    private final Gson gson;

    public BotService(BotManager botManager, Map<String, BaseMessageHandle> messageHandleMap, BotSessionService botSessionService, BotSenderManager botSenderManager, BotTaskMapper botTaskMapper, PlayTwentyFourHandle playTwentyFourHandle, BotSendMessageRecordMapper botSendMessageRecordMapper) {
        this.botManager = botManager;
        this.messageHandleMap = messageHandleMap;
        this.botSessionService = botSessionService;
        this.botSenderManager = botSenderManager;
        this.botTaskMapper = botTaskMapper;
        this.playTwentyFourHandle = playTwentyFourHandle;
        this.botSendMessageRecordMapper = botSendMessageRecordMapper;
        gson = new Gson();
    }

    @Async
    public void syncHandleTextMessage(BotMessage botMessage) {
        String sendType = botMessage.getSendType();
        List<String> alwaysReplySendTypeList = Arrays.asList(SendTypeEmum.FRIEND_MESSAGE.sendType, SendTypeEmum.TEMP_MESSAGE.sendType);
        boolean alwaysReply = alwaysReplySendTypeList.contains(sendType);
        try {
            BotSessionService.MiraiSession session = botSessionService.getSession(getSessionKey(botMessage));
            BotMessageAction botMessageAction = new BotMessageAction(botMessage, session);
            String actionKey = botMessageAction.getKey();

            String prefix = "";
            if (Objects.equals(sendType, SendTypeEmum.GUILD_MESSAGE.sendType)) {
                prefix = ".";
                actionKey = actionKey.replaceAll("^[.。]", prefix);
            }

            BotSender botSender = botSenderManager.getSenderByBotMessage(botMessage);
            botMessageAction.setBotSender(botSender);

            List<BotTask> botTaskDTOList = botTaskMapper.getBotTaskListBySenderIdAndKeyOrNotKey(botSender.getId(), actionKey, prefix);

            // 尝试智能匹配key
            boolean isNoKey = botTaskDTOList.stream().noneMatch(StreamUtil.isEqual(BotTask::getSort, 0));
            if (isNoKey) {
                String key;
                // 尝试匹配回答24点
                key = playTwentyFourHandle.isThisTask(botMessageAction);
                if (key != null) {
                    botMessage.getBotMessageChainList().add(0, BotMessageChain.ofPlain(prefix + key + " "));
                    syncHandleTextMessage(botMessage);
                    return;
                }
            }

            String quoteMessageId = botMessageAction.getQuoteMessageId();
            Long quoteSenderId = botMessageAction.getQuoteSenderId();
            if (quoteMessageId != null) {
                if (Objects.equals(String.valueOf(quoteSenderId), BOT_QQ)) {
                    BotSendMessageRecord sendMessageRecord = botSendMessageRecordMapper.getNewBotSendMessageRecordByMessageId(quoteMessageId);
                    BotMessage quoteMessage = gson.fromJson(sendMessageRecord.getMessage(), BotMessage.class);
                    botMessageAction.setQuoteMessage(quoteMessage);
                } else {
                    BotMessageRecord quoteMessageRecord = botManager.getMessage(quoteMessageId);
                    BotMessage quoteMessage = botManager.handleMessageRecordToBotMessage(quoteMessageRecord);
                    botMessageAction.setQuoteMessage(quoteMessage);
                }
            }


            BotMessage respMessage = null;
            for (BotTask botTask : botTaskDTOList) {
                BaseMessageHandle messageHandle = messageHandleMap.get(botTask.getName());
                try {
                    // 返回null则代表跳过，继续寻找
                    // 返回空消息则代表已处理完毕但不回复，直接结束
                    respMessage = messageHandle.handleMessage(botMessageAction);
                } catch (AssertException e) {
                    log.debug(e.getMessage());
                    respMessage = messageHandle.handleAssertException(botMessageAction, e);
                }
                if (respMessage != null) {
                    break;
                }
            }

            // 如果最后为null，则标志无匹配处理器，则回复表情包
            Asserts.notNull(respMessage, "无回复");
            // 如果最后是空消息，则表示匹配到处理器并处理完毕但不需要回复
            if (CollectionUtils.isEmpty(respMessage.getBotMessageChainList())) {
                return;
            }
            // 如果最后是消息，则回复
            if (respMessage.getSendType() == null) {
                respMessage.setSendType(sendType);
                respMessage.setQq(botMessage.getQq());
                respMessage.setGroup(botMessage.getGroup());
                respMessage.setGuildId(botMessage.getGuildId());
                respMessage.setChannelId(botMessage.getChannelId());
            }

            botManager.sendMessage(respMessage);
        } catch (AssertException e) {
            log.debug("异步消息处理断言异常, message={}", e.getMessage());
            if (alwaysReply) {
                botManager.sendMessage(BotMessage.simpleImageMessage("http://m.qpic.cn/psc?/V53UUlnk2IehYn4WcXfY2dBFO92OvB1L/TmEUgtj9EK6.7V8ajmQrEPBYbjL66rmGmhZeULQk5K23cRElRpiBGW67YBgbgQxSQQ*jZ1sT2lB3FSogwc0t5DyuSeiAT17yAwmaSTNULPo!/b&bo=aABPAAAAAAABFxc!&rf=viewer_4", botMessage));
            }
        } catch (Exception e) {
            log.error("异步消息处理异常", e);
            if (alwaysReply) {
                botManager.sendMessage(BotMessage.simpleImageMessage("http://m.qpic.cn/psc?/V53UUlnk2IehYn4WcXfY2dBFO92OvB1L/TmEUgtj9EK6.7V8ajmQrENdFC7iq*X8AsvjACl.g*DjfOPu0Ohw4r47052XDpNQGtOBy0dw5ZNtRggzAZvOvUBGBlTjwCDv4o3k*J7IWang!/b&bo=eABcAAAAAAABFxQ!&rf=viewer_4", botMessage));
            }
        }
    }

    public String getSessionKey(BotMessage botMessage) {
        String sendType = botMessage.getSendType();
        switch (sendType) {
            case SendTypeEmum.TEMP_MESSAGE_STR: Asserts.notNull(botMessage.getGroup(), "找不到发送对象");
            case SendTypeEmum.FRIEND_MESSAGE_STR: Asserts.notNull(botMessage.getQq(), "找不到发送对象"); return SendTypeEmum.FRIEND_MESSAGE_STR + "-" + botMessage.getQq();
            case SendTypeEmum.GROUP_MESSAGE_STR: Asserts.notNull(botMessage.getGroup(), "找不到发送对象"); return SendTypeEmum.GROUP_MESSAGE_STR + "-" + botMessage.getGroup();
            case SendTypeEmum.GUILD_MESSAGE_STR: {
                ChannelEmum channel = botMessage.getChannel();
                Long guildId = channel != null? channel.guildId: botMessage.getGuildId();
                Long channelId = channel != null? channel.channelId: botMessage.getChannelId();
                Asserts.notNull(guildId, "找不到发送对象");
                Asserts.notNull(channelId, "找不到发送对象");
                return SendTypeEmum.GUILD_MESSAGE_STR + "-" + guildId + "-" + channelId;
            }
            default: throw new AssertException("未知发送类型："+sendType);
        }
    }

}
