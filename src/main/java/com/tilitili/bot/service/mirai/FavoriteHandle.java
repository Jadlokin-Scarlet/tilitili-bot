package com.tilitili.bot.service.mirai;

import com.google.common.collect.Lists;
import com.tilitili.bot.entity.bot.BotMessageAction;
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
import com.tilitili.common.mapper.mysql.BotFavoriteActionAddMapper;
import com.tilitili.common.mapper.mysql.BotFavoriteMapper;
import com.tilitili.common.mapper.mysql.BotFavoriteTalkMapper;
import com.tilitili.common.mapper.mysql.BotItemMapper;
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
	private final BotFavoriteMapper botFavoriteMapper;
	private final BotFavoriteManager botFavoriteManager;
	private final BotFavoriteTalkMapper botFavoriteTalkMapper;
	private final BotUserItemMappingManager botUserItemMappingManager;
	private final BotFavoriteActionAddMapper botFavoriteActionAddMapper;
	private final ForwardMarkHandle forwardMarkHandle;

	private final Random random;
	private final BotUserManager botUserManager;

	@Autowired
	public FavoriteHandle(RedisCache redisCache, BotItemMapper botItemMapper, BotFavoriteMapper botFavoriteMapper, BotFavoriteManager botFavoriteManager, BotFavoriteTalkMapper botFavoriteTalkMapper, BotUserItemMappingManager botUserItemMappingManager, BotFavoriteActionAddMapper botFavoriteActionAddMapper, ForwardMarkHandle forwardMarkHandle, BotUserManager botUserManager) {
		this.redisCache = redisCache;
		this.botItemMapper = botItemMapper;
		this.botFavoriteMapper = botFavoriteMapper;
		this.botFavoriteManager = botFavoriteManager;
		this.botFavoriteTalkMapper = botFavoriteTalkMapper;
		this.botUserItemMappingManager = botUserItemMappingManager;
		this.botFavoriteActionAddMapper = botFavoriteActionAddMapper;
		this.forwardMarkHandle = forwardMarkHandle;
		this.botUserManager = botUserManager;
		this.random = new Random(System.currentTimeMillis());
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotUserDTO botUser = messageAction.getBotUser();

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
		String name = botFavorite.getName();
		String level = botFavorite.getLevel();

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

			if (FavoriteEnum.strange.getLevel().equals(botFavorite.getLevel())) {
				Integer favorite = botFavorite.getFavorite();
				FavoriteEnum favoriteEnum = FavoriteEnum.getFavoriteByLevel(level);
				int favoriteLimit = favoriteEnum.getFavorite();
				if (favorite + favoriteActionAdd.getFavorite() > favoriteLimit) {
					FavoriteEnum lastFavoriteEnum = FavoriteEnum.getFavoriteById(favoriteEnum.getId() + 1);
					botFavoriteMapper.updateBotFavoriteSelective(new BotFavorite().setId(botFavorite.getId()).setLevel(lastFavoriteEnum.getLevel()));
					externalText = String.format("(关系提升为%s)", lastFavoriteEnum.getLevel());
				}
			}

			if (FavoriteEnum.anti.getLevel().equals(botFavorite.getLevel())) {
				FavoriteEnum lastFavoriteEnum = FavoriteEnum.strange;
				botFavoriteMapper.updateBotFavoriteSelective(new BotFavorite().setId(botFavorite.getId()).setLevel(lastFavoriteEnum.getLevel()).setFavorite(FavoriteEnum.anti.getFavorite()));
				externalText = String.format("(关系提升为%s)", lastFavoriteEnum.getLevel());
			}
//		}

		List<BotMessageChain> respChainList = this.randomTalkToMessageChain(messageAction, botFavorite, filterFavoriteTalkList, addFavorite, externalText);
		return BotMessage.simpleListMessage(respChainList);
	}

	private BotMessage handleAction(BotMessageAction messageAction) {
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();
		String action = messageAction.getText();

		// 触发对话的关键词不会太长
		if (action.length() > 5) {
			return null;
		}

		// 没解锁的跳过判定
		BotFavorite botFavorite = botFavoriteMapper.getBotFavoriteByUserId(userId);
		if (botFavorite == null) {
			return null;
		}
		String name = botFavorite.getName();
		String level = botFavorite.getLevel();

		if (action.equals(name)) {
			action = "交谈";
		}

		// 获取对话
		List<BotFavoriteTalk> favoriteTalkList = botFavoriteTalkMapper.getBotFavoriteTalkByCondition(new BotFavoriteTalkQuery().setType(FavoriteConstant.TYPE_ACTION).setAction(action).setLevel(level).setStatus(0));
		List<BotFavoriteTalk> filterFavoriteTalkList = favoriteTalkList.stream().filter(talk -> talk.getComplexResp() != null || talk.getResp() != null).collect(Collectors.toList());
		if (filterFavoriteTalkList.isEmpty()) {
			return null;
		}

		// 获取好感度增量
		Integer addFavorite = 0;
		BotFavoriteActionAdd favoriteActionAdd = botFavoriteActionAddMapper.getBotFavoriteActionAddByActionAndLevelAndType(action, level, FavoriteConstant.TYPE_ACTION);
		if (favoriteActionAdd != null && FavoriteConstant.TYPE_ACTION.equals(favoriteActionAdd.getType())) {
			// 凌晨4点刷新
			String dayStr = DateUtils.formatDateYMD(DateUtils.addTime(new Date(), Calendar.HOUR_OF_DAY, -4));
			// 每个人每天每个动作只能加一次好感度
			String redisKey = String.format("favorite-%s-%s-%s", dayStr, userId, action);
			if (!redisCache.exists(redisKey)) {
				addFavorite = botFavoriteManager.addFavorite(userId, favoriteActionAdd.getFavorite());
				redisCache.setValue(redisKey, "yes", Math.toIntExact(TimeUnit.DAYS.toSeconds(1)));
			}
		}

		List<BotMessageChain> respChainList = this.randomTalkToMessageChain(messageAction, botFavorite, filterFavoriteTalkList, addFavorite, "");
		return BotMessage.simpleListMessage(respChainList);
	}

	private List<BotMessageChain> randomTalkToMessageChain(BotMessageAction messageAction, BotFavorite botFavorite, List<BotFavoriteTalk> filterFavoriteTalkList, Integer addFavorite, String externalText) {
		BotSender botSender = messageAction.getBotSender();
		String name = botFavorite.getName();

		List<BotMessageChain> respChainList = new ArrayList<>();
		BotFavoriteTalk favoriteTalk = filterFavoriteTalkList.get(random.nextInt(filterFavoriteTalkList.size()));
		if (favoriteTalk.getComplexResp() != null) {
			String resp = favoriteTalk.getComplexResp();
			resp = this.replaceResp(messageAction, name, resp);
			List<BotMessageNode> nodeList = forwardMarkHandle.getForwardMessageByText(botSender, resp, name);
			if (addFavorite != 0) {
				nodeList.add(new BotMessageNode().setSenderName("旁白").setMessageChain(Lists.newArrayList(BotMessageChain.ofPlain(String.format("(好感度%+d)", addFavorite)))));
			}
			if (StringUtils.isNotBlank(externalText)) {
				nodeList.add(new BotMessageNode().setSenderName("旁白").setMessageChain(Lists.newArrayList(BotMessageChain.ofPlain(externalText))));
			}
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
		BotEnum bot = messageAction.getBot();
		BotUserDTO botUser = messageAction.getBotUser();

		resp = resp.replaceAll("\\{name}", name);
		resp = resp.replaceAll("\\{master}", botUser.getName());
		resp = resp.replaceAll("\\{botQQ}", "1");
		resp = resp.replaceAll("\\{masterQQ}", String.valueOf(botUser.getQq()));
		resp = resp.replaceAll("\\{narration}", "0");
		resp = resp.replaceAll("\\{timeTalk}", TimeUtil.getTimeTalk());
		return resp;
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
