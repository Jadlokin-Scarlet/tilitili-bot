package com.tilitili.bot.service;

import com.google.gson.Gson;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.BaseEventHandle;
import com.tilitili.bot.service.mirai.base.BaseMessageHandle;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandleAdapt;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.*;
import com.tilitili.common.entity.query.BotTaskQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.*;
import com.tilitili.common.mapper.mysql.BotSendMessageRecordMapper;
import com.tilitili.common.mapper.mysql.BotTaskMapper;
import com.tilitili.common.mapper.mysql.BotUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StreamUtil;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class BotService {
    private final Map<String, BaseMessageHandle> messageHandleMap;
    private final Map<String, BaseEventHandle> eventHandleMap;
    private final BotSessionService botSessionService;
    private final BotManager botManager;
    private final BotTaskMapper botTaskMapper;
    private final BotSendMessageRecordMapper botSendMessageRecordMapper;
    private final BotUserManager botUserManager;
    private final Gson gson;
    private final BotUserMapper botUserMapper;
    private final BotMessageRecordManager botMessageRecordManager;
    private final BotSenderManager botSenderManager;
    private final MiraiManager miraiManager;
    private final GoCqhttpManager goCqhttpManager;
    private final ConcurrentHashMap<Long, Boolean> userIdLockMap = new ConcurrentHashMap<>();

    public BotService(BotManager botManager, Map<String, BaseMessageHandle> messageHandleMap, Map<String, BaseEventHandle> eventHandleMap, BotSessionService botSessionService, BotTaskMapper botTaskMapper, BotSendMessageRecordMapper botSendMessageRecordMapper, BotUserManager botUserManager, BotUserMapper botUserMapper, BotMessageRecordManager botMessageRecordManager, BotSenderManager botSenderManager, MiraiManager miraiManager, GoCqhttpManager goCqhttpManager) {
        this.botManager = botManager;
        this.messageHandleMap = messageHandleMap;
        this.eventHandleMap = eventHandleMap;
        this.botSessionService = botSessionService;
        this.botTaskMapper = botTaskMapper;
        this.botSendMessageRecordMapper = botSendMessageRecordMapper;
        this.botUserManager = botUserManager;
        this.botUserMapper = botUserMapper;
        this.botMessageRecordManager = botMessageRecordManager;
        this.botSenderManager = botSenderManager;
        this.miraiManager = miraiManager;
        this.goCqhttpManager = goCqhttpManager;
        gson = new Gson();
    }

    @Async
    public void syncHandleEvent(String message) {
        try {
            String eventType = StringUtils.patten1("\"type\":\"(\\w+)\"", message);
            log.debug("eventType=" + eventType);
            Asserts.notBlank(eventType, "");
            String handleName = eventType.substring(0, 1).toLowerCase() + eventType.substring(1);
            Asserts.isTrue(eventHandleMap.containsKey(handleName), "未定义的事件");
            BaseEventHandle messageHandle = eventHandleMap.get(handleName);
            messageHandle.handleEventStr(message);
        } catch (AssertException e) {
            log.warn(e.getMessage(), e);
        } catch (Exception e) {
            log.error("处理事件异常", e);
        }
    }

    @Async
    public void syncHandleTextMessage(String message, BotEmum botEmum) {
        BotMessage botMessage = null;
        // 是否总是回复消息
        boolean alwaysReply = false;
        try {
            // 解析message
            botMessage = this.getBotMessageFromStr(message, botEmum);
            if (botMessage == null) return;
            botMessageRecordManager.asyncLogRecord(message, botMessage);
            alwaysReply = SendTypeEmum.FRIEND_MESSAGE.sendType.equals(botMessage.getSendType());
            Long externalId = botMessage.getExternalId();

            // 获取sender
            BotSender botSender = botSenderManager.getSenderByBotMessage(botMessage);
            if (!Objects.equals(botSender.getBot(), botEmum.value)) return;

            // 获取用户锁，并保存user消息
//            userIdLockMap.putIfAbsent()
//            if (!userIdLockMap.putIfAbsent(externalId, Boolean.TRUE)) {
//
//            }
            Asserts.checkNull(userIdLockMap.putIfAbsent(externalId, true), "听我说你先别急。");
            BotUser botUser = this.updateBotUser(botMessage);
            // 获取session
            BotSessionService.MiraiSession session = botSessionService.getSession(getSessionKey(botMessage));
            // 解析指令
            BotMessageAction botMessageAction = new BotMessageAction(botMessage, session, botSender, botUser);
            // 查询匹配任务列表
            List<BotTask> botTaskDTOList = this.queryBotTasks(botMessageAction);
            // 查询回复消息
            botMessageAction.setQuoteMessage(this.queryQuoteMessage(botSender, botMessageAction));

            // 匹配并执行指令
            BotMessage respMessage = null;
            for (BotTask botTask : botTaskDTOList) {
                BaseMessageHandle messageHandle = messageHandleMap.get(botTask.getName());
                try {
                    // 返回null则代表跳过，继续寻找
                    // 返回空消息则代表已处理完毕但不回复，直接结束
                    respMessage = messageHandle.handleMessage(botMessageAction);
                } catch (AssertException e) {
                    log.debug(e.getMessage(), e);
                    if (messageHandle instanceof ExceptionRespMessageHandleAdapt) {
                        respMessage = ((ExceptionRespMessageHandleAdapt)messageHandle).handleAssertException(botMessageAction, e);
                    }
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
                respMessage.setSendType(botMessage.getSendType());
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
        } finally {
            if (botMessage != null) {
                userIdLockMap.remove(botMessage.getExternalId());
            }
        }
    }

    private BotMessage getBotMessageFromStr(String message, BotEmum botEmum) {
        if (botEmum.type.equals(BotEmum.TYPE_MIRAI)) {
            return miraiManager.handleMiraiWsMessageToBotMessage(message);
        } else {
            if (message.contains("post_type\":\"meta_event")) return null;
            if (message.contains("post_type\":\"notice")) return null;
            return goCqhttpManager.handleGoCqhttpWsMessageToBotMessage(message);
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
        BotUser botUser = botUserMapper.getBotUserByExternalId(botMessage.getExternalId());
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
