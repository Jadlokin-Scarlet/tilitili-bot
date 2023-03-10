package com.tilitili.bot.service;

import com.google.gson.Gson;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.BaseEventHandle;
import com.tilitili.bot.service.mirai.base.BaseMessageHandle;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandleAdapt;
import com.tilitili.common.constant.BotTaskConstant;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.emnus.SendTypeEnum;
import com.tilitili.common.entity.BotMessageRecord;
import com.tilitili.common.entity.BotSendMessageRecord;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotTask;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotTaskQuery;
import com.tilitili.common.entity.view.bot.BotEvent;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.*;
import com.tilitili.common.mapper.mysql.BotMessageRecordMapper;
import com.tilitili.common.mapper.mysql.BotSendMessageRecordMapper;
import com.tilitili.common.mapper.mysql.BotTaskMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StreamUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BotService {
    public static final String lastMessageIdKey = "lastMessageId";
    private final SendMessageManager sendMessageManager;
    private final Map<String, BaseMessageHandle> messageHandleMap;
    private final Map<String, BaseEventHandle> eventHandleMap;
    private final BotSessionService botSessionService;
    private final BotManager botManager;
    private final BotTaskMapper botTaskMapper;
    private final Gson gson;
    private final BotMessageRecordManager botMessageRecordManager;
    private final BotSenderTaskMappingManager botSenderTaskMappingManager;
    private final BotMessageRecordMapper botMessageRecordMapper;
    private final BotSendMessageRecordMapper botSendMessageRecordMapper;
    private final ConcurrentHashMap<Long, Boolean> userIdLockMap = new ConcurrentHashMap<>();

    public BotService(SendMessageManager sendMessageManager, BotManager botManager, Map<String, BaseMessageHandle> messageHandleMap, List<BaseEventHandle> eventHandleList, BotSessionService botSessionService, BotTaskMapper botTaskMapper, BotSendMessageRecordMapper botSendMessageRecordMapper, BotMessageRecordManager botMessageRecordManager, BotSenderTaskMappingManager botSenderTaskMappingManager, BotMessageRecordMapper botMessageRecordMapper) {
        this.sendMessageManager = sendMessageManager;
        this.botManager = botManager;
        this.messageHandleMap = messageHandleMap;
        this.botSessionService = botSessionService;
        this.botTaskMapper = botTaskMapper;
        this.botMessageRecordManager = botMessageRecordManager;
        this.botMessageRecordMapper = botMessageRecordMapper;
        this.botSendMessageRecordMapper = botSendMessageRecordMapper;
        this.botSenderTaskMappingManager = botSenderTaskMappingManager;
        gson = new Gson();

        this.eventHandleMap = eventHandleList.stream().collect(Collectors.toMap(BaseEventHandle::getEventType, Function.identity()));
    }

    public void testHandleMessage(BotEnum bot, String message) {
        this.syncHandleMessage(bot, message);
    }

    @Async
    public void syncHandleMessage(BotEnum bot, String message) {
        // 解析message
        BotMessage botMessage = null;
        try {
            botMessage = botManager.handleMessageToBotMessage(bot, message);
        } catch (Exception e) {
            log.warn("解析失败", e);
        }
        if (botMessage == null) {
            log.warn("解析失败");
            return;
        }
        if (botMessage.getBotMessageChainList() != null) {
            this.syncHandleChatMessage(bot, botMessage);
        } else if (botMessage.getBotEvent() != null) {
            this.syncHandleEventMessage(bot, botMessage);
        }
    }

    private void syncHandleChatMessage(BotEnum bot, BotMessage botMessage) {
        List<Long> lockUserId = new ArrayList<>();
        try {
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
            botMessageRecordManager.logRecord(botMessage);

            // 获取用户锁
            Asserts.checkNull(userIdLockMap.putIfAbsent(botMessage.getBotUser().getId(), true), "听我说你先别急。");
            lockUserId.add(botMessage.getBotUser().getId());
            // 解析指令
            BotMessageAction botMessageAction = new BotMessageAction(botMessage, session, bot);
            for (BotUserDTO atUser : botMessageAction.getAtList()) {
                Long atUserId = atUser.getId();
                if (Objects.equals(atUserId, botMessage.getBotUser().getId())) continue;
                Asserts.checkNull(userIdLockMap.putIfAbsent(atUserId, true), "听我说你先别急。");
                lockUserId.add(atUserId);
            }
            // 查询匹配任务列表
            List<BotTask> botTaskDTOList = this.queryBotTasks(botMessageAction);
            // 查询回复消息
            botMessageAction.setQuoteMessage(this.queryQuoteMessage(botSender, botMessageAction));

            // 匹配并执行指令
            List<BotMessage> respMessage = null;
            for (BotTask botTask : botTaskDTOList) {
                BaseMessageHandle messageHandle = messageHandleMap.get(botTask.getName());
                try {
                    // 返回null则代表跳过，继续寻找
                    // 返回空消息则代表已处理完毕但不回复，直接结束
                    respMessage = messageHandle.handleMessageNew(botMessageAction);
                } catch (AssertException e) {
                    log.debug(e.getMessage(), e);
                    // 如果继承了实现了全局异常处理的基类，则调用异常处理方法
                    if (messageHandle instanceof ExceptionRespMessageHandleAdapt) {
                        respMessage = Collections.singletonList(((ExceptionRespMessageHandleAdapt)messageHandle).handleAssertException(botMessageAction, e));
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
            if (CollectionUtils.isEmpty(respMessage)) {
                log.info(botMessage.getMessageId() + "已处理");
                return;
            }
            for (BotMessage message : respMessage) {
                // 没设置发送者，就默认原路发回
                if (message.getBotSender() == null) {
                    message.setBotSender(botSender);
                }
                // 记录消息和回复消息的关系
                message.setMessageId(botMessage.getMessageId());
            }
            // 如果最后是消息，则回复
            sendMessageManager.sendMessage(respMessage);
//            if (messageId != null) {
//                Long respSenderId = respMessage.getBotSender() != null ? respMessage.getBotSender().getId() : respMessage.getSenderId();
//                if (respSenderId != null) {
//                    BotSessionService.MiraiSession respSession = botSessionService.getSession(respSenderId);
//                    respSession.put(lastMessageIdKey, messageId);
//                }
//            }
        } catch (AssertException e) {
            log.warn("异步消息处理断言异常, message=" + e.getMessage(), e);
        } catch (Exception e) {
            log.error("异步消息处理异常", e);
        } finally {
            for (Long userId : lockUserId) {
                userIdLockMap.remove(userId);
            }
        }
    }

    private void syncHandleEventMessage(BotEnum bot, BotMessage botMessage) {
        try {
            BotEvent botEvent = botMessage.getBotEvent();
            BotSender botSender = botMessage.getBotSender();
            // 校验权限
            boolean hasHelp = botSenderTaskMappingManager.checkSenderHasTaskCache(botSender.getId(), BotTaskConstant.helpTaskId);
            if (!hasHelp) {
                log.info(botMessage.getMessageId() + "无权限");
                return;
            }

            BaseEventHandle eventHandle = eventHandleMap.get(botEvent.getType());
            if (eventHandle == null) {
                log.info(botMessage.getMessageId() + "无处理");
                return;
            }
            BotMessage respMessage = null;
            try {
                // 返回null则代表跳过，继续寻找
                // 返回空消息则代表已处理完毕但不回复，直接结束
                respMessage = eventHandle.handleEvent(bot, botMessage);
            } catch (AssertException e) {
                log.debug(e.getMessage(), e);
            }

            // 如果最后为null，则标志无匹配处理器，则回复表情包
            if (respMessage == null) {
                log.info(botMessage.getMessageId() + "无回复");
                return;
            }
            // 如果最后是空消息，则表示匹配到处理器并处理完毕但不需要回复
            if (CollectionUtils.isEmpty(respMessage.getBotMessageChainList())) {
                log.info(botMessage.getMessageId() + "已处理");
                return;
            }

            // 没设置发送者，就默认原路发回
            if (respMessage.getBotSender() == null) {
                respMessage.setBotSender(botSender);
            }
            // 如果最后是消息，则回复
            sendMessageManager.sendMessage(respMessage);
//            if (messageId != null) {
//                Long respSenderId = respMessage.getBotSender() != null ? respMessage.getBotSender().getId() : respMessage.getSenderId();
//                if (respSenderId != null) {
//                    BotSessionService.MiraiSession respSession = botSessionService.getSession(respSenderId);
//                    respSession.put(lastMessageIdKey, messageId);
//                }
//            }
        } catch (AssertException e) {
            log.warn("异步事件处理断言异常, message=" + e.getMessage(), e);
        } catch (Exception e) {
            log.error("异步事件处理异常", e);
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
        BotEnum bot = BotEnum.getBotById(botSender.getBot());
        if (bot == null) return null;

        if (Objects.equals(quoteSenderId, bot.qq)) {
            BotSendMessageRecord sendMessageRecord = botSendMessageRecordMapper.getNewBotSendMessageRecordByMessageId(quoteMessageId);
            if (sendMessageRecord != null) {
                return gson.fromJson(sendMessageRecord.getMessage(), BotMessage.class);
            }
        }

        BotMessageRecord quoteMessageRecord = botMessageRecordMapper.getBotMessageRecordByMessageIdAndSenderId(quoteMessageId, botSender.getId());
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
        if (Objects.equals(sendType, SendTypeEnum.GUILD_MESSAGE.sendType)) {
            prefix = ".";
            actionKey = actionKey.replaceAll("^[.。]", prefix);
        }

        return botTaskMapper.getBotTaskListBySenderIdAndKeyOrNotKey(botSender.getId(), actionKey, prefix);
    }

}
