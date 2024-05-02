package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.constant.BotConfigConstant;
import com.tilitili.common.constant.BotUserSenderMappingConstant;
import com.tilitili.common.entity.BotAdminStatistics;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUserSenderMapping;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotAdminStatisticsQuery;
import com.tilitili.common.entity.query.BotUserSenderMappingQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotConfigManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotAdminStatisticsMapper;
import com.tilitili.common.mapper.mysql.BotUserSenderMappingMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GroupAdminHandle extends ExceptionRespMessageHandle {
	private final BotManager botManager;
	private final BotUserManager botUserManager;
	private final BotAdminStatisticsMapper botAdminStatisticsMapper;
	private final BotConfigManager botConfigManager;
	private final BotUserSenderMappingMapper botUserSenderMappingMapper;

	@Autowired
	public GroupAdminHandle(BotManager botManager, BotUserManager botUserManager, BotAdminStatisticsMapper botAdminStatisticsMapper, BotConfigManager botConfigManager, BotUserSenderMappingMapper botUserSenderMappingMapper) {
		this.botManager = botManager;
		this.botUserManager = botUserManager;
		this.botAdminStatisticsMapper = botAdminStatisticsMapper;
		this.botConfigManager = botConfigManager;
		this.botUserSenderMappingMapper = botUserSenderMappingMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String key = messageAction.getKeyWithoutPrefix();
		switch (key) {
			case "弃权管理员": return handleDeleteAdmin(messageAction);
			case "投票管理员": return handleStatistics(messageAction);
//			case "管理员": return handleAdmin(messageAction);
		}
		return null;
	}

	private BotMessage handleStatistics(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		BotUserDTO botUser = messageAction.getBotUser();
		BotSender botSender = messageAction.getBotSender();
		List<BotUserDTO> atList = messageAction.getAtList();

		BotAdminStatistics adminStatistics = botAdminStatisticsMapper.getBotAdminStatisticsBySenderIdAndUserId(botSender.getId(), botUser.getId());
		if (adminStatistics != null) {
			BotUserDTO targetUser = botUserManager.getValidBotUserByIdWithParent(adminStatistics.getTargetUserId());
			Asserts.isTrue(adminStatistics.getUpdateTime().before(DateUtils.getCurrentDay()), "今天已经投给%s啦，不能反悔哦，明天再来吧。", targetUser.getName());
		}

		Asserts.notEmpty(atList, "谁?");
		Asserts.checkEquals(atList.size(), 1, "不要太贪心哦。");
		BotUserDTO atUser = atList.get(0);
		Long atUserId = atUser.getId();

		if (adminStatistics != null) {
			botAdminStatisticsMapper.updateBotAdminStatisticsSelective(new BotAdminStatistics().setId(adminStatistics.getId()).setTargetUserId(atUserId));
		} else {
			botAdminStatisticsMapper.addBotAdminStatisticsSelective(new BotAdminStatistics().setSenderId(botSender.getId()).setUserId(botUser.getId()).setTargetUserId(atUserId));
		}

		List<BotAdminStatistics> adminStatisticsList = botAdminStatisticsMapper.getBotAdminStatisticsByCondition(new BotAdminStatisticsQuery().setSenderId(botSender.getId()));
		Map<Long, Long> statisticsMap = adminStatisticsList.stream().map(BotAdminStatistics::getTargetUserId).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

		StringBuilder respBuilder = new StringBuilder();
		respBuilder.append("投票成功");
		if (adminStatistics != null && Objects.equals(adminStatistics.getTargetUserId(), atUserId)) {
			respBuilder.append(String.format("，%s票数+0(当前%s)", atUser.getName(), statisticsMap.getOrDefault(atUserId, 0L)));
		} else {
			respBuilder.append(String.format("，%s票数+1(当前%s)", atUser.getName(), statisticsMap.getOrDefault(atUserId, 0L)));
			if (adminStatistics != null) {
				BotUserDTO oldTargetUser = botUserManager.getValidBotUserByIdWithParent(botSender.getId(), adminStatistics.getTargetUserId());
				respBuilder.append(String.format("，%s票数-1(当前%s)", oldTargetUser.getName(), statisticsMap.getOrDefault(oldTargetUser.getId(), 0L)));
			}
		}
		respBuilder.append("。\n");

		List<Map.Entry<Long, Long>> sortedStatisticsList = statisticsMap.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toList());

		int total = botUserSenderMappingMapper.countBotUserSenderMappingByCondition(new BotUserSenderMappingQuery().setSenderId(botSender.getId()));
		// 向上取证
		int adminLimit = total / 10;
		int adminTotal = 10;

		if (sortedStatisticsList.isEmpty()) {
			respBuilder.append("还没有人票数达标。");
		} else {
			int adminCnt = 0;
			respBuilder.append("下一届管理员为：");
			for (Map.Entry<Long, Long> entry : sortedStatisticsList) {
				Long userId = entry.getKey();
				Long cnt = entry.getValue();

				if (cnt <= adminLimit) {
					break;
				}

				BotUserDTO adminUser = botUserManager.getValidBotUserByIdWithParent(botSender.getId(), userId);
				Boolean giveUp = botConfigManager.getBooleanUserConfigCache(userId, BotConfigConstant.giveUpAdminKey);
				respBuilder.append("\n");
				if (giveUp) {
					respBuilder.append(String.format("%s(%s票,弃权)", adminUser.getName(), cnt));
				} else {
					respBuilder.append(String.format("%s(%s票)", adminUser.getName(), cnt));
					if (++adminCnt >= adminTotal) {
						break;
					}
				}
			}
		}

		return BotMessage.simpleTextMessage(respBuilder.toString()).setQuote(messageAction.getMessageId());
	}

	private BotMessage handleAdmin(BotMessageAction messageAction) {
		BotRobot bot = messageAction.getBot();
		BotUserDTO botUser = messageAction.getBotUser();
		BotSender botSender = messageAction.getBotSender();

		if (!Boolean.TRUE.equals(botConfigManager.getBooleanUserConfigCache(botUser.getId(), BotConfigConstant.giveUpAdminKey))) {
			return null;
		}

		botConfigManager.addOrUpdateUserConfig(botUser.getId(), BotConfigConstant.giveUpAdminKey, false);
		return BotMessage.simpleTextMessage("好的喵");
//		List<BotUserDTO> atList = messageAction.getAtList();
//
//		if (!BotUserConstant.MASTER_USER_ID.equals(botUser.getId())) {
//			return null;
//		}
//
//		Asserts.notEmpty(atList, "谁?");
//		for (BotUserDTO atBotUser : atList) {
//			botManager.setMemberAdmin(bot, botSender, atBotUser, true);
//		}
//		return BotMessage.simpleTextMessage("好了喵。");
	}

	private BotMessage handleDeleteAdmin(BotMessageAction messageAction) {
		BotRobot bot = messageAction.getBot();
		BotUserDTO botUser = messageAction.getBotUser();
		BotSender botSender = messageAction.getBotSender();
		String value = messageAction.getValue();

		BotUserSenderMapping mapping = botUserSenderMappingMapper.getBotUserSenderMappingBySenderIdAndUserId(botSender.getId(), botUser.getId());
		boolean isAdmin = BotUserSenderMappingConstant.PERMISSION_ADMIN.equals(mapping.getPermission());
		// 只有no为不弃权，其他都是弃权
		boolean giveUpAdmin = !"no".equals(value);

		if (giveUpAdmin && isAdmin) {
			botManager.setMemberAdmin(bot, botSender, botUser, false);
		}
		botConfigManager.addOrUpdateUserConfig(botUser.getId(), BotConfigConstant.giveUpAdminKey, giveUpAdmin);
		return BotMessage.simpleTextMessage("好的喵");


//		List<BotUserDTO> atList = messageAction.getAtList();
//
//		if (!BotUserConstant.MASTER_USER_ID.equals(botUser.getId())) {
//			return null;
//		}
//
//		Asserts.notEmpty(atList, "谁?");
//		for (BotUserDTO atBotUser : atList) {
//			botManager.setMemberAdmin(bot, botSender, atBotUser, false);
//		}
//		return BotMessage.simpleTextMessage("好了喵。");
	}
}
