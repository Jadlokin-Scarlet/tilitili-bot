package com.tilitili.bot.service.mirai.talk;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.BotTalk;
import com.tilitili.common.entity.query.BotTalkQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.mysql.BotTalkMapper;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class ReplyHandle extends ExceptionRespMessageHandle {
    @Value("${mirai.master-qq}")
    private Long MASTER_QQ;
    private final BotTalkMapper botTalkMapper;

    @Autowired
    public ReplyHandle(BotTalkMapper botTalkMapper) {
        this.botTalkMapper = botTalkMapper;
    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.REPLY_HANDLE;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String text = messageAction.getText();
        BotMessage botMessage = messageAction.getBotMessage();
        String sendType = botMessage.getSendType();
        Long qq = botMessage.getQq();
        Long group = botMessage.getGroup();
        String guildId = botMessage.getGuildId();
        String channelId = botMessage.getChannelId();

        BotTalkQuery botTalkQuery = new BotTalkQuery().setReq(text).setSendType(sendType);
        switch (sendType) {
            case SendTypeEmum.FRIEND_MESSAGE: botMessage.setQq(qq); break;
            case SendTypeEmum.GROUP_MESSAGE: botMessage.setGroup(group); break;
            case SendTypeEmum.TEMP_MESSAGE: botMessage.setQq(qq).setGroup(group); break;
            case SendTypeEmum.GUILD_MESSAGE: botMessage.setGuildId(guildId).setChannelId(channelId); break;
        }
        List<BotTalk> botTalkList = botTalkMapper.getBotTalkByCondition(botTalkQuery);
        if (!botTalkList.isEmpty()) {
            BotTalk botTalk = botTalkList.get(0);
            return BotMessage.simpleTextMessage(botTalk.getResp());
        }

        int ddCount = StringUtils.findCount("dd|DD|dD|Dd", text);
        if (ddCount > 0) {
            String repeat = IntStream.range(0, ddCount).mapToObj(c -> "bd").collect(Collectors.joining());
            return BotMessage.simpleTextMessage(repeat);
        }

        if (Objects.equals(qq, MASTER_QQ) && text.equals("cww")) {
            return BotMessage.simpleTextMessage("cww");
        }

        if (text.equals("让我看看") || text.equals("让我康康")) {
            return BotMessage.simpleTextMessage("不要！");
        }

        return null;
    }
}
