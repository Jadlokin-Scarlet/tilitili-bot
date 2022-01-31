package com.tilitili.bot.service.mirai.subscription;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.Owner;
import com.tilitili.common.entity.Subscription;
import com.tilitili.common.entity.query.SubscriptionQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BilibiliManager;
import com.tilitili.common.mapper.mysql.SubscriptionMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String key = messageAction.getParamOrDefault("key", messageAction.getValue());
        BotMessage botMessage = messageAction.getBotMessage();

        Long qq = botMessage.getQq();
        Long group = botMessage.getGroup();
        String guildId = botMessage.getChannel() == null? botMessage.getGuildId(): botMessage.getChannel().guildId;
        String channelId = botMessage.getChannel() == null? botMessage.getChannelId(): botMessage.getChannel().channelId;
        String sendType = botMessage.getSendType();

        Asserts.notBlank(key, "格式错啦(key)");

        Owner owner = bilibiliManager.getOwnerWithCacheByUidOrName(key);
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
