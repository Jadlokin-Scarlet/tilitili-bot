package com.tilitili.bot.service.mirai.subscription;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.Owner;
import com.tilitili.common.entity.Subscription;
import com.tilitili.common.entity.query.SubscriptionQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BilibiliManager;
import com.tilitili.common.mapper.tilitili.SubscriptionMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StreamUtil;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AddSubscriptionHandle extends ExceptionRespMessageHandle {
    private final SubscriptionMapper subscriptionMapper;
    private final BilibiliManager bilibiliManager;

    @Autowired
    public AddSubscriptionHandle(SubscriptionMapper subscriptionMapper, BilibiliManager bilibiliManager) {
        this.subscriptionMapper = subscriptionMapper;
        this.bilibiliManager = bilibiliManager;
    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.AddSubscriptionHandle;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String key = messageAction.getParamOrDefault("key", messageAction.getValue());
        BotMessage botMessage = messageAction.getBotMessage();

        Long qq = botMessage.getQq();
        Long group = botMessage.getGroup();
        String guildId = botMessage.getChannel() == null? botMessage.getGuildId(): botMessage.getChannel().guildId;
        String channelId = botMessage.getChannelId() == null? botMessage.getChannelId(): botMessage.getChannel().channelId;
        String sendType = botMessage.getSendType();

        Asserts.notBlank(key, "格式错啦(key)");

        Owner owner;
        if (StringUtils.isNumber(key)) {
            owner = bilibiliManager.getUserWithCache(Long.valueOf(key));
        } else {
            List<Owner> ownerList = bilibiliManager.searchUserWithCache(key);
            List<Owner> filterOwnerList = ownerList.stream().filter(StreamUtil.isEqual(Owner::getName, key)).collect(Collectors.toList());
            Asserts.notEmpty(filterOwnerList, "找不到用户");
            Asserts.checkEquals(filterOwnerList.size(), 1, "有重名(%s)", filterOwnerList.stream().map(Owner::getName).collect(Collectors.joining(",")));
            owner = filterOwnerList.get(0);
        }
        Long uid = owner.getUid();
        String name = owner.getName();
        Long roomId = owner.getRoomId();

        Long qqWithoutGroup = SendTypeEmum.GROUP_MESSAGE.equals(sendType)? null: qq;
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery().setStatus(0).setType(1).setValue(String.valueOf(uid)).setSendType(sendType).setSendGroup(group).setSendQq(qqWithoutGroup).setSendGuild(guildId).setSendChannel(channelId);
        int oldCount = subscriptionMapper.countSubscriptionByCondition(subscriptionQuery);
        Asserts.isTrue(oldCount == 0, "已经关注了哦。");

        Subscription add = new Subscription().setValue(String.valueOf(uid)).setType(1).setSendType(sendType).setSendGroup(group).setSendQq(qqWithoutGroup).setSendGuild(guildId).setSendChannel(channelId).setName(name).setRoomId(roomId);
        subscriptionMapper.addSubscriptionSelective(add);

        return BotMessage.simpleTextMessage(String.format("关注%s成功！", name));
    }
}
