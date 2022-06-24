package com.tilitili.bot.service.mirai.talk;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.GuildEmum;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.BotTalk;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotTalkManager;
import com.tilitili.common.utils.QQUtil;
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
    private final BotTalkManager botTalkManager;

    @Autowired
    public ReplyHandle(BotTalkManager botTalkManager) {
        this.botTalkManager = botTalkManager;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String text = messageAction.getText();
        List<String> imageList = messageAction.getImageList();
        BotMessage botMessage = messageAction.getBotMessage();
        Long qq = botMessage.getQq();
        Long guildId = botMessage.getGuildId();
        String sendType = botMessage.getSendType();
        List<BotTalk> botTalkList;
        if (StringUtils.isBlank(text) && imageList.size() == 1) {
            botTalkList = botTalkManager.getBotTalkByBotMessage(QQUtil.getImageUrl(imageList.get(0)), botMessage);
        } else {
            botTalkList = botTalkManager.getBotTalkByBotMessage(text, botMessage);
        }
        if (!botTalkList.isEmpty()) {
            BotTalk botTalk = botTalkList.get(0);
            if (botTalk.getType().equals(0)) {
                return BotMessage.simpleTextMessage(botTalk.getResp());
            } else {
                return BotMessage.simpleImageMessage(botTalk.getResp());
            }
        }

        if (! Objects.equals(SendTypeEmum.GUILD_MESSAGE.sendType, sendType) || Objects.equals(guildId, GuildEmum.OUR_HOMO.guildId)) {
            if (! text.contains("addons") && !text.contains("http")) {
                int ddCount = StringUtils.findCount("dd|DD|dD|Dd", text);
                if (ddCount > 0) {
                    String repeat = IntStream.range(0, ddCount).mapToObj(c -> "bd").collect(Collectors.joining());
                    return BotMessage.simpleTextMessage(repeat);
                }
            }
        }

        if (Objects.equals(qq, MASTER_QQ) && text.equals("cww")) {
            return BotMessage.simpleTextMessage("cww");
        }

        return null;
    }
}
