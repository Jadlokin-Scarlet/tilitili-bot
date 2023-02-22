package com.tilitili.bot.service.mirai;

import com.google.common.collect.Lists;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.FunctionTalkService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.constant.BotItemConstant;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.constant.FavoriteConstant;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.emnus.FavoriteEnum;
import com.tilitili.common.entity.*;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotFavoriteTalkQuery;
import com.tilitili.common.entity.query.BotItemQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.BotMessageNode;
import com.tilitili.common.manager.BotFavoriteManager;
import com.tilitili.common.manager.BotUserItemMappingManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.*;
import com.tilitili.common.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class FavoriteHandle extends ExceptionRespMessageHandle {
	private final RedisCache redisCache;
	private final BotItemMapper botItemMapper;
	private final ForwardMarkHandle forwardMarkHandle;
	private final BotFavoriteMapper botFavoriteMapper;
	private final BotFavoriteManager botFavoriteManager;
	private final BotFavoriteTalkMapper botFavoriteTalkMapper;
	private final BotUserItemMappingManager botUserItemMappingManager;
	private final BotFavoriteActionAddMapper botFavoriteActionAddMapper;

	private final Random random;
	private final BotUserManager botUserManager;
	private final BotUserConfigMapper botUserConfigMapper;
	private final FunctionTalkService functionTalkService;

	@Autowired
	public FavoriteHandle(RedisCache redisCache, BotItemMapper botItemMapper, BotFavoriteMapper botFavoriteMapper, BotFavoriteManager botFavoriteManager, BotFavoriteTalkMapper botFavoriteTalkMapper, BotUserItemMappingManager botUserItemMappingManager, BotFavoriteActionAddMapper botFavoriteActionAddMapper, ForwardMarkHandle forwardMarkHandle, BotUserManager botUserManager, BotUserConfigMapper botUserConfigMapper, FunctionTalkService functionTalkService) {
		this.redisCache = redisCache;
		this.botItemMapper = botItemMapper;
		this.botUserManager = botUserManager;
		this.botFavoriteMapper = botFavoriteMapper;
		this.botFavoriteManager = botFavoriteManager;
		this.botFavoriteTalkMapper = botFavoriteTalkMapper;
		this.botUserItemMappingManager = botUserItemMappingManager;
		this.botFavoriteActionAddMapper = botFavoriteActionAddMapper;
		this.forwardMarkHandle = forwardMarkHandle;
		this.botUserConfigMapper = botUserConfigMapper;
		this.functionTalkService = functionTalkService;
		this.random = new Random(System.currentTimeMillis());
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
//		BotUserDTO botUser = messageAction.getBotUser();

		// 其他平台必须绑定主账号
//		if (BotUserConstant.USER_TYPE_QQ != botUser.getType()) {
//			return null;
//		}

		switch (messageAction.getKeyWithoutPrefix()) {
			case "认领老婆": return handleStart(messageAction);
			case "赠送": return handleGift(messageAction);
			case "好感度查询": return handleQuery(messageAction);
			case "抽礼物": return handleBuyGift(messageAction);
			default: return handleAction(messageAction);
		}
	}

	private BotMessage handleBuyGift(BotMessageAction messageAction) {
		BotUserDTO botUser = messageAction.getBotUser();
		List<BotItem> giftItemList = botItemMapper.getBotItemByCondition(new BotItemQuery().setBag(BotItemConstant.GIFT_BAG));
		Asserts.notEmpty(giftItemList, "啊嘞，礼物呢。");
		BotItem giftItem = giftItemList.get(ThreadLocalRandom.current().nextInt(giftItemList.size()));

		int price = 100;
		Asserts.isTrue(botUser.getScore() >= price, "啊嘞，积分不够了。");
		Integer updateScore = botUserManager.safeUpdateScore(botUser, -price);
		Asserts.checkEquals(updateScore, -price, "啊嘞，不对劲");

		Integer upd = botUserItemMappingManager.addMapping(new BotUserItemMapping().setUserId(botUser.getId()).setItemId(giftItem.getId()).setNum(1));
		Asserts.checkEquals(upd, 1, "啊嘞，不对劲");

		String reply = String.format("抽到一个%s，%s(消耗%d)", giftItem.getName(), giftItem.getDescription(), price);
		if (giftItem.getIcon() == null) {
			return BotMessage.simpleTextMessage(reply);
		} else {
			return BotMessage.simpleImageTextMessage(reply, giftItem.getIcon());
		}
	}

	private BotMessage handleQuery(BotMessageAction messageAction) {
		Long userId = messageAction.getBotUser().getId();
		BotFavorite botFavorite = botFavoriteMapper.getBotFavoriteByUserId(userId);
		Asserts.notNull(botFavorite, "先认领老婆喵");
		int favorite = botFavorite.getFavorite();
		String level = botFavorite.getLevel();
		int favoriteLimit = FavoriteEnum.getFavoriteLimit(level);
		String limitStr = favorite == favoriteLimit? "，好感度已满，达成一定条件即可更进一步喵": "";
		return BotMessage.simpleTextMessage(String.format("当前好感度为%s(%s)%s", favorite, level, limitStr));
	}

	private BotMessage handleGift(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();
		String itemName = messageAction.getValue();

		BotFavorite botFavorite = botFavoriteMapper.getBotFavoriteByUserId(userId);
		if (botFavorite == null) {
			return null;
		}
		String level = botFavorite.getLevel();
		FavoriteEnum favoriteEnum = FavoriteEnum.getFavoriteByLevel(level);
		Asserts.notNull(favoriteEnum, "啊嘞，不对劲。");

		BotItem botItem = botItemMapper.getBotItemByName(itemName);
		Asserts.notNull(botItem, "那是啥。");

		// 获取对话
		List<BotFavoriteTalk> favoriteTalkList = botFavoriteTalkMapper.getBotFavoriteTalkByCondition(new BotFavoriteTalkQuery().setType(FavoriteConstant.TYPE_ITEM).setAction(itemName).setStatus(0));
		List<BotFavoriteTalk> filterFavoriteTalkList = favoriteTalkList.stream().filter(talk -> talk.getComplexResp() != null || talk.getResp() != null).collect(Collectors.toList());
		if (filterFavoriteTalkList.isEmpty()) {
			return null;
		}

		BotFavoriteActionAdd favoriteActionAdd = botFavoriteActionAddMapper.getBotFavoriteActionAddByActionAndLevelAndType(itemName, FavoriteEnum.strange.getLevel(), FavoriteConstant.TYPE_ITEM);
		if (favoriteActionAdd == null || !FavoriteConstant.TYPE_ITEM.equals(favoriteActionAdd.getType())) {
			return null;
		}

		Integer subNum = botUserItemMappingManager.addMapping(new BotUserItemMapping().setUserId(userId).setItemId(botItem.getId()).setNum(-1));
		Asserts.checkEquals(subNum, -1, "啊嘞，没有道具惹。");

		// 凌晨4点刷新
//		String dayStr = DateUtils.formatDateYMD(DateUtils.addTime(new Date(), Calendar.HOUR_OF_DAY, -4));
		// 每个人每天每个道具只能加一次好感度
//		String redisKey = String.format("favorite-%s-%s-%s", dayStr, userId, itemName);
		Integer addFavorite = 0;
		String externalText = "";
//		if (!redisCache.exists(redisKey)) {
		addFavorite = botFavoriteManager.addFavorite(userId, favoriteActionAdd.getFavorite());
//			if (addFavorite != 0) {
//				redisCache.setValue(redisKey, "yes", Math.toIntExact(TimeUnit.DAYS.toSeconds(1)));
//			}
		FavoriteEnum previousFavoriteEnum = FavoriteEnum.getFavoriteById(favoriteEnum.getId() - 1);
		if (previousFavoriteEnum != null && botFavorite.getFavorite() + addFavorite < previousFavoriteEnum.getFavorite()) {
			externalText = String.format("(关系下降为%s)", previousFavoriteEnum.getLevel());
		}

		if (FavoriteEnum.strange.getLevel().equals(botFavorite.getLevel()) && BotItemConstant.FAVORITE_ICE_CREAM.equals(botItem.getId())) {
			Integer favorite = botFavorite.getFavorite();
			int favoriteLimit = favoriteEnum.getFavorite();
			if (favorite + favoriteActionAdd.getFavorite() > favoriteLimit) {
				FavoriteEnum lastFavoriteEnum = FavoriteEnum.passer;
				botFavoriteMapper.updateBotFavoriteSelective(new BotFavorite().setId(botFavorite.getId()).setLevel(lastFavoriteEnum.getLevel()));
				externalText = String.format("(关系提升为%s)", lastFavoriteEnum.getLevel());
			}
		}

		if (FavoriteEnum.anti.getLevel().equals(botFavorite.getLevel()) && BotItemConstant.FAVORITE_SNACK_BOX.equals(botItem.getId())) {
			FavoriteEnum lastFavoriteEnum = FavoriteEnum.strange;
			botFavoriteMapper.updateBotFavoriteSelective(new BotFavorite().setId(botFavorite.getId()).setLevel(lastFavoriteEnum.getLevel()).setFavorite(FavoriteEnum.anti.getFavorite()));
			externalText = String.format("(关系提升为%s)", lastFavoriteEnum.getLevel());
			filterFavoriteTalkList = Collections.singletonList(new BotFavoriteTalk().setResp("只有这一次哦，如果还有下一次，再也不原谅你了哦。"));
		}
//		}

		List<BotMessageChain> respChainList = this.randomTalkToMessageChain(messageAction, botFavorite, filterFavoriteTalkList, addFavorite, externalText);
		return BotMessage.simpleListMessage(respChainList);
	}

	private BotMessage handleAction(BotMessageAction messageAction) {
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();
		String action = messageAction.getText();

		// 没解锁的跳过判定
		BotFavorite botFavorite = botFavoriteMapper.getBotFavoriteByUserId(userId);
		if (botFavorite == null) {
			return null;
		}
		String name = botFavorite.getName();
		String level = botFavorite.getLevel();
		FavoriteEnum favoriteEnum = FavoriteEnum.getFavoriteByLevel(level);
		Asserts.notNull(favoriteEnum, "啊嘞，不对劲。");

		if (action.equals(name)) {
			action = "交谈";
		}

		// 触发对话的关键词不会太长
		if (action.length() > 5) {
			return null;
		}

		// 获取对话
		List<BotFavoriteTalk> favoriteTalkList = botFavoriteTalkMapper.getBotFavoriteTalkByCondition(new BotFavoriteTalkQuery().setType(FavoriteConstant.TYPE_ACTION).setAction(action).setLevel(level).setStatus(0));
		List<BotFavoriteTalk> filterFavoriteTalkList = favoriteTalkList.stream().filter(talk -> talk.getComplexResp() != null || talk.getResp() != null).collect(Collectors.toList());
		if (filterFavoriteTalkList.isEmpty()) {
			return null;
		}

		// 获取好感度增量
		Integer addFavorite = 0;
		String externalText = "";
		BotFavoriteActionAdd favoriteActionAdd = botFavoriteActionAddMapper.getBotFavoriteActionAddByActionAndLevelAndType(action, level, FavoriteConstant.TYPE_ACTION);
		if (favoriteActionAdd != null && FavoriteConstant.TYPE_ACTION.equals(favoriteActionAdd.getType())) {
			// 凌晨4点刷新
			String dayStr = DateUtils.formatDateYMD(DateUtils.addTime(new Date(), Calendar.HOUR_OF_DAY, -4));
			// 每个人每天每个动作只能加一次好感度
			String redisKey = String.format("favorite-%s-%s-%s", dayStr, userId, action);
			if (!redisCache.exists(redisKey)) {
				addFavorite = botFavoriteManager.addFavorite(userId, favoriteActionAdd.getFavorite());
				redisCache.increment(redisKey);
				redisCache.expire(redisKey, Math.toIntExact(TimeUnit.DAYS.toSeconds(1)));

				FavoriteEnum previousFavoriteEnum = FavoriteEnum.getFavoriteById(favoriteEnum.getId() - 1);
				if (previousFavoriteEnum != null && botFavorite.getFavorite() + addFavorite < previousFavoriteEnum.getFavorite()) {
					externalText = String.format("(关系下降为%s)", previousFavoriteEnum.getLevel());
				}
			}
		}

		List<BotMessageChain> respChainList = this.randomTalkToMessageChain(messageAction, botFavorite, filterFavoriteTalkList, addFavorite, externalText);
		return BotMessage.simpleListMessage(respChainList);
	}

	private List<BotMessageChain> randomTalkToMessageChain(BotMessageAction messageAction, BotFavorite botFavorite, List<BotFavoriteTalk> filterFavoriteTalkList, Integer addFavorite, String externalText) {
		BotSender botSender = messageAction.getBotSender();
		String name = botFavorite.getName();

		List<BotMessageChain> respChainList = new ArrayList<>();
		BotFavoriteTalk favoriteTalk = filterFavoriteTalkList.get(random.nextInt(filterFavoriteTalkList.size()));
		if (favoriteTalk.getComplexResp() != null) {
			String resp = favoriteTalk.getComplexResp();
			// 插入好感度变动
			if (addFavorite != 0) {
				resp += String.format("(好感度%+d)", addFavorite);
			}

			// 插入好感度阶段变动
			if (StringUtils.isNotBlank(externalText)) {
				resp += externalText;
			}

			// 处理剧本
			resp = this.replaceResp(messageAction, name, resp);
			List<BotMessageNode> nodeList = this.getForwardMessageByText(botSender, resp, name);

			// 如果剧本就一个节点，就用普通模式回答
			if (nodeList.size() == 1) {
				BotMessageNode node = nodeList.get(0);
				respChainList.add(BotMessageChain.ofSpeaker(node.getSenderName()));
				respChainList.addAll(node.getMessageChain());
			} else {
				respChainList.add(BotMessageChain.ofForward(nodeList));
			}
		} else if (favoriteTalk.getResp() != null) {
			String resp = favoriteTalk.getResp();
			resp = this.replaceResp(messageAction, name, resp);
			respChainList.add(BotMessageChain.ofSpeaker(name));
			respChainList.add(BotMessageChain.ofPlain(resp));
			if (addFavorite != 0) {
				respChainList.add(BotMessageChain.ofPlain(String.format("(好感度%+d)", addFavorite)));
			}
			if (StringUtils.isNotBlank(externalText)) {
				respChainList.add(BotMessageChain.ofPlain(externalText));
			}
		}
		return respChainList;
	}

	private String replaceResp(BotMessageAction messageAction, String name, String resp) {
		BotUserDTO botUser = messageAction.getBotUser();

		String favoriteUserIdStr = botUserConfigMapper.getValueByUserIdAndKey(botUser.getId(), ConfigHandle.favoriteUserIdKey);

		resp = resp.replaceAll("\\{name}", name);
		resp = resp.replaceAll("\\{master}", botUser.getName());
		if (favoriteUserIdStr == null) {
			resp = resp.replaceAll("\\{botQQ}", "1");
		} else {
			resp = resp.replaceAll("\\{botQQ}", favoriteUserIdStr);
		}
		resp = resp.replaceAll("\\{masterQQ}", String.valueOf(botUser.getId()));
		resp = resp.replaceAll("\\{narration}", "0");
		resp = resp.replaceAll("\\{timeTalk}", TimeUtil.getTimeTalk());
		return resp;
	}

	public List<BotMessageNode> getForwardMessageByText(BotSender botSender, String body, String customBotName) {
		String[] rowList = body.split("\n");

		List<BotMessageNode> nodeList = new ArrayList<>();
		for (int i = 0; i < rowList.length; i++) {
			String row = rowList[i];
			List<String> cellList = StringUtils.extractList("(\\d+)[：:](.+)", row);
			Asserts.checkEquals(cellList.size(), 2, "第%s句格式错啦", i);
			long userId = Long.parseLong(cellList.get(0));
			String text = cellList.get(1);
			List<BotMessageChain> botMessageChains = functionTalkService.convertCqToMessageChain(botSender, text);

			if (userId == 0) {
				nodeList.add(new BotMessageNode().setSenderName("旁白").setMessageChain(botMessageChains));
			} else if (userId == 1) {
				Asserts.notNull(customBotName, "啊嘞，不对劲");
				nodeList.add(new BotMessageNode().setSenderName(customBotName).setMessageChain(botMessageChains));
			} else {
				BotUserDTO botUser = botUserManager.getBotUserByIdWithParent(userId);
				Asserts.notNull(botUser, "第%s句找不到人", i);
				nodeList.add(new BotMessageNode().setUserId(botUser.getId()).setSenderName(botUser.getName()).setMessageChain(botMessageChains));
			}
		}
		return nodeList;
	}

	private BotMessage handleStart(BotMessageAction messageAction) {
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();
		BotFavorite favorite = botFavoriteMapper.getBotFavoriteByUserId(userId);
		Asserts.checkNull(favorite, "不准花心喵");

		String name = messageAction.getValue();
		Asserts.notBlank(name, "格式错啦（名）");
		Asserts.checkNull(botFavoriteMapper.getBotFavoriteByName(name), "%s已经被认领啦", name);

		int addCnt = botFavoriteMapper.addBotFavoriteSelective(new BotFavorite().setUserId(userId).setName(name).setFavorite(0).setLevel(FavoriteEnum.strange.getLevel()));
		Asserts.notEquals(addCnt, 0, "认领失败惹");

		String tips = botUser.getType() == BotUserConstant.USER_TYPE_QQ? "": "(tips：有共同群聊最好先申请合体再领。";
		return BotMessage.simpleTextMessage(String.format("你好，初次见面，我叫%s。%s", name, tips));
	}

//	private BotMessage handleStart(BotMessageAction messageAction) {
//		BotSender botSender = messageAction.getBotSender();
//		BotUserDTO botUser = messageAction.getBotUser();
//		Long userId = botUser.getId();
//		List<Long> atList = messageAction.getAtList();
//		atList.retainAll(BotUserConstant.BOT_USER_ID_LIST);
//		boolean hasAtBot = !atList.isEmpty();
//		boolean isFriend = SendTypeEnum.FRIEND_MESSAGE_STR.equals(botSender.getSendType());
//		if (hasAtBot || isFriend) {
//			Asserts.checkEquals(botUser.getType(), BotUserConstant.USER_TYPE_QQ, "未绑定");
//
//			BotFavorite favorite = botFavoriteMapper.getBotFavoriteByUserId(userId);
//			Asserts.checkNull(favorite, "已经开启了");
//			int addCnt = botFavoriteMapper.addBotFavoriteSelective(new BotFavorite().setUserId(userId).setFavorite(FavoriteEnum.strange.getFavorite()).setLevel(FavoriteEnum.strange.getLevel()));
//			Asserts.notEquals(addCnt, 0, "启用失败");
//			if (addCnt != 0) {
//				return BotMessage.simpleTextMessage("你好，初次见面，我叫琪露诺。(好感度启用)");
//			}
//		}
//		return null;
//	}
}
