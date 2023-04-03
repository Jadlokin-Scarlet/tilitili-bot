package com.tilitili.bot.service.mirai.talk;

import com.google.gson.Gson;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.FunctionTalkService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.GroupEnum;
import com.tilitili.common.entity.*;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotTalkManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotFunctionMapper;
import com.tilitili.common.mapper.mysql.BotFunctionTalkMapper;
import com.tilitili.common.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class ReplyHandle extends ExceptionRespMessageHandle {
    public static final String timeNumKey = "ReplyHandle.timeNum";
    @Value("${mirai.master-qq}")
    private Long MASTER_QQ;
    private final BotTalkManager botTalkManager;
    private final BotFunctionTalkMapper botFunctionTalkMapper;
    private final Random random;
    private final Gson gson;
    private final FunctionTalkService functionTalkService;
    private final BotFunctionMapper botFunctionMapper;
    private final BotUserManager botUserManager;
    private final RedisCache redisCache;

    @Autowired
    public ReplyHandle(BotTalkManager botTalkManager, BotFunctionTalkMapper botFunctionTalkMapper, FunctionTalkService functionTalkService, BotFunctionMapper botFunctionMapper, BotUserManager botUserManager, RedisCache redisCache) throws IOException {
        this.botFunctionTalkMapper = botFunctionTalkMapper;
        this.functionTalkService = functionTalkService;
        this.botFunctionMapper = botFunctionMapper;
        this.botUserManager = botUserManager;
        this.redisCache = redisCache;
        this.gson = new Gson();
        this.botTalkManager = botTalkManager;
        this.random = new Random(System.currentTimeMillis());
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String text = messageAction.getText();
        BotRobot bot = messageAction.getBot();
        BotMessage botMessage = messageAction.getBotMessage();
        BotSender botSender = messageAction.getBotSender();
        Long qq = botSender.getQq();
        Long group = botSender.getGroup();
        BotUserDTO botUser = messageAction.getBotUser();

        String req = TalkHandle.convertMessageToString(botMessage);
        List<BotFunctionTalk> functionTalkList = botFunctionTalkMapper.getRespListByReq(req, botSender.getId(), botUser.getQq());
        if (!functionTalkList.isEmpty()) {
            BotFunctionTalk functionTalk = functionTalkList.get(random.nextInt(functionTalkList.size()));
            BotFunction botFunction = botFunctionMapper.getBotFunctionById(functionTalk.getFunctionId());
            if (botFunction.getScore() > 0) {
                Asserts.notNull(botUser.getScore(), "未绑定");
                if (botUser.getScore() < botFunction.getScore()) {
                    return BotMessage.simpleTextMessage(String.format("啊嘞，积分不够了。(%s)", botFunction.getScore()));
                }
            }
            BotMessage respMessage = Gsons.fromJson(functionTalk.getResp(), BotMessage.class);
            functionTalkService.supplementChain(bot, botSender, respMessage);
            String redisKey = timeNumKey + "-" + botFunction.getFunction() + "-" + DateUtils.formatDateYMD(new Date()) + "-" + botUser.getId();
            Long theTimeNum = redisCache.increment(redisKey, 1L);
            redisCache.expire(redisKey, 60 * 60 * 24);
            if (theTimeNum > botFunction.getTimeNum()) {
                return BotMessage.simpleTextMessage("不要太贪心哦。");
            }
            if (botFunction.getScore() > 0) {
                botUserManager.safeUpdateScore(botUser, - botFunction.getScore());
            }
            return respMessage.setQuote(messageAction.getMessageId());
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

        if (Objects.equals(group, GroupEnum.HOMO_LIVE_GROUP.value)) {
            int ddCount = StringUtils.findCount("dd|DD|dD|Dd", text);
            if (ddCount > 0) {
                String repeat = IntStream.range(0, ddCount).mapToObj(c -> "bd").collect(Collectors.joining());
                return BotMessage.simpleTextMessage(repeat);
            }
        }

        if (Pattern.matches("[笨蛋]+", text)) {
            int ddCount = StringUtils.findCount("笨蛋", text);
            if (ddCount > 0) {
                String repeat = IntStream.range(0, ddCount).mapToObj(c -> "不笨").collect(Collectors.joining());
                return BotMessage.simpleTextMessage(repeat);
            }
        }

        if (Objects.equals(qq, MASTER_QQ) && text.equals("cww")) {
            return BotMessage.simpleTextMessage("cww");
        }

        return null;
    }
}
