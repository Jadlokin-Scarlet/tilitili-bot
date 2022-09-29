package com.tilitili.bot.service.mirai.talk;

import com.google.gson.Gson;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.GroupEmum;
import com.tilitili.common.entity.BotFunctionTalk;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotTalk;
import com.tilitili.common.entity.query.BotFunctionTalkQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotTalkManager;
import com.tilitili.common.mapper.mysql.BotFunctionTalkMapper;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class ReplyHandle extends ExceptionRespMessageHandle {
    @Value("${mirai.master-qq}")
    private Long MASTER_QQ;
    private final BotTalkManager botTalkManager;
    private final BotFunctionTalkMapper botFunctionTalkMapper;
    private final Random random;
    private final Gson gson;

    @Autowired
    public ReplyHandle(BotTalkManager botTalkManager, BotFunctionTalkMapper botFunctionTalkMapper) throws IOException {
        this.botFunctionTalkMapper = botFunctionTalkMapper;
        this.gson = new Gson();
        this.botTalkManager = botTalkManager;
        this.random = new Random(System.currentTimeMillis());
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String text = messageAction.getText();
        BotMessage botMessage = messageAction.getBotMessage();
        BotSender botSender = messageAction.getBotSender();
        Long qq = botSender.getQq();
        Long group = botSender.getGroup();

        String req = TalkHandle.convertMessageToString(botMessage);
        List<BotFunctionTalk> functionTalkList = botFunctionTalkMapper.getBotFunctionTalkByCondition(new BotFunctionTalkQuery().setReq(req).setSenderId(botSender.getId()).setStatus(0));
        if (!functionTalkList.isEmpty()) {
            BotFunctionTalk functionTalk = functionTalkList.get(random.nextInt(functionTalkList.size()));
            return TalkHandle.convertStringToMessage(functionTalk.getResp());
        }

        BotTalk botTalk = botTalkManager.getJsonTalkOrOtherTalk(req, botMessage);
        if (botTalk != null) {
            if (botTalk.getType().equals(0)) {
                return BotMessage.simpleTextMessage(botTalk.getResp());
            } else if (botTalk.getType() == 1) {
                return BotMessage.simpleImageMessage(botTalk.getResp());
            } else if (botTalk.getType() == 2) {
                return BotMessage.simpleListMessage(gson.fromJson(botTalk.getResp(), BotMessage.class).getBotMessageChainList());
            }
        }

        if (Objects.equals(group, GroupEmum.HOMO_LIVE_GROUP.value)) {
            int ddCount = StringUtils.findCount("dd|DD|dD|Dd", text);
            if (ddCount > 0) {
                String repeat = IntStream.range(0, ddCount).mapToObj(c -> "bd").collect(Collectors.joining());
                return BotMessage.simpleTextMessage(repeat);
            }
        }

        if (Objects.equals(qq, MASTER_QQ) && text.equals("cww")) {
            return BotMessage.simpleTextMessage("cww");
        }

        return null;
    }
}
