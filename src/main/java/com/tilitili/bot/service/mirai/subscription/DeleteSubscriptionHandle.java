package com.tilitili.bot.service.mirai.subscription;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.Subscription;
import com.tilitili.common.entity.query.SubscriptionQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.tilitili.SubscriptionMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeleteSubscriptionHandle extends ExceptionRespMessageHandle {
	private final SubscriptionMapper subscriptionMapper;

	@Autowired
	public DeleteSubscriptionHandle(SubscriptionMapper subscriptionMapper) {
		this.subscriptionMapper = subscriptionMapper;
	}

	@Override
	public MessageHandleEnum getType() {
		return MessageHandleEnum.DeleteSubscriptionHandle;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		String uid = messageAction.getParamOrDefault("uid", messageAction.getValue());
		BotMessage botMessage = messageAction.getBotMessage();

		Long qq = botMessage.getQq();
		Long group = botMessage.getGroup();
		String guildId = botMessage.getChannel() == null? botMessage.getGuildId(): botMessage.getChannel().guildId;
		String channelId = botMessage.getChannelId() == null? botMessage.getChannelId(): botMessage.getChannel().channelId;
		String sendType = botMessage.getSendType();

		Asserts.notBlank(uid, "格式错啦(uid)");

		Long qqWithoutGroup = SendTypeEmum.GROUP_MESSAGE.equals(sendType)? null: qq;
		SubscriptionQuery subscriptionQuery = new SubscriptionQuery().setStatus(0).setType(1).setValue(uid).setSendType(sendType).setSendGroup(group).setSendQq(qqWithoutGroup).setSendGuild(guildId).setSendChannel(channelId);
		List<Subscription> oldList = subscriptionMapper.getSubscriptionByCondition(subscriptionQuery);
		Asserts.notEmpty(oldList, "还没关注哦。");
		Asserts.checkEquals(oldList.size(), 1, "不太对劲。");
		Subscription old = oldList.get(0);

		subscriptionMapper.updateSubscriptionSelective(new Subscription().setId(old.getId()).setStatus(-1));

		return BotMessage.simpleTextMessage("搞定。");
	}
}
