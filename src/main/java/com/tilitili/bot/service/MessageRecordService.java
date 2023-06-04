package com.tilitili.bot.service;

import com.tilitili.bot.entity.MessageRecordDTO;
import com.tilitili.common.entity.BotMessageRecord;
import com.tilitili.common.entity.query.BotMessageRecordQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotMessageRecordMapper;
import com.tilitili.common.utils.StreamUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessageRecordService {
    private final BotMessageRecordMapper botMessageRecordMapper;
    private final BotManager botManager;

    public MessageRecordService(BotMessageRecordMapper botMessageRecordMapper, BotManager botManager) {
        this.botMessageRecordMapper = botMessageRecordMapper;
        this.botManager = botManager;
    }

    public BaseModel<PageModel<MessageRecordDTO>> list(BotMessageRecordQuery query) {
        query.setPageSize(20).setSorter("id").setSorted("desc");
        int total = botMessageRecordMapper.countBotMessageRecordByCondition(query);
        List<BotMessageRecord> list = botMessageRecordMapper.getBotMessageRecordByCondition(query);
        List<MessageRecordDTO> result = list.stream().map(record -> {
            MessageRecordDTO messageRecordDTO = new MessageRecordDTO();
            BotMessage botMessage;
            try {
                botMessage = botManager.handleMessageRecordToBotMessage(record);
            } catch (Exception e) {
                log.warn("处理消息异常", e);
                return null;
            }
            String content = botMessage.getBotMessageChainList().stream().map(messageChain -> {
                switch (messageChain.getType()) {
                    case BotMessage.MESSAGE_TYPE_PLAIN: return messageChain.getText();
                    case BotMessage.MESSAGE_TYPE_AT: return String.format("@%s ", messageChain.getTarget().getName());
                    case BotMessage.MESSAGE_TYPE_FACE: return "[emoji]";
                    default: return "";
                }
            }).filter(Objects::nonNull).collect(Collectors.joining());
            List<String> picList = botMessage.getBotMessageChainList().stream().filter(StreamUtil.isEqual(BotMessageChain::getType, BotMessage.MESSAGE_TYPE_IMAGE)).map(BotMessageChain::getUrl).collect(Collectors.toList());
            messageRecordDTO.setId(messageRecordDTO.getId());
            messageRecordDTO.setContent(content);
            messageRecordDTO.setPicList(picList);
            messageRecordDTO.setSendType(botMessage.getBotSender().getSendType());
            messageRecordDTO.setSenderName(botMessage.getBotSender().getName());
            messageRecordDTO.setUserName(botMessage.getBotUser().getName());
            messageRecordDTO.setHasReply(record.getReplyMessageId() != null);

            return messageRecordDTO;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        return PageModel.of(total, query.getPageSize(), query.getCurrent(), result);
    }
}
