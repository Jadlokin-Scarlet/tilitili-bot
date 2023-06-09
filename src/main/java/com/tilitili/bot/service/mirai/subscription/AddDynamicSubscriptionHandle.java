package com.tilitili.bot.service.mirai.subscription;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
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
public class AddDynamicSubscriptionHandle extends ExceptionRespMessageHandle {
    private final SubscriptionMapper subscriptionMapper;
    private final BilibiliManager bilibiliManager;

    @Autowired
    public AddDynamicSubscriptionHandle(SubscriptionMapper subscriptionMapper, BilibiliManager bilibiliManager) {
        this.subscriptionMapper = subscriptionMapper;
        this.bilibiliManager = bilibiliManager;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String key = messageAction.getParamOrDefault("key", messageAction.getValue());
        BotSender botSender = messageAction.getBotSender();
        Long senderId = botSender.getId();

        Asserts.notBlank(key, "格式错啦(key)");

        Owner owner = bilibiliManager.getOwnerWithCacheByUidOrName(key);
        Long uid = owner.getUid();
        String name = owner.getName();
        Long roomId = owner.getRoomId();

        SubscriptionQuery subscriptionQuery = new SubscriptionQuery().setStatus(0).setType(3).setValue(String.valueOf(uid)).setSenderId(senderId);
        int oldCount = subscriptionMapper.countSubscriptionByCondition(subscriptionQuery);
        Asserts.isTrue(oldCount == 0, "已经关注了哦。");

        Subscription add = new Subscription().setValue(String.valueOf(uid)).setType(3).setSenderId(senderId).setName(name).setRoomId(roomId);
        subscriptionMapper.addSubscriptionSelective(add);

        return BotMessage.simpleTextMessage(String.format("关注%s成功！", name));
    }
}
