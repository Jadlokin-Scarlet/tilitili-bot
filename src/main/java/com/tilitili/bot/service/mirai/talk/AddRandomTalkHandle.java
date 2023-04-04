package com.tilitili.bot.service.mirai.talk;

import com.tilitili.bot.entity.ExcelResult;
import com.tilitili.bot.entity.FishConfigDTO;
import com.tilitili.bot.entity.RandomTalkDTO;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.FunctionTalkService;
import com.tilitili.bot.service.mirai.base.BaseMessageHandleAdapt;
import com.tilitili.bot.util.ExcelUtil;
import com.tilitili.common.constant.BotItemConstant;
import com.tilitili.common.constant.BotPlaceConstant;
import com.tilitili.common.emnus.SendTypeEnum;
import com.tilitili.common.entity.*;
import com.tilitili.common.entity.query.BotFunctionTalkQuery;
import com.tilitili.common.entity.query.BotSenderQuery;
import com.tilitili.common.entity.query.FishConfigQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotPlaceManager;
import com.tilitili.common.mapper.mysql.*;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StreamUtil;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AddRandomTalkHandle extends BaseMessageHandleAdapt {
	private final BotManager botManager;
	private final BotSenderMapper botSenderMapper;
	private final BotFunctionMapper botFunctionMapper;
	private final BotFunctionTalkMapper botFunctionTalkMapper;
	private final FunctionTalkService functionTalkService;
	private final FishConfigMapper fishConfigMapper;
	private final BotItemMapper botItemMapper;
	private final BotPlaceManager botPlaceManager;

	@Autowired
	public AddRandomTalkHandle(BotManager botManager, BotSenderMapper botSenderMapper, BotFunctionMapper botFunctionMapper, BotFunctionTalkMapper BotFunctionTalkMapper, FunctionTalkService functionTalkService, FishConfigMapper fishConfigMapper, BotItemMapper botItemMapper, BotPlaceManager botPlaceManager) {
		this.botManager = botManager;
		this.botSenderMapper = botSenderMapper;
		this.botFunctionMapper = botFunctionMapper;
		this.botFunctionTalkMapper = BotFunctionTalkMapper;
		this.functionTalkService = functionTalkService;
		this.fishConfigMapper = fishConfigMapper;
		this.botItemMapper = botItemMapper;
		this.botPlaceManager = botPlaceManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		if (!SendTypeEnum.GROUP_MESSAGE_STR.equals(messageAction.getBotSender().getSendType())) {
			return null;
		}
		List<BotMessageChain> chainList = messageAction.getBotMessage().getBotMessageChainList();
		BotMessageChain fileChain = chainList.stream().filter(StreamUtil.isEqual(BotMessageChain::getType, BotMessage.MESSAGE_TYPE_FILE)).findFirst().orElse(null);
		if (fileChain == null) {
			return null;
		}
		String fileName = fileChain.getName();

		if (fileName.startsWith("钓鱼奖励配置")) {
			return this.handleFishFile(messageAction, fileChain);
		} else if (fileName.startsWith("随机对话模板")) {
			return this.handleRandomTalkFile(messageAction, fileChain);
		} else {
			return null;
		}
	}

	private BotMessage handleRandomTalkFile(BotMessageAction messageAction, BotMessageChain fileChain) {
		BotRobot bot = messageAction.getBot();
		String fileId = fileChain.getId();
		Asserts.notNull(fileId, "啊嘞，找不到文件。");
		File file = botManager.downloadGroupFile(bot, messageAction.getBotSender(), fileId);
		ExcelResult<RandomTalkDTO> excelResult = ExcelUtil.getListFromExcel(file, RandomTalkDTO.class);
		List<RandomTalkDTO> resultList = excelResult.getResultList();
		String function = excelResult.getParam("分组");
		String friendList = excelResult.getParam("私聊");
		String groupList = excelResult.getParam("群号");
		String guildList = excelResult.getParam("频道");
		String channelList = excelResult.getParam("子频道");
		String scoreStr = excelResult.getParamOrDefault("积分", "0");
		String timeUnit = excelResult.getParamOrDefault("每", "天");
		String timeNumStr = excelResult.getParamOrDefault("次数", "99999999");
		Asserts.notBlank(function, "分组不能为空");
		Asserts.isNumber(scoreStr, "积分请输入正整数");
		Asserts.isNumber(timeNumStr, "次数请输入正整数");
		int score = Integer.parseInt(scoreStr);
		int timeNum = Integer.parseInt(timeNumStr);

		BotFunction oldFunction = botFunctionMapper.getLastFunction(function);
		BotFunction newFunction = new BotFunction().setFunction(function).setScore(score).setTimeNum(timeNum);
		botFunctionMapper.addBotFunctionSelective(newFunction);
		List<BotSender> botSenderList = new ArrayList<>();
		if (StringUtils.isNotBlank(friendList)) {
			botSenderList.addAll(Arrays.stream(friendList.split(",")).map(Long::valueOf)
					.map(botSenderMapper::getBotSenderByQq)
					.filter(Objects::nonNull).collect(Collectors.toList()));
		}
		if (StringUtils.isNotBlank(groupList)) {
			botSenderList.addAll(Arrays.stream(groupList.split(",")).map(Long::valueOf)
					.map(botSenderMapper::getBotSenderByGroup)
					.filter(Objects::nonNull).collect(Collectors.toList()));
		}
		if (StringUtils.isNotBlank(guildList)) {
			botSenderList.addAll(Arrays.stream(guildList.split(",")).map(Long::valueOf)
					.flatMap(guildId -> botSenderMapper.getBotSenderByCondition(new BotSenderQuery().setGuildId(guildId).setStatus(0)).stream())
					.filter(Objects::nonNull).collect(Collectors.toList()));
		}
		if (StringUtils.isNotBlank(channelList)) {
			botSenderList.addAll(Arrays.stream(channelList.split(",")).map(Long::valueOf)
					.map(botSenderMapper::getBotSenderByChannelId)
					.filter(Objects::nonNull).collect(Collectors.toList()));
		}
		List<BotFunctionTalk> newFunctionTalkList = new ArrayList<>();
		for (RandomTalkDTO randomTalkDTO : resultList) {
			Asserts.notBlank(randomTalkDTO.getReq(), "关键词不能为空");
			Asserts.notBlank(randomTalkDTO.getResp(), "回复不能为空");
			if (botSenderList.isEmpty()) {
				continue;
			}
			for (BotSender botSender : botSenderList) {
				BotFunctionTalk newFunctionTalk = new BotFunctionTalk().setReq(randomTalkDTO.getReq()).setResp(randomTalkDTO.getResp()).setFunction(function).setFunctionId(newFunction.getId()).setSenderId(botSender.getId()).setBlackList(randomTalkDTO.getBlackList());
				newFunctionTalkList.add(newFunctionTalk);
			}
		}
		if (oldFunction != null) {
			List<BotFunctionTalk> oldFunctionTalkList = botFunctionTalkMapper.getBotFunctionTalkByCondition(new BotFunctionTalkQuery().setFunctionId(oldFunction.getId()));

			for (BotFunctionTalk oldFunctionTalk : oldFunctionTalkList) {
				botFunctionTalkMapper.updateBotFunctionTalkSelective(new BotFunctionTalk().setId(oldFunctionTalk.getId()).setStatus(-1));
			}
		}
		for (BotFunctionTalk newFunctionTalk : newFunctionTalkList) {
			botFunctionTalkMapper.addBotFunctionTalkSelective(newFunctionTalk);
		}

		String senderMessage = (StringUtils.isNotBlank(friendList)? "私聊"+friendList: "") +
				(StringUtils.isNotBlank(groupList)? "群号"+groupList: "") +
				(StringUtils.isNotBlank(guildList)? "频道"+guildList: "") +
				(StringUtils.isNotBlank(channelList)? "子频道"+channelList: "");
		String scoreMessage = score == 0? "": String.format("，积分%s", score);
		String timeNumMessage = timeNum == 99999999? "": "，每" + timeUnit + timeNumStr + "次";
		return BotMessage.simpleTextMessage(String.format("搞定√(分组%s导入%s条对话，%s%s%s)", function, newFunctionTalkList.size(), senderMessage, scoreMessage, timeNumMessage));
	}

	private BotMessage handleFishFile(BotMessageAction messageAction, BotMessageChain fileChain) {
		String fileId = fileChain.getId();
		Asserts.notNull(fileId, "啊嘞，找不到文件。");
		File file = botManager.downloadGroupFile(messageAction.getBot(), messageAction.getBotSender(), fileId);
		ExcelResult<FishConfigDTO> excelResult = ExcelUtil.getListFromExcel(file, FishConfigDTO.class);
		List<FishConfigDTO> resultList = excelResult.getResultList();
		log.info("{}", resultList);
		int rateSum = 0;
		List<FishConfig> newFishConfigList = new ArrayList<>();
		for (FishConfigDTO config : resultList) {
			try {
				int scale = "小".equals(config.getScaleStr()) ? 0 : 1;
				String place = config.getPlace();
				Integer cost = config.getCost();
				Integer rate = config.getRate();
				Integer price = config.getPrice();
				String type = config.getType();
				Asserts.notNull(cost, "格式错啦(cost)");
				Asserts.notNull(rate, "格式错啦(rate)");
				Asserts.notNull(type, "格式错啦(type)");
				if (rate == 0) {
					continue;
				}
				Long placeId;
				if (place != null) {
					BotPlace botPlace = botPlaceManager.getBotPlaceByPlaceCache(place);
					Asserts.notNull(botPlace, "未知区域(%s)", place);
					placeId = botPlace.getId();
				} else {
					placeId = BotPlaceConstant.PLACE_FIRST_FISH;
				}
				rateSum += rate;
				if ("事件".equals(type)) {
					String desc = config.getDesc();
					String icon = config.getImage();
					newFishConfigList.add(new FishConfig().setPlaceId(placeId).setDescription(desc).setScale(scale).setCost(cost).setRate(rate).setPrice(price).setIcon(icon));
				} else {
					Asserts.notNull(price, "格式错啦(price)");
					String itemName = config.getItemName();
					String itemDesc = config.getItemDesc();
					String itemGrade = config.getItemGrade();
					String itemIcon = config.getImage();
					Asserts.notNull(itemName, "格式错啦(itemName)");
					Asserts.notNull(itemDesc, "格式错啦(itemDesc)");
					Asserts.notNull(itemGrade, "格式错啦(itemGrade)");
					Asserts.notNumber(itemName, "格式错啦，道具名称不能是数字");
					Asserts.isFalse(itemName.contains(" *，,、"), "格式错啦，道具名称不能有[ *，,、]符号");
					BotItem botItem = botItemMapper.getBotItemByName(itemName);
					if (botItem == null) {
						botItem = new BotItem().setName(itemName).setDescription(itemDesc).setSellPrice(price).setGrade(itemGrade).setIcon(itemIcon).setBag(BotItemConstant.FISH_BAG);
						botItemMapper.addBotItemSelective(botItem);
					} else {
						botItemMapper.updateBotItemSelective(new BotItem().setId(botItem.getId()).setDescription(itemDesc).setSellPrice(price).setGrade(itemGrade).setIcon(itemIcon));
					}
					newFishConfigList.add(new FishConfig().setPlaceId(placeId).setItemId(botItem.getId()).setScale(scale).setCost(cost).setRate(rate));
				}
			} catch (AssertException e) {
				return BotMessage.simpleTextMessage(e.getMessage());
			} catch (Exception e) {
				return BotMessage.simpleTextMessage("格式不对");
			}
		}
		for (FishConfig fishConfig : fishConfigMapper.getFishConfigByCondition(new FishConfigQuery().setStatus(0))) {
			fishConfigMapper.updateFishConfigSelective(new FishConfig().setId(fishConfig.getId()).setStatus(-1));
		}
		for (FishConfig fishConfig : newFishConfigList) {
			fishConfigMapper.addFishConfigSelective(fishConfig);
		}
		return BotMessage.simpleTextMessage(String.format("搞定√(导入%s项配置，总权重%s)", newFishConfigList.size(), rateSum));
	}
}
