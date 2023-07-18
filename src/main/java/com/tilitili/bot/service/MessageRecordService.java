package com.tilitili.bot.service;

import com.tilitili.bot.entity.MessageRecordVO;
import com.tilitili.common.entity.dto.MessageRecordDTO;
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
public class
MessageRecordService {
    private final BotMessageRecordMapper botMessageRecordMapper;
    private final BotManager botManager;

    public MessageRecordService(BotMessageRecordMapper botMessageRecordMapper, BotManager botManager) {
        this.botMessageRecordMapper = botMessageRecordMapper;
        this.botManager = botManager;
    }

    public BaseModel<PageModel<MessageRecordVO>> list(BotMessageRecordQuery query) {
        query.setPageSize(20).setSorter("id").setSorted("desc");
        int total = botMessageRecordMapper.countBotMessageRecordByCondition(query);
        List<MessageRecordDTO> list = botMessageRecordMapper.listPageBotMessageRecord(query);
        List<MessageRecordVO> resultList = list.stream().map(record -> {
            MessageRecordVO result = new MessageRecordVO();
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
            result.setId(record.getId());
            result.setContent(content);
            result.setPicList(picList);
            result.setSendType(botMessage.getBotSender().getSendType());
            result.setSenderName(botMessage.getBotSender().getName());
            result.setUserName(botMessage.getBotUser().getName());
            result.setHasReply(record.getReplyMessageId() != null);

            log.info("list....");
            return result;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        return PageModel.of(total, query.getPageSize(), query.getCurrent(), resultList);
    }
}
