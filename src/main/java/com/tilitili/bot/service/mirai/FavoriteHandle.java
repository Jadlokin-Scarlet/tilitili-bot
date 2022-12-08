package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.BaseMessageHandleAdapt;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.constant.FavoriteConstant;
import com.tilitili.common.emnus.FavoriteEmum;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.*;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotFavoriteTalkQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotFavoriteManager;
import com.tilitili.common.manager.BotUserItemMappingManager;
import com.tilitili.common.mapper.mysql.BotFavoriteActionAddMapper;
import com.tilitili.common.mapper.mysql.BotFavoriteMapper;
import com.tilitili.common.mapper.mysql.BotFavoriteTalkMapper;
import com.tilitili.common.mapper.mysql.BotItemMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.DateUtils;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component
public class FavoriteHandle extends BaseMessageHandleAdapt {
	private final RedisCache redisCache;
	private final BotItemMapper botItemMapper;
	private final BotFavoriteMapper botFavoriteMapper;
	private final BotFavoriteManager botFavoriteManager;
	private final BotFavoriteTalkMapper botFavoriteTalkMapper;
	private final BotUserItemMappingManager botUserItemMappingManager;
	private final BotFavoriteActionAddMapper botFavoriteActionAddMapper;

	private final Random random;

	@Autowired
	public FavoriteHandle(RedisCache redisCache, BotItemMapper botItemMapper, BotFavoriteMapper botFavoriteMapper, BotFavoriteManager botFavoriteManager, BotFavoriteTalkMapper botFavoriteTalkMapper, BotUserItemMappingManager botUserItemMappingManager, BotFavoriteActionAddMapper botFavoriteActionAddMapper) {
		this.redisCache = redisCache;
		this.botItemMapper = botItemMapper;
		this.botFavoriteMapper = botFavoriteMapper;
		this.botFavoriteManager = botFavoriteManager;
		this.botFavoriteTalkMapper = botFavoriteTalkMapper;
		this.botUserItemMappingManager = botUserItemMappingManager;
		this.botFavoriteActionAddMapper = botFavoriteActionAddMapper;
		this.random = new Random(System.currentTimeMillis());
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotSessionService.MiraiSession session = messageAction.getSession();
		Long senderId = messageAction.getBotSender().getId();
		String source = session.getOrDefault(ChatHandle.nameKey + senderId, "tx");
		if (!source.equals("qln")) {
			return null;
		}

		BotUserDTO botUser = messageAction.getBotUser();
		String action = messageAction.getText();

		// 触发对话的关键词不会太长
		if (action.length() > 5) {
			return null;
		}

		// 其他平台必须绑定主账号
		if (BotUserConstant.USER_TYPE_QQ != botUser.getType()) {
			return null;
		}

		switch (messageAction.getKeyWithoutPrefix()) {
			case "你好": return handleStart(messageAction);
			case "赠送": return handleGift(messageAction);
			default: return handleAction(messageAction);
		}
	}

	private BotMessage handleGift(BotMessageAction messageAction) {
		Long userId = messageAction.getBotUser().getId();
		String itemName = messageAction.getValue();

		BotItem botItem = botItemMapper.getBotItemByName(itemName);
		Asserts.notNull(botItem, "那是啥。");

		BotFavorite botFavorite = botFavoriteMapper.getBotFavoriteByUserId(userId);
		Asserts.notNull(botFavorite, "好感度未启用。");
		List<BotMessageChain> respChainList = new ArrayList<>();

		// 获取对话
		List<BotFavoriteTalk> favoriteTalkList = botFavoriteTalkMapper.getBotFavoriteTalkByCondition(new BotFavoriteTalkQuery().setType(FavoriteConstant.TYPE_ITEM).setAction(itemName).setLevel(botFavorite.getLevel()));
		if (favoriteTalkList.isEmpty()) {
			return null;
		}
		BotFavoriteTalk favoriteTalk = favoriteTalkList.get(ThreadLocalRandom.current().nextInt(favoriteTalkList.size()));
		respChainList.add(BotMessageChain.ofPlain(favoriteTalk.getResp()));

		BotFavoriteActionAdd favoriteActionAdd = botFavoriteActionAddMapper.getBotFavoriteActionAddByActionAndLevel(itemName, botFavorite.getLevel());
		if (favoriteActionAdd == null || !FavoriteConstant.TYPE_ITEM.equals(favoriteActionAdd.getType())) {
			return null;
		}

		// 凌晨4点刷新
		String dayStr = DateUtils.formatDateYMD(DateUtils.addTime(new Date(), Calendar.HOUR_OF_DAY, -4));
		// 每个人每天每个动作只能加一次好感度
		String redisKey = String.format("favorite-%s-%s-%s", dayStr, userId, itemName);
		if (redisCache.exists(redisKey)) {
			return null;
		}

		Integer subNum = botUserItemMappingManager.addMapping(new BotUserItemMapping().setUserId(userId).setItemId(botItem.getId()).setNum(-1));
		Asserts.checkEquals(subNum, -1, "啊嘞，不对劲");
		Integer addFavorite = botFavoriteManager.safeAddFavorite(userId, favoriteActionAdd.getFavorite());
		if (addFavorite != 0) {
			respChainList.add(BotMessageChain.ofPlain(String.format("(好感度+%d)", addFavorite)));
		}
		redisCache.setValue(redisKey, "yes", Math.toIntExact(TimeUnit.DAYS.toSeconds(1)));
		return BotMessage.simpleListMessage(respChainList);
	}

	private BotMessage handleAction(BotMessageAction messageAction) {
		Long userId = messageAction.getBotUser().getId();
		String action = messageAction.getText();

		// 没解锁的跳过判定
		BotFavorite botFavorite = botFavoriteMapper.getBotFavoriteByUserId(userId);
		if (botFavorite == null) {
			return null;
		}
		String level = botFavorite.getLevel();

		// 获取对话
		List<BotFavoriteTalk> favoriteTalkList = botFavoriteTalkMapper.getBotFavoriteTalkByCondition(new BotFavoriteTalkQuery().setType(FavoriteConstant.TYPE_ACTION).setAction(action).setLevel(level));
		if (favoriteTalkList.isEmpty()) {
			return null;
		}

		List<BotMessageChain> respChainList = new ArrayList<>();

		BotFavoriteTalk favoriteTalk = favoriteTalkList.get(random.nextInt(favoriteTalkList.size()));
		respChainList.add(BotMessageChain.ofPlain(favoriteTalk.getResp()));

		// 获取好感度增量
		BotFavoriteActionAdd favoriteActionAdd = botFavoriteActionAddMapper.getBotFavoriteActionAddByActionAndLevel(action, level);
		if (favoriteActionAdd != null && FavoriteConstant.TYPE_ACTION.equals(favoriteActionAdd.getType())) {
			// 凌晨4点刷新
			String dayStr = DateUtils.formatDateYMD(DateUtils.addTime(new Date(), Calendar.HOUR_OF_DAY, -4));
			// 每个人每天每个动作只能加一次好感度
			String redisKey = String.format("favorite-%s-%s-%s", dayStr, userId, action);
			if (!redisCache.exists(redisKey)) {
				Integer addFavorite = botFavoriteManager.safeAddFavorite(userId, favoriteActionAdd.getFavorite());
				if (addFavorite != 0) {
					respChainList.add(BotMessageChain.ofPlain(String.format("(好感度+%d)", addFavorite)));
					redisCache.setValue(redisKey, "yes", Math.toIntExact(TimeUnit.DAYS.toSeconds(1)));
				}
			}
		}


		return BotMessage.simpleListMessage(respChainList);
	}

	private BotMessage handleStart(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		Long userId = messageAction.getBotUser().getId();
		List<Long> atList = messageAction.getAtList();
		atList.retainAll(BotUserConstant.BOT_USER_ID_LIST);
		boolean hasAtBot = !atList.isEmpty();
		boolean isFriend = SendTypeEmum.FRIEND_MESSAGE_STR.equals(botSender.getSendType());
		if (hasAtBot || isFriend) {
			BotFavorite favorite = botFavoriteMapper.getBotFavoriteByUserId(userId);
			if (favorite == null) {
				int addCnt = botFavoriteMapper.addBotFavoriteSelective(new BotFavorite().setUserId(userId).setFavorite(FavoriteEmum.strange.getFavorite()).setLevel(FavoriteEmum.strange.getLevel()));
				if (addCnt != 0) {
					return BotMessage.simpleTextMessage("你好，初次见面，我叫琪露诺。(好感度启用)");
				}
			}
		}
		return null;
	}
}
