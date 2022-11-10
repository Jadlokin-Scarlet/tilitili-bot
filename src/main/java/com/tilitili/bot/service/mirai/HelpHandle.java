package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotTask;
import com.tilitili.common.entity.dto.BotTaskDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotSenderManager;
import com.tilitili.common.mapper.mysql.BotTaskMapper;
import com.tilitili.common.utils.Asserts;
import org.jsoup.helper.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HelpHandle extends ExceptionRespMessageHandle {
    private final BotSenderManager botSenderManager;
    private final BotTaskMapper botTaskMapper;

    @Autowired
    public HelpHandle(BotSenderManager botSenderManager, BotTaskMapper botTaskMapper) {
        this.botSenderManager = botSenderManager;
        this.botTaskMapper = botTaskMapper;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String paramListStr = messageAction.getValueOrDefault("").replaceAll("\\s+", " ").trim();;
//        String sendType = messageAction.getBotMessage().getSendType();
//        String guildPrefix = sendType.equals(SendTypeEmum.Guild_Message.sendType)? ".": "";

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
                reply = String.format("[%s]的作用是[%s]。", paramListStr, paramListStr);
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
