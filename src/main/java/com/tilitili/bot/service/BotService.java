package com.tilitili.bot.service;

import com.google.gson.Gson;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.BaseMessageHandle;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.*;
import com.tilitili.common.entity.query.BotTaskQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotSendMessageRecordMapper;
import com.tilitili.common.mapper.mysql.BotTaskMapper;
import com.tilitili.common.mapper.mysql.BotUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StreamUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class BotService {
    private final Map<String, BaseMessageHandle> messageHandleMap;
    private final BotSessionService botSessionService;
    private final BotManager botManager;
    private final BotTaskMapper botTaskMapper;
    private final BotSendMessageRecordMapper botSendMessageRecordMapper;
    private final BotUserManager botUserManager;
    private final Gson gson;
    private final BotUserMapper botUserMapper;

    public BotService(BotManager botManager, Map<String, BaseMessageHandle> messageHandleMap, BotSessionService botSessionService, BotTaskMapper botTaskMapper, BotSendMessageRecordMapper botSendMessageRecordMapper, BotUserManager botUserManager, BotUserMapper botUserMapper) {
        this.botManager = botManager;
        this.messageHandleMap = messageHandleMap;
        this.botSessionService = botSessionService;
        this.botTaskMapper = botTaskMapper;
        this.botSendMessageRecordMapper = botSendMessageRecordMapper;
        this.botUserManager = botUserManager;
        this.botUserMapper = botUserMapper;
        gson = new Gson();
    }

    @Async
    public void syncHandleTextMessage(BotMessage botMessage, BotSender botSender) {
        String sendType = botMessage.getSendType();
        List<String> alwaysReplySendTypeList = Arrays.asList(SendTypeEmum.FRIEND_MESSAGE.sendType);
        boolean alwaysReply = alwaysReplySendTypeList.contains(sendType);
        try {
            // 保存user消息
            BotUser botUser = this.updateBotUser(botMessage);
            // 获取session
            BotSessionService.MiraiSession session = botSessionService.getSession(getSessionKey(botMessage));
            // 解析指令
            BotMessageAction botMessageAction = new BotMessageAction(botMessage, session, botSender, botUser);
            // 查询匹配任务列表
            List<BotTask> botTaskDTOList = this.queryBotTasks(botMessageAction);
            // 查询回复消息
            botMessageAction.setQuoteMessage(this.queryQuoteMessage(botSender, botMessageAction));


            BotMessage respMessage = null;
            for (BotTask botTask : botTaskDTOList) {
                BaseMessageHandle messageHandle = messageHandleMap.get(botTask.getName());
                try {
                    // 返回null则代表跳过，继续寻找
                    // 返回空消息则代表已处理完毕但不回复，直接结束
                    respMessage = messageHandle.handleMessage(botMessageAction);
                } catch (AssertException e) {
                    log.debug(e.getMessage(), e);
                    respMessage = messageHandle.handleAssertException(botMessageAction, e);
                }
                if (respMessage != null) {
                    break;
                }
            }

            // 如果最后为null，则标志无匹配处理器，则回复表情包
            if (respMessage == null) {
                log.info("无回复");
                return;
            }
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
            log.debug("异步消息处理断言异常, message=" + e.getMessage(), e);
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

    private List<BotTask> queryBotTasks(BotMessageAction botMessageAction) {
        List<BotTask> botTaskDTOList = this.getBotTalkDtoList(botMessageAction);

        // 尝试智能匹配key
        boolean isNoKey = botTaskDTOList.stream().noneMatch(StreamUtil.isEqual(BotTask::getSort, 0));
        if (isNoKey) {
            String key;
            List<BotTask> allBotTask = botTaskMapper.getBotTaskByCondition(new BotTaskQuery().setStatus(0));
            for (BotTask botTask : allBotTask) {
                BaseMessageHandle messageHandle = messageHandleMap.get(botTask.getName());
                // 尝试匹配回答24点
                key = messageHandle.isThisTask(botMessageAction);
                if (key != null) {
                    botMessageAction.setVirtualKey(key);
                    botTaskDTOList.add(0, botTask);
                }
            }
        }
        return botTaskDTOList;
    }

    private BotMessage queryQuoteMessage(BotSender botSender, BotMessageAction botMessageAction) {
        String quoteMessageId = botMessageAction.getQuoteMessageId();
        Long quoteSenderId = botMessageAction.getQuoteSenderId();
        if (quoteMessageId == null) return null;
        if (Objects.equals(quoteSenderId, botSender.getBot())) {
            BotSendMessageRecord sendMessageRecord = botSendMessageRecordMapper.getNewBotSendMessageRecordByMessageId(quoteMessageId);
            return gson.fromJson(sendMessageRecord.getMessage(), BotMessage.class);
        } else {
            BotMessageRecord quoteMessageRecord = botManager.getMessage(quoteMessageId);
            BotMessage quoteMessage = null;
            if (quoteMessageRecord != null) {
                quoteMessage = botManager.handleMessageRecordToBotMessage(quoteMessageRecord);
            } else {
                BotSendMessageRecord sendMessageRecord = botSendMessageRecordMapper.getNewBotSendMessageRecordByMessageId(quoteMessageId);
                if (sendMessageRecord != null) {
                    quoteMessage = gson.fromJson(sendMessageRecord.getMessage(), BotMessage.class);
                }
            }
            return quoteMessage;
        }
    }

    private BotUser updateBotUser(BotMessage botMessage) {
        BotUser botUser = botUserMapper.getBotUserByExternalId(SendTypeEmum.GUILD_MESSAGE_STR.equals(botMessage.getSendType())? botMessage.getTinyId(): botMessage.getQq());
        if (botUser != null) {
            botUserMapper.updateBotUserSelective(new BotUser().setId(botUser.getId()).setName(botMessage.getGroupNickName()));
        } else {
            botUser = botUserManager.convertBotUserByBotMessage(botMessage);
            botUserMapper.addBotUserSelective(botUser);
        }
        return botUser;
    }

    public String getSessionKey(BotMessage botMessage) {
        String sendType = botMessage.getSendType();
        switch (sendType) {
            case SendTypeEmum.TEMP_MESSAGE_STR: Asserts.notNull(botMessage.getGroup(), "找不到发送对象");
            case SendTypeEmum.FRIEND_MESSAGE_STR: Asserts.notNull(botMessage.getQq(), "找不到发送对象"); return SendTypeEmum.FRIEND_MESSAGE_STR + "-" + botMessage.getQq();
            case SendTypeEmum.GROUP_MESSAGE_STR: Asserts.notNull(botMessage.getGroup(), "找不到发送对象"); return SendTypeEmum.GROUP_MESSAGE_STR + "-" + botMessage.getGroup();
            case SendTypeEmum.GUILD_MESSAGE_STR: {
                Long guildId = botMessage.getGuildId();
                Long channelId = botMessage.getChannelId();
                Asserts.notNull(guildId, "找不到发送对象");
                Asserts.notNull(channelId, "找不到发送对象");
                return SendTypeEmum.GUILD_MESSAGE_STR + "-" + guildId + "-" + channelId;
            }
            default: throw new AssertException("未知发送类型："+sendType);
        }
    }

    private List<BotTask> getBotTalkDtoList(BotMessageAction botMessageAction) {
        String actionKey = botMessageAction.getKey();
        BotSender botSender = botMessageAction.getBotSender();
        String sendType = botSender.getSendType();

        String prefix = "";
        if (Objects.equals(sendType, SendTypeEmum.GUILD_MESSAGE.sendType)) {
            prefix = ".";
            actionKey = actionKey.replaceAll("^[.。]", prefix);
        }

        return botTaskMapper.getBotTaskListBySenderIdAndKeyOrNotKey(botSender.getId(), actionKey, prefix);
    }

}
