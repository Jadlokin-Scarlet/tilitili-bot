package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.bot.service.mirai.talk.ReplyHandle;
import com.tilitili.bot.service.mirai.talk.TalkHandle;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.entity.BotFunction;
import com.tilitili.common.entity.BotFunctionTalk;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUserSenderMapping;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotUserSenderMappingQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotUserSenderMappingMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.DateUtils;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
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
        BotSender botSender = messageAction.getBotSender();
        BotUserDTO botUser = messageAction.getBotUser();

        String redisKey = ReplyHandle.timeNumKey + "-" + DateUtils.formatDateYMD(new Date()) + "-" + botUser.getId();
        Long theTimeNum = redisCache.increment(redisKey, 1L);
        redisCache.expire(redisKey, 60 * 60 * 24);
        if (theTimeNum >= 1) {
            return BotMessage.simpleTextMessage("不要太贪心哦。");
        }

        List<BotUserSenderMapping> mappingList = botUserSenderMappingMapper.getBotUserSenderMappingByCondition(new BotUserSenderMappingQuery().setSenderId(botSender.getId()));
        List<BotUserDTO> userList = mappingList.stream().map(BotUserSenderMapping::getUserId).map(botUserManager::getBotUserByIdWithParent).filter(Objects::nonNull).collect(Collectors.toList());
        BotUserDTO wifeUser = userList.get(ThreadLocalRandom.current().nextInt(userList.size()));
        String resp = String.format("你今天的群老婆是：%s(%s)", wifeUser.getName(), wifeUser.getQq());
        String image = wifeUser.getFace();
        return BotMessage.simpleImageTextMessage(resp, image);

    }
}
