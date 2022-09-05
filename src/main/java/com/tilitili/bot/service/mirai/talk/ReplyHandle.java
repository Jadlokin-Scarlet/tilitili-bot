package com.tilitili.bot.service.mirai.talk;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.GroupEmum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotTalk;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotTalkManager;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.IOUtils;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class ReplyHandle extends ExceptionRespMessageHandle {
    @Value("${mirai.master-qq}")
    private Long MASTER_QQ;
    private final BotTalkManager botTalkManager;
    private final Gson gson;
    private final Random random;
    private final Map<String, List<String>> wordMap;

    @Autowired
    public ReplyHandle(BotTalkManager botTalkManager) throws IOException {
        this.gson = new Gson();
        this.botTalkManager = botTalkManager;
        random = new Random(System.currentTimeMillis());
        String jsonStr = IOUtils.toString(ReplyHandle.class.getResourceAsStream("/word.json"), StandardCharsets.UTF_8);
        wordMap = Gsons.fromJson(jsonStr, new TypeToken<Map<String, List<String>>>() {}.getType());
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String text = messageAction.getText();
        BotMessage botMessage = messageAction.getBotMessage();
        BotSender botSender = messageAction.getBotSender();
        Long qq = botSender.getQq();
        Long group = botSender.getGroup();
        BotTalk botTalk = botTalkManager.getJsonTalkOrOtherTalk(TalkHandle.convertMessageToString(botMessage), botMessage);
        if (botTalk != null) {
            if (botTalk.getType().equals(0)) {
                return BotMessage.simpleTextMessage(botTalk.getResp());
            } else if (botTalk.getType() == 1) {
                return BotMessage.simpleImageMessage(botTalk.getResp());
            } else if (botTalk.getType() == 2) {
                return BotMessage.simpleListMessage(gson.fromJson(botTalk.getResp(), BotMessage.class).getBotMessageChainList());
            }
        }
        if (Objects.equals(group, GroupEmum.HOMO_LIVE_GROUP.value) || Objects.equals(qq, MASTER_QQ)) {
            for (Map.Entry<String, List<String>> entry : wordMap.entrySet()) {
                String key = entry.getKey();
                List<String> valueList = entry.getValue();
                if (text.length() - 3 <= key.length() && text.contains(key)) {
                    String resp = valueList.get(random.nextInt(valueList.size()));
                    return BotMessage.simpleTextMessage(resp);
                }
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
