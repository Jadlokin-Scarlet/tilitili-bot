package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotSenderTaskMapping;
import com.tilitili.common.entity.BotTask;
import com.tilitili.common.entity.dto.BotTaskDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.mysql.BotSenderTaskMappingMapper;
import com.tilitili.common.mapper.mysql.BotTaskMapper;
import com.tilitili.common.utils.Asserts;
import org.jsoup.helper.StringUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HelpHandle extends ExceptionRespMessageHandle {
    private final BotTaskMapper botTaskMapper;
    private final BotSenderTaskMappingMapper botSenderTaskMappingMapper;

    public HelpHandle(BotTaskMapper botTaskMapper, BotSenderTaskMappingMapper botSenderTaskMappingMapper) {
        this.botTaskMapper = botTaskMapper;
        this.botSenderTaskMappingMapper = botSenderTaskMappingMapper;
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
        if (!BotUserConstant.MASTER_USER_ID.equals(messageAction.getBotUser().getId())) {
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
        if (!BotUserConstant.MASTER_USER_ID.equals(messageAction.getBotUser().getId())) {
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
        String paramListStr = messageAction.getValueOrDefault("").replaceAll("\\s+", " ").trim();
        ;
//        String sendType = messageAction.getBotMessage().getSendType();
//        String guildPrefix = sendType.equals(SendTypeEnum.Guild_Message.sendType)? ".": "";

        BotSender botSender = messageAction.getBotSender();
        List<BotTaskDTO> botTaskDTOList = botTaskMapper.getBotTaskListForHelp(botSender.getId());
        if (botTaskDTOList.isEmpty()) {
            return null;
        }

        if (StringUtil.isBlank(paramListStr)) {
            StringBuilder reply = new StringBuilder("咱可以帮你做这些事！查看详情发送（帮助 [指令]）\n");
            for (int i = 0; i < botTaskDTOList.size(); i++) {
                BotTaskDTO botTask = botTaskDTOList.get(i);
                String key = botTask.getKeyListStr() == null ? "" : botTask.getKeyListStr();
                reply.append(String.format("%s.%s: %s%n", i + 1, botTask.getNick(), key));
            }
            if (reply.charAt(reply.length() - 1) == '\n') {
                reply.deleteCharAt(reply.length() - 1);
            }
            return BotMessage.simpleTextMessage(reply.toString());
        } else if (! paramListStr.contains(" ")) {
            List<BotTask> botTaskList = botTaskMapper.getBotTaskListBySenderIdAndKey(botSender.getId(), paramListStr, "");
            Asserts.isTrue(botTaskList.size() < 2, "不对劲");
            String reply;
            if (botTaskList.isEmpty()) {
                return null;
//                reply = String.format("[%s]的作用是[%s]。", paramListStr, paramListStr);
            } else {
                BotTask botTask = botTaskList.get(0);
                reply = String.format("[%s]的作用是[%s]。", paramListStr, botTask.getDescription());
            }
            return BotMessage.simpleTextMessage(reply);
        } else {
            return null;
        }
    }
}
