package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.entity.BotForwardConfig;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.query.BotForwardConfigQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
import com.tilitili.common.mapper.mysql.BotUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class BindHandle extends ExceptionRespMessageToSenderHandle {
	private static final String applyKey = "BindHandle.apply-";

	private final RedisCache redisCache;
	private final BotUserMapper botUserMapper;
	private final BotForwardConfigMapper botForwardConfigMapper;

	public BindHandle(RedisCache redisCache, BotUserMapper botUserMapper, BotForwardConfigMapper botForwardConfigMapper) {
		this.redisCache = redisCache;
		this.botUserMapper = botUserMapper;
		this.botForwardConfigMapper = botForwardConfigMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		switch (messageAction.getKey()) {
			case "申请合体": return handleApply(messageAction);
			case "合体！": return handleAccept(messageAction);
			default: throw new AssertException();
		}
	}

	private BotMessage handleAccept(BotMessageAction messageAction) {
		BotUser botUser = messageAction.getBotUser();

		String targetKey = applyKey + botUser.getId();
		if (!redisCache.exists(targetKey)) {
			return null;
		}
		String sourceKey = (String) redisCache.getValue(targetKey);

		Asserts.notEquals(botUser.getType(), 0, "啊嘞，不对劲");

		long sourceUserId = Long.parseLong(sourceKey.replaceAll(applyKey, ""));
		botUserMapper.updateBotUserSelective(new BotUser().setId(sourceUserId).setParentId(botUser.getId()));

		redisCache.delete(sourceKey);
		redisCache.delete(targetKey);

		return BotMessage.simpleTextMessage("合体成功！");
	}

	private BotMessage handleApply(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		BotUser botUser = messageAction.getBotUser();
		Asserts.notEquals(botUser.getType(), 0, "无需合体");
		String qqStr = messageAction.getValue();
		Asserts.isNumber(qqStr, "格式错啦(QQ号)");
		long qq = Long.parseLong(qqStr);
		BotUser targetBotUser = botUserMapper.getBotUserByExternalIdAndType(qq, 0);

		String sourceKey = applyKey + botUser.getId();
		String targetKey = applyKey + targetBotUser.getId();
		Asserts.isFalse(redisCache.exists(sourceKey), "你已经申请啦");
		Asserts.isFalse(redisCache.exists(targetKey), "他还在抉择中");

		List<BotForwardConfig> forwardConfigList = botForwardConfigMapper.getBotForwardConfigByCondition(new BotForwardConfigQuery().setSourceSenderId(botSender.getId()).setStatus(0));
		Asserts.notEmpty(forwardConfigList, "无权限");
		BotForwardConfig forwardConfig = forwardConfigList.get(0);
		Long targetSenderId = forwardConfig.getTargetSenderId();

		redisCache.setValue(sourceKey, targetKey);
		redisCache.setValue(targetKey, sourceKey);

		String sourceName = String.join("-", forwardConfig.getSourceName(), botSender.getName());
		return BotMessage.simpleListMessage(Arrays.asList(
				BotMessageChain.ofAt(qq),
				BotMessageChain.ofPlain(String.format("来自%s的%s申请和你合体。(合体！/我拒绝)",sourceName, botUser.getName()))
		)).setSenderId(targetSenderId);
	}
}
