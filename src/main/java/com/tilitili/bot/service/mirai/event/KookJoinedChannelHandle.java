package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.KookAutoEventHandle;
import com.tilitili.common.constant.BotTaskConstant;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.BotForwardConfig;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.query.BotForwardConfigQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.mirai.event.KookJoinedChannel;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotSenderTaskMappingManager;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.mapper.mysql.BotUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class KookJoinedChannelHandle extends KookAutoEventHandle<KookJoinedChannel> {
	private final BotSenderMapper botSenderMapper;
	private final BotUserMapper botUserMapper;
	private final BotForwardConfigMapper botForwardConfigMapper;
	private final BotSenderTaskMappingManager botSenderTaskMappingManager;
	private final BotManager botManager;

	@Autowired
	public KookJoinedChannelHandle(BotSenderMapper botSenderMapper, BotUserMapper botUserMapper, BotForwardConfigMapper botForwardConfigMapper, BotSenderTaskMappingManager botSenderTaskMappingManager, BotManager botManager) {
		super(KookJoinedChannel.class);
		this.botSenderMapper = botSenderMapper;
		this.botUserMapper = botUserMapper;
		this.botForwardConfigMapper = botForwardConfigMapper;
		this.botSenderTaskMappingManager = botSenderTaskMappingManager;
		this.botManager = botManager;
	}

	@Override
	public void handleEvent(BotEmum bot, KookJoinedChannel event) throws Exception {
		log.info(Gsons.toJson(event));
		Long userId = event.getUserId();
		Long channelId = event.getChannelId();

		BotSender botSender = botSenderMapper.getBotSenderByKookChannelId(channelId);
		Asserts.notNull(botSender, "找不到频道");
		BotUser botUser = botUserMapper.getBotUserByExternalId(userId);
//		if (botUser == null) {
//			botManager.getMemberInfo(bot, )
//		}
		Asserts.notNull(botUser, "找不到用户");

		Asserts.isTrue(botSenderTaskMappingManager.checkSenderHasTask(botSender.getId(), BotTaskConstant.helpTaskId), "无帮助权限");
		Asserts.isTrue(botSenderTaskMappingManager.checkSenderHasTask(botSender.getId(), BotTaskConstant.ForwardTaskId), "无转发权限");

		List<BotForwardConfig> forwardConfigList = botForwardConfigMapper.getBotForwardConfigByCondition(new BotForwardConfigQuery().setSourceSenderId(botSender.getId()));

		for (BotForwardConfig forwardConfig : forwardConfigList) {
			Long targetSenderId = forwardConfig.getTargetSenderId();
			BotSender targetSender = botSenderMapper.getBotSenderById(targetSenderId);

			Asserts.isTrue(botSenderTaskMappingManager.checkSenderHasTask(targetSender.getId(), BotTaskConstant.helpTaskId), "无帮助权限");

			String sourceNameStr = forwardConfig.getSourceName() != null? forwardConfig.getSourceName() + "-": "";
			botManager.sendMessage(BotMessage.simpleTextMessage(String.format("%s加入了语音频道%s%s", botUser.getName(), sourceNameStr, botSender.getName())).setBotSender(targetSender));
		}
	}
}