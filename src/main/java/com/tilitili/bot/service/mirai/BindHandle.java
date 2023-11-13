package com.tilitili.bot.service.mirai;

import com.google.common.collect.Lists;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.constant.BotTaskConstant;
import com.tilitili.common.entity.*;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotForwardConfigQuery;
import com.tilitili.common.entity.query.BotUserSenderMappingQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotSenderTaskMappingManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotCattleMapper;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.mapper.mysql.BotUserSenderMappingMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BindHandle extends ExceptionRespMessageToSenderHandle {
	private static final String applyKey = "BindHandle.apply-";

	private final RedisCache redisCache;
	private final BotUserManager botUserManager;
	private final BotSenderCacheManager botSenderCacheManager;
	private final BotCattleMapper botCattleMapper;
	private final BotForwardConfigMapper botForwardConfigMapper;
	private final BotUserSenderMappingMapper botUserSenderMappingMapper;
	private final BotSenderTaskMappingManager botSenderTaskMappingManager;

	public BindHandle(RedisCache redisCache, BotUserManager botUserManager, BotSenderCacheManager botSenderCacheManager, BotCattleMapper botCattleMapper, BotForwardConfigMapper botForwardConfigMapper, BotUserSenderMappingMapper botUserSenderMappingMapper, BotSenderTaskMappingManager botSenderTaskMappingManager) {
		this.redisCache = redisCache;
		this.botUserManager = botUserManager;
		this.botSenderCacheManager = botSenderCacheManager;
		this.botCattleMapper = botCattleMapper;
		this.botForwardConfigMapper = botForwardConfigMapper;
		this.botUserSenderMappingMapper = botUserSenderMappingMapper;
		this.botSenderTaskMappingManager = botSenderTaskMappingManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		switch (messageAction.getKeyWithoutPrefix()) {
			case "申请合体": return handleApply(messageAction);
			case "合体！": return handleAccept(messageAction);
			default: throw new AssertException();
		}
	}

	private BotMessage handleAccept(BotMessageAction messageAction) {
		BotRobot bot = messageAction.getBot();
		BotUserDTO botUser = messageAction.getBotUser();
		BotSender botSender = messageAction.getBotSender();

		String targetKey = applyKey + botUser.getId();
		if (!redisCache.exists(targetKey)) {
			return null;
		}
		String sourceKey = (String) redisCache.getValue(targetKey);

		Asserts.checkEquals(botUser.getType(), 0, "啊嘞，不对劲");

		long sourceUserId = Long.parseLong(sourceKey.replaceAll(applyKey, ""));
		BotUserDTO sourceBotUser = botUserManager.getValidBotUserByIdWithParent(sourceUserId);
		botUserManager.bindUser(sourceBotUser, botUser);

		redisCache.delete(sourceKey);
		redisCache.delete(targetKey);

		BotCattle sourceCattle = botCattleMapper.getBotCattleByUserId(sourceUserId);
		if (sourceCattle != null) {
			botCattleMapper.updateBotCattleSelective(new BotCattle().setId(sourceCattle.getId()).setStatus(-1));
		}

		return BotMessage.simpleTextMessage("合体成功！");
	}

	private BotMessage handleApply(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		BotUserDTO botUser = messageAction.getBotUser();
		Asserts.notEquals(botUser.getType(), 0, "无需合体");
		String qqStr = messageAction.getValue();
		Asserts.isNumber(qqStr, "格式错啦(QQ号)");
		long qq = Long.parseLong(qqStr);
		BotUserDTO targetBotUser = botUserManager.getValidBotUserByExternalIdWithParent(qq, 0);
		Asserts.notNull(targetBotUser, "查无此人");

		String sourceKey = applyKey + botUser.getId();
		String targetKey = applyKey + targetBotUser.getId();
//		Asserts.isFalse(redisCache.exists(sourceKey), "你已经申请啦");
		Asserts.isFalse(redisCache.exists(targetKey), "他还在抉择中");

		List<BotUserSenderMapping> mappingList = botUserSenderMappingMapper.getBotUserSenderMappingByCondition(new BotUserSenderMappingQuery().setUserId(targetBotUser.getId()));
		BotSender targetSender = null;
		for (BotUserSenderMapping mapping : mappingList) {
			Long senderId = mapping.getSenderId();
			BotSender theTargetSender = botSenderCacheManager.getValidBotSenderById(senderId);
			boolean hasBind = botSenderTaskMappingManager.checkSenderHasTask(senderId, BotTaskConstant.bindTaskId);
			if (hasBind) {
				targetSender = theTargetSender;
				break;
			}
		}
		Asserts.notNull(targetSender, "查无此人");

		if (redisCache.exists(sourceKey)) {
			String otherTargetKey = (String) redisCache.getValue(sourceKey);
			redisCache.delete(sourceKey);
			redisCache.delete(otherTargetKey);
		}
		redisCache.setValue(sourceKey, targetKey, 60 * 60);
		redisCache.setValue(targetKey, sourceKey, 60 * 60);

		List<BotForwardConfig> forwardConfigList = botForwardConfigMapper.getBotForwardConfigByCondition(new BotForwardConfigQuery().setSourceSenderId(botSender.getId()).setStatus(0));

		String sourceName = forwardConfigList.isEmpty()? botSender.getName(): String.join("-", forwardConfigList.get(0).getSourceName(), botSender.getName());
		return BotMessage.simpleListMessage(Lists.newArrayList(
				BotMessageChain.ofAt(targetBotUser),
				BotMessageChain.ofPlain(String.format("来自%s的%s申请和你合体。(合体！/但是我拒绝)",sourceName, botUser.getName()))
		)).setBotSender(targetSender);
	}
}
