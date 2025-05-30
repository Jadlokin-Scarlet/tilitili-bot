package com.tilitili.bot.service.mirai;

import com.google.common.collect.Lists;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.constant.BotTaskConstant;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.entity.BotForwardConfig;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUserSenderMapping;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotForwardConfigQuery;
import com.tilitili.common.entity.query.BotUserQuery;
import com.tilitili.common.entity.query.BotUserSenderMappingQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.*;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
import com.tilitili.common.mapper.mysql.BotUserSenderMappingMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BindHandle extends ExceptionRespMessageToSenderHandle {
	private static final String applyKey = "BindHandle.apply-";

	private final RedisCache redisCache;
	private final BotUserManager botUserManager;
	private final BotSenderCacheManager botSenderCacheManager;
	private final BotForwardConfigMapper botForwardConfigMapper;
	private final BotUserSenderMappingMapper botUserSenderMappingMapper;
	private final BotSenderTaskMappingManager botSenderTaskMappingManager;
	private final BotRoleManager botRoleManager;
	private final BotUserBindManager botUserBindManager;

	public BindHandle(RedisCache redisCache, BotUserManager botUserManager, BotSenderCacheManager botSenderCacheManager, BotForwardConfigMapper botForwardConfigMapper, BotUserSenderMappingMapper botUserSenderMappingMapper, BotSenderTaskMappingManager botSenderTaskMappingManager, BotRoleManager botRoleManager, BotUserBindManager botUserBindManager) {
		this.redisCache = redisCache;
		this.botUserManager = botUserManager;
		this.botSenderCacheManager = botSenderCacheManager;
		this.botForwardConfigMapper = botForwardConfigMapper;
		this.botUserSenderMappingMapper = botUserSenderMappingMapper;
		this.botSenderTaskMappingManager = botSenderTaskMappingManager;
		this.botRoleManager = botRoleManager;
		this.botUserBindManager = botUserBindManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		switch (messageAction.getKeyWithoutPrefix()) {
			case "申请合体": return handleApply(messageAction);
			case "合体！": return handleAccept(messageAction);
			case "合体": return handleAdminBind(messageAction);
			case "绑定码": return handleBindCode(messageAction);
			default: throw new AssertException();
		}
	}

	private BotMessage handleBindCode(BotMessageAction messageAction) {
		BotUserDTO parentUser = messageAction.getBotUser();
		Asserts.checkNull(parentUser.getBindCode(), "你已绑定！");
		Asserts.checkNull(parentUser.getParentId());
		String bindCode = messageAction.getValue();
		Asserts.notNull(bindCode, "格式错啦（绑定码）");
		List<BotUserDTO> bindCodeList = botUserManager.getBotUserByCondition(new BotUserQuery().setBindCode(bindCode));
		// 一个绑定码应该只存在于一个账号关系树中，通过getBotUserByCondition取出来的应该都是数顶点用户，所以去重后应该只剩下一个
		List<BotUserDTO> bindCodeListDistinct = bindCodeList.stream().distinct().collect(Collectors.toList());
		Asserts.checkEquals(bindCodeListDistinct.size(), 1);
		BotUserDTO subUser = bindCodeListDistinct.get(0);
		Asserts.checkNull(subUser.getParentId());
		// ?
		Asserts.notEquals(parentUser.getType(), subUser.getType());

		// 虽然好像没必要，但还是让QQ类型账号作为父账号
		if (subUser.getType() == BotUserConstant.USER_TYPE_QQ) {
			// 父子反转
			botUserBindManager.bindUser(parentUser, subUser);
		} else {
			botUserBindManager.bindUser(subUser, parentUser);
		}

		return BotMessage.simpleTextMessage("绑定成功！");
	}

	private BotMessage handleAdminBind(BotMessageAction messageAction) {
		BotRobot bot = messageAction.getBot();
		BotUserDTO botUser = messageAction.getBotUser();
		boolean canUseBotAdminTask = botRoleManager.canUseBotAdminTask(bot, botUser);
		if (!canUseBotAdminTask) {
			return null;
		}
		String qq = messageAction.getValue();
		Asserts.isNumber(qq, "格式错啦(qq号)");
		BotUserDTO parentUser = botUserManager.getValidBotUserByExternalIdWithParent(BotUserConstant.USER_TYPE_QQ, qq);

		String targetKey = applyKey + parentUser.getId();
		String sourceKey = (String) redisCache.getValue(targetKey);
		long subUserId = Long.parseLong(sourceKey.replaceAll(applyKey, ""));
		BotUserDTO subUser = botUserManager.getValidBotUserByIdWithParent(subUserId);

		Asserts.checkEquals(parentUser.getType(), 0, "啊嘞，不对劲");

		botUserBindManager.bindUser(subUser, parentUser);

		redisCache.delete(sourceKey);
		redisCache.delete(targetKey);

		return BotMessage.simpleTextMessage("合体成功！");
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
		botUserBindManager.bindUser(sourceBotUser, botUser);

		redisCache.delete(sourceKey);
		redisCache.delete(targetKey);

		return BotMessage.simpleTextMessage("合体成功！");
	}

	private BotMessage handleApply(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		BotUserDTO botUser = messageAction.getBotUser();
		Asserts.notEquals(botUser.getType(), 0, "无需合体");
		String qq = messageAction.getValue();
		Asserts.isNumber(qq, "格式错啦(QQ号)");
		BotUserDTO targetBotUser = botUserManager.getValidBotUserByExternalIdWithParent(BotUserConstant.USER_TYPE_QQ, qq);
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
