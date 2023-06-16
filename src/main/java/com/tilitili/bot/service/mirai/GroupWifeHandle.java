package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.bot.service.mirai.talk.ReplyHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUserSenderMapping;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotUserSenderMappingQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotUserSenderMappingMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.DateUtils;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.StreamUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GroupWifeHandle extends ExceptionRespMessageHandle {

    private final RedisCache redisCache;
    private final BotUserManager botUserManager;
    private final BotUserSenderMappingMapper botUserSenderMappingMapper;

    public GroupWifeHandle(RedisCache redisCache, BotUserManager botUserManager, BotUserSenderMappingMapper botUserSenderMappingMapper) {
        this.redisCache = redisCache;
        this.botUserManager = botUserManager;
        this.botUserSenderMappingMapper = botUserSenderMappingMapper;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
        switch (messageAction.getKeyWithoutPrefix()) {
            case "抽老婆": return this.handleStart(messageAction);
            case "灌注!": case "灌注！": return this.handleLove(messageAction);
            default: throw new AssertException();
        }
    }

    private final Date limitTime = DateUtils.parseDateYMD("2023-06-07");
    private BotMessage handleLove(BotMessageAction messageAction) {
        BotSender botSender = messageAction.getBotSender();
        BotUserDTO botUser = messageAction.getBotUser();
        Long wifeUserId = redisCache.getValueLong(this.getWifeMappingCacheKey(botSender, botUser));
        Asserts.notNull(wifeUserId, "没老婆灌注啥？飞机吗");
        List<BotUserDTO> atList = messageAction.getAtList();
        Asserts.isTrue(atList.stream().allMatch(StreamUtil.isEqual(BotUserDTO::getId, wifeUserId)), "不准牛喵");
        BotUserDTO wife = botUserManager.getValidBotUserByIdWithParent(wifeUserId);
        Long userMax = redisCache.getValueLong(this.getUserMaxCacheKey(botSender, botUser));
        if (userMax == null) {
            userMax = 100000L;
        }
        if (userMax < 1000) {
            return null;
        }
        long add = ThreadLocalRandom.current().nextLong(userMax);
        if (new Date().after(limitTime)) {
            redisCache.setValue(this.getUserMaxCacheKey(botSender, botUser), add, TimeUnit.DAYS.toSeconds(1));
            if (add < 100) {
                return BotMessage.simpleTextMessage(String.format("诶？才%.3f毫升？你没事吧？要不休息一下？", add / 1000.0));
            } else if (add < 1000) {
                return BotMessage.simpleTextMessage(String.format("才%.3f毫升吗？大哥哥这就不行了呀，真是的~明天再来吧", add / 1000.0));
            }
        }
        Long total = redisCache.increment(this.getWifeTotalCacheKey(botSender, wife), add, TimeUnit.DAYS.toSeconds(1));
        return BotMessage.simpleTextMessage(String.format("好欸！%s给%s注入了%.3f毫升的脱氧核糖核酸，当日总注入量为：%.3f",
                botUser.getName(), wife.getName(), add / 1000.0, total / 1000.0));
    }

    private String getUserMaxCacheKey(BotSender botSender, BotUserDTO botUser) {
        String time = DateUtils.formatDateYMD(new Date());
        return String.format("GroupWifeHandle.userMax-%s-%s-%s", time, botSender.getId(), botUser.getId());
    }

    private String getWifeTotalCacheKey(BotSender botSender, BotUserDTO wife) {
        String time = DateUtils.formatDateYMD(new Date());
        return String.format("GroupWifeHandle.wifeTotal-%s-%s-%s", time, botSender.getId(), wife.getId());
    }

    private String getWifeMappingCacheKey(BotSender botSender, BotUserDTO botUser) {
        String time = DateUtils.formatDateYMD(new Date());
        return String.format("GroupWifeHandle.wifeMapping-%s-%s-%s", time, botSender.getId(), botUser.getId());
    }

    private BotMessage handleStart(BotMessageAction messageAction) {
        BotSender botSender = messageAction.getBotSender();
        BotUserDTO botUser = messageAction.getBotUser();

        String redisKey = ReplyHandle.timeNumKey + "-" + DateUtils.formatDateYMD(new Date()) + "-" + botUser.getId();
        Long theTimeNum = redisCache.increment(redisKey, 1L);
        redisCache.expire(redisKey, 60 * 60 * 24);
        if (theTimeNum > 1) {
            return BotMessage.simpleTextMessage("不要太贪心哦。");
        }

        List<BotUserSenderMapping> mappingList = botUserSenderMappingMapper.getBotUserSenderMappingByCondition(new BotUserSenderMappingQuery().setSenderId(botSender.getId()));
        List<BotUserDTO> userList = mappingList.stream()
                .map(BotUserSenderMapping::getUserId).filter(Predicate.isEqual(botUser.getId()).negate())
                .map(userId -> botUserManager.getValidBotUserByIdWithParent(botSender.getId(), userId))
                .filter(Objects::nonNull).collect(Collectors.toList());
        BotUserDTO wifeUser = userList.get(ThreadLocalRandom.current().nextInt(userList.size()));
        String resp = String.format("你今天的群老婆是：%s(%s)", wifeUser.getName(), wifeUser.getQq());
        String image = wifeUser.getFace();
        redisCache.setValue(this.getWifeMappingCacheKey(botSender, botUser), wifeUser.getId(), TimeUnit.DAYS.toSeconds(1));
        return BotMessage.simpleImageTextMessage(resp, image);
    }
}
