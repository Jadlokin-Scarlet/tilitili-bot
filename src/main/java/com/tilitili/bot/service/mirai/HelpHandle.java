package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.BaseMessageHandle;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotSenderTaskMapping;
import com.tilitili.common.entity.BotTask;
import com.tilitili.common.entity.dto.BotTaskDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotRoleManager;
import com.tilitili.common.mapper.mysql.BotSenderTaskMappingMapper;
import com.tilitili.common.mapper.mysql.BotTaskMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class HelpHandle extends ExceptionRespMessageHandle {
    private final BotTaskMapper botTaskMapper;
    private final Map<String, BaseMessageHandle> messageHandleMap;
    private final BotSenderTaskMappingMapper botSenderTaskMappingMapper;
    private final BotRoleManager botRoleManager;

    public HelpHandle(BotTaskMapper botTaskMapper, Map<String, BaseMessageHandle> messageHandleMap, BotSenderTaskMappingMapper botSenderTaskMappingMapper, BotRoleManager botRoleManager) {
        this.botTaskMapper = botTaskMapper;
        this.messageHandleMap = messageHandleMap;
        this.botSenderTaskMappingMapper = botSenderTaskMappingMapper;
        this.botRoleManager = botRoleManager;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        switch (messageAction.getKeyWithoutPrefix()) {
            case "启用": return handleOpen(messageAction);
            case "禁用": return handleClose(messageAction);
            default: return handelHelp(messageAction);
        }
    }

    private BotMessage handleClose(BotMessageAction messageAction) {
        boolean canUseBotAdminTask = botRoleManager.canUseBotAdminTask(messageAction.getBot(), messageAction.getBotUser());
        if (!canUseBotAdminTask) {
            return null;
        }
        Long senderId = messageAction.getBotSender().getId();
        String taskNameList = messageAction.getValue();
        Asserts.notBlank(taskNameList, "格式错啦");
        List<Long> taskIdList = new ArrayList<>();
        for (String taskName : taskNameList.split("，")) {
            BotTask task = botTaskMapper.getBotTaskByNick(taskName);
            Asserts.notNull(task, "%s是什么", taskName);
            taskIdList.add(task.getId());
        }

        for (Long taskId : taskIdList) {
            BotSenderTaskMapping botSenderTaskMapping = botSenderTaskMappingMapper.getBotSenderTaskMappingBySenderIdAndTaskId(senderId, taskId);
            if (botSenderTaskMapping != null) {
                botSenderTaskMappingMapper.deleteBotSenderTaskMappingById(botSenderTaskMapping.getId());
            }
        }
        return BotMessage.simpleTextMessage("√");
    }

    private BotMessage handleOpen(BotMessageAction messageAction) {
        boolean canUseBotAdminTask = botRoleManager.canUseBotAdminTask(messageAction.getBot(), messageAction.getBotUser());
        if (!canUseBotAdminTask) {
            return null;
        }
        Long senderId = messageAction.getBotSender().getId();
        String taskNameList = messageAction.getValue();
        Asserts.notBlank(taskNameList, "格式错啦");
        List<Long> taskIdList = new ArrayList<>();
        for (String taskName : taskNameList.split("，")) {
            BotTask task = botTaskMapper.getBotTaskByNick(taskName);
            Asserts.notNull(task, "%s是什么", taskName);
            taskIdList.add(task.getId());
        }

        for (Long taskId : taskIdList) {
            BotSenderTaskMapping botSenderTaskMapping = botSenderTaskMappingMapper.getBotSenderTaskMappingBySenderIdAndTaskId(senderId, taskId);
            if (botSenderTaskMapping == null) {
                botSenderTaskMappingMapper.addBotSenderTaskMappingSelective(new BotSenderTaskMapping().setTaskId(taskId).setSenderId(senderId));
            }
        }
        return BotMessage.simpleTextMessage("√");
    }

    private BotMessage handelHelp(BotMessageAction messageAction) {
        String paramListStr = messageAction.getValueOrDefault("").trim();//.replaceAll("\\s+", " ")
        ;
//        String sendType = messageAction.getBotMessage().getSendType();
//        String guildPrefix = sendType.equals(SendTypeEnum.Guild_Message.sendType)? ".": "";

        BotSender botSender = messageAction.getBotSender();
        List<BotTaskDTO> botTaskDTOList = botTaskMapper.getBotTaskListForHelp(botSender.getId());
        if (botTaskDTOList.isEmpty()) {
            return null;
        }

        if (StringUtils.isBlank(paramListStr)) {
            StringBuilder reply = new StringBuilder("咱可以帮你做这些事！查看详情发送（帮助 [指令]）\n");
            for (int i = 0; i < botTaskDTOList.size(); i++) {
                BotTaskDTO botTask = botTaskDTOList.get(i);
                String key = botTask.getKeyListStr() == null ? "" : botTask.getKeyListStr();
                reply.append(String.format("%s. %s: %s\n", i + 1, botTask.getNick(), key));
            }
            if (reply.charAt(reply.length() - 1) == '\n') {
                reply.deleteCharAt(reply.length() - 1);
            }
            return BotMessage.simpleTextMessage(reply.toString());
        } else if (! paramListStr.contains(" ")) {
            BotTask botTask = getBotTaskByTaskName(botSender, paramListStr);
            BaseMessageHandle messageHandle = messageHandleMap.get(botTask.getName());
            String reply = String.format("[%s]的作用是[%s]。", paramListStr, messageHandle.getHelpMessage(botTask));
            return BotMessage.simpleTextMessage(reply);
        } else {
            int paramIndex = paramListStr.indexOf(" ");
            String taskName = paramListStr.substring(0, paramIndex).trim();
            String taskKey = paramListStr.substring(paramIndex).trim();

            BotTask botTask = getBotTaskByTaskName(botSender, taskName);
            BaseMessageHandle messageHandle = messageHandleMap.get(botTask.getName());
            String reply = String.format("[%s]的作用是[%s]。", paramListStr, messageHandle.getHelpMessage(botTask, taskKey));
            return BotMessage.simpleTextMessage(reply);
        }
    }

    private BotTask getBotTaskByTaskName(BotSender botSender, String paramListStr) {
        BotTask botTask = botTaskMapper.getBotTaskByNick(paramListStr);
        if (botTask == null) {
            List<BotTask> botTaskList = botTaskMapper.getBotTaskListBySenderIdAndKey(botSender.getId(), paramListStr, "");
            Asserts.isTrue(botTaskList.size() < 2, "不对劲");
            Asserts.notEmpty(botTaskList, "%s是啥", paramListStr);
            botTask = botTaskList.get(0);
        }
        return botTask;
    }
}
