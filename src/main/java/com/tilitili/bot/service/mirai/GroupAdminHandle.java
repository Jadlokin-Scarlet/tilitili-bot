package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.entity.BotAdminStatistics;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotAdminStatisticsQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotAdminStatisticsMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GroupAdminHandle extends ExceptionRespMessageHandle {
	private final BotManager botManager;
	private final BotUserManager botUserManager;
	private final BotAdminStatisticsMapper botAdminStatisticsMapper;

	@Autowired
	public GroupAdminHandle(BotManager botManager, BotUserManager botUserManager, BotAdminStatisticsMapper botAdminStatisticsMapper) {
		this.botManager = botManager;
		this.botUserManager = botUserManager;
		this.botAdminStatisticsMapper = botAdminStatisticsMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String key = messageAction.getKeyWithoutPrefix();
		switch (key) {
			case "投票管理员": return handleStatistics(messageAction);
			case "管理员": return handleAdmin(messageAction);
			case "取消管理员": return handleDeleteAdmin(messageAction);
		}
		return null;
	}

	private BotMessage handleStatistics(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		BotUserDTO botUser = messageAction.getBotUser();
		BotSender botSender = messageAction.getBotSender();
		List<Long> atList = messageAction.getAtList();

		BotAdminStatistics adminStatistics = botAdminStatisticsMapper.getBotAdminStatisticsBySenderIdAndUserId(botSender.getId(), botUser.getId());
		if (adminStatistics != null) {
			Asserts.isTrue(adminStatistics.getUpdateTime().before(DateUtils.getCurrentDay()), "今天已经投过票啦，不能反悔哦，明天再来吧。");
		}

		Asserts.notEmpty(atList, "谁?");
		Asserts.checkEquals(atList.size(), 1, "不要太贪心哦。");
		Long atUserId = atList.get(0);
		BotUserDTO atUser = botUserManager.getBotUserByIdWithParent(atUserId);

		if (adminStatistics != null) {
			botAdminStatisticsMapper.updateBotAdminStatisticsSelective(new BotAdminStatistics().setId(adminStatistics.getId()).setTargetUserId(atUserId));
		} else {
			botAdminStatisticsMapper.addBotAdminStatisticsSelective(new BotAdminStatistics().setSenderId(botSender.getId()).setUserId(botUser.getId()).setTargetUserId(atUserId));
		}

		List<BotAdminStatistics> adminStatisticsList = botAdminStatisticsMapper.getBotAdminStatisticsByCondition(new BotAdminStatisticsQuery().setSenderId(botSender.getId()));
		Map<Long, Long> statisticsMap = adminStatisticsList.stream().map(BotAdminStatistics::getTargetUserId).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		List<Map.Entry<Long, Long>> sortedStatisticsList = statisticsMap.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).filter(e -> e.getValue() > 2).limit(3).collect(Collectors.toList());

		StringBuilder respBuilder = new StringBuilder();
		respBuilder.append("投票成功");
		if (adminStatistics != null && Objects.equals(adminStatistics.getTargetUserId(), atUserId)) {
			respBuilder.append(String.format("，%s票数+0(当前%s)", atUser.getName(), statisticsMap.getOrDefault(atUserId, 0L)));
		} else {
			respBuilder.append(String.format("，%s票数+1(当前%s)", atUser.getName(), statisticsMap.getOrDefault(atUserId, 0L)));
			if (adminStatistics != null) {
				BotUserDTO oldTargetUser = botUserManager.getBotUserByIdWithParent(adminStatistics.getTargetUserId());
				respBuilder.append(String.format("，%s票数-1(当前%s)", oldTargetUser.getName(), statisticsMap.getOrDefault(oldTargetUser.getId(), 0L)));
			}
		}
		respBuilder.append("。\n");

		if (sortedStatisticsList.isEmpty()) {
			respBuilder.append("还没有人票数达标。");
		} else {
			String adminList = sortedStatisticsList.stream().map(e -> {
				BotUserDTO adminUser = botUserManager.getBotUserByIdWithParent(e.getKey());
				return String.format("%s(%s票)", adminUser.getName(), e.getValue());
			}).collect(Collectors.joining(","));

			respBuilder.append(String.format("下一届管理员为：%s", adminList));
		}

		return BotMessage.simpleTextMessage(respBuilder.toString()).setQuote(messageAction.getMessageId());
	}

	private BotMessage handleAdmin(BotMessageAction messageAction) {
		BotEnum bot = messageAction.getBot();
		BotUserDTO botUser = messageAction.getBotUser();
		BotSender botSender = messageAction.getBotSender();
		List<Long> atList = messageAction.getAtList();

		if (BotUserConstant.MASTER_USER_ID.equals(botUser.getId())) {
			return null;
		}

		Asserts.notEmpty(atList, "谁?");
		for (Long userId : atList) {
			BotUserDTO atBotUser = botUserManager.getBotUserByIdWithParent(userId);
			botManager.setMemberAdmin(bot, botSender, atBotUser, true);
		}
		return BotMessage.simpleTextMessage("好了喵。");
	}

	private BotMessage handleDeleteAdmin(BotMessageAction messageAction) {
		BotEnum bot = messageAction.getBot();
		BotUserDTO botUser = messageAction.getBotUser();
		BotSender botSender = messageAction.getBotSender();
		List<Long> atList = messageAction.getAtList();

		if (BotUserConstant.MASTER_USER_ID.equals(botUser.getId())) {
			return null;
		}

		Asserts.notEmpty(atList, "谁?");
		for (Long userId : atList) {
			BotUserDTO atBotUser = botUserManager.getBotUserByIdWithParent(userId);
			botManager.setMemberAdmin(bot, botSender, atBotUser, false);
		}
		return BotMessage.simpleTextMessage("好了喵。");
	}
}
