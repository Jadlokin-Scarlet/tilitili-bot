package com.tilitili.bot.service;

import com.google.common.base.CaseFormat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.BaseEventHandle;
import com.tilitili.bot.service.mirai.base.BaseMessageHandle;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandleAdapt;
import com.tilitili.common.constant.BotTaskConstant;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.*;
import com.tilitili.common.entity.query.BotTaskQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.gocqhttp.GocqhttpBaseEvent;
import com.tilitili.common.entity.view.bot.kook.*;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.*;
import com.tilitili.common.mapper.mysql.BotMessageRecordMapper;
import com.tilitili.common.mapper.mysql.BotSendMessageRecordMapper;
import com.tilitili.common.mapper.mysql.BotTaskMapper;
import com.tilitili.common.mapper.mysql.BotUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
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
    public static final String lastMessageIdKey = "lastMessageId";

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
    private final BotSenderTaskMappingManager botSenderTaskMappingManager;
    private final KookManager kookManager;
    private final BotMessageRecordMapper botMessageRecordMapper;
    private final ConcurrentHashMap<Long, Boolean> userIdLockMap = new ConcurrentHashMap<>();

    public BotService(BotManager botManager, Map<String, BaseMessageHandle> messageHandleMap, Map<String, BaseEventHandle> eventHandleMap, BotSessionService botSessionService, BotTaskMapper botTaskMapper, BotSendMessageRecordMapper botSendMessageRecordMapper, BotUserManager botUserManager, BotUserMapper botUserMapper, BotMessageRecordManager botMessageRecordManager, BotSenderManager botSenderManager, MiraiManager miraiManager, GoCqhttpManager goCqhttpManager, BotSenderTaskMappingManager botSenderTaskMappingManager, KookManager kookManager, BotMessageRecordMapper botMessageRecordMapper) {
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
        this.botSenderTaskMappingManager = botSenderTaskMappingManager;
        this.kookManager = kookManager;
        this.botMessageRecordMapper = botMessageRecordMapper;
        gson = new Gson();
    }

    @Async
    public void syncHandleEvent(BotEmum bot, String message) {
        try {
            String handleName;
            if (BotEmum.TYPE_MIRAI.equals(bot.getType())) {
                String eventType = StringUtils.patten1("\"type\":\"(\\w+)\"", message);
                Asserts.notBlank(eventType, "获取事件类型失败");
                handleName = "mirai" + eventType + "Handle";
            } else if (BotEmum.TYPE_GOCQ.equals(bot.getType())) {
                GocqhttpBaseEvent baseEvent = Gsons.fromJson(message, GocqhttpBaseEvent.class);
                String postType = baseEvent.getPostType();
                String noticeType = baseEvent.getNoticeType();
                String subType = baseEvent.getSubType();
                Asserts.notNull(postType, "啊嘞，不对劲");
                postType = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, postType);
                noticeType = noticeType == null? "": CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, noticeType);
                subType = subType == null? "": CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, subType);
                handleName = "gocq" + postType + noticeType + subType + "Handle";
            } else if (BotEmum.TYPE_KOOK.equals(bot.getType())) {
                KookWsEvent<?> data = Gsons.fromJson(message, new TypeToken<KookWsEvent<?>>() {}.getType());
                KookEventExtra<?> extra = data.getD().getExtra();
                String eventType = extra.getType();
                handleName = "kook" + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, eventType) + "Handle";
            } else {
                throw new AssertException();
            }
            Asserts.isTrue(eventHandleMap.containsKey(handleName), "未定义的事件=%s", handleName);
            BaseEventHandle messageHandle = eventHandleMap.get(handleName);
            messageHandle.handleEventStr(bot, message);
        } catch (AssertException e) {
            log.warn(e.getMessage(), e);
        } catch (Exception e) {
            log.error("处理事件异常", e);
        }
    }

    @Async
    public void syncHandleTextMessage(String message, BotEmum bot) {
        BotMessage botMessage = null;
        try {
            // 解析message
            botMessage = botManager.handleWsMessageToBotMessage(bot, message);
            if (botMessage == null) return;
            // 获取sender，校验权限
            BotSender botSender = botMessage.getBotSender();
            // 校验权限
            boolean hasHelp = botSenderTaskMappingManager.checkSenderHasTaskCache(botSender.getId(), BotTaskConstant.helpTaskId);
            if (!hasHelp) {
                log.info(botMessage.getMessageId() + "无权限");
                return;
            }
            // 获取session
            BotSessionService.MiraiSession session = botSessionService.getSession(botSender.getId());
            // 消息记录
            botMessageRecordManager.asyncLogRecord(message, botMessage);

            // 获取用户锁
            Asserts.checkNull(userIdLockMap.putIfAbsent(botMessage.getBotUser().getId(), true), "听我说你先别急。");
            // 解析指令
            BotMessageAction botMessageAction = new BotMessageAction(botMessage, session, bot);
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
                    // 如果继承了实现了全局异常处理的基类，则调用异常处理方法
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
                log.info(botMessage.getMessageId() + "无回复");
                return;
            }
            // 如果最后是空消息，则表示匹配到处理器并处理完毕但不需要回复
            if (CollectionUtils.isEmpty(respMessage.getBotMessageChainList())) {
                return;
            }
            // 没设置发送者，就默认原路发回
            if (respMessage.getBotSender() == null) {
                respMessage.setBotSender(botSender);
            }
            // 如果最后是消息，则回复
            String messageId = botManager.sendMessage(respMessage);
            if (messageId != null) {
                session.put(lastMessageIdKey, messageId);
            }
        } catch (AssertException e) {
            log.warn("异步消息处理断言异常, message=" + e.getMessage(), e);
        } catch (Exception e) {
            log.error("异步消息处理异常", e);
        } finally {
            if (botMessage != null && botMessage.getBotUser() != null && botMessage.getBotUser().getId() != null) {
                userIdLockMap.remove(botMessage.getBotUser().getId());
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
                if (messageHandle != null) {
                    // 尝试匹配回答24点
                    key = messageHandle.isThisTask(botMessageAction);
                    if (key != null) {
                        botMessageAction.setVirtualKey(key);
                        botTaskDTOList.add(0, botTask);
                    }
                }
            }
        }
        return botTaskDTOList;
    }

    private BotMessage queryQuoteMessage(BotSender botSender, BotMessageAction botMessageAction) {
        String quoteMessageId = botMessageAction.getQuoteMessageId();
        Long quoteSenderId = botMessageAction.getQuoteSenderId();
        if (quoteMessageId == null) return null;
        BotEmum bot = BotEmum.getBotById(botSender.getBot());
        if (bot == null) return null;

        if (Objects.equals(quoteSenderId, bot.qq)) {
            BotSendMessageRecord sendMessageRecord = botSendMessageRecordMapper.getNewBotSendMessageRecordByMessageId(quoteMessageId);
            if (sendMessageRecord != null) {
                return gson.fromJson(sendMessageRecord.getMessage(), BotMessage.class);
            }
        }

        BotMessageRecord quoteMessageRecord = botMessageRecordMapper.getBotMessageRecordByMessageId(quoteMessageId);
        if (quoteMessageRecord != null) {
            return botManager.handleMessageRecordToBotMessage(quoteMessageRecord);
        }
        log.warn("messageId={}找不到", quoteMessageId);
        return null;
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
