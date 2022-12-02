package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.BaseMessageHandleAdapt;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.emnus.FavoriteEmum;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.BotFavorite;
import com.tilitili.common.entity.BotFavoriteActionAdd;
import com.tilitili.common.entity.BotFavoriteTalk;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotFavoriteTalkQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotFavoriteManager;
import com.tilitili.common.mapper.mysql.BotFavoriteActionAddMapper;
import com.tilitili.common.mapper.mysql.BotFavoriteMapper;
import com.tilitili.common.mapper.mysql.BotFavoriteTalkMapper;
import com.tilitili.common.utils.DateUtils;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class FavoriteHandle extends BaseMessageHandleAdapt {
	private final Random random;
	private final RedisCache redisCache;
	private final BotFavoriteMapper botFavoriteMapper;
	private final BotFavoriteManager botFavoriteManager;
	private final BotFavoriteTalkMapper botFavoriteTalkMapper;
	private final BotFavoriteActionAddMapper botFavoriteActionAddMapper;

	@Autowired
	public FavoriteHandle(RedisCache redisCache, BotFavoriteMapper botFavoriteMapper, BotFavoriteManager botFavoriteManager, BotFavoriteTalkMapper botFavoriteTalkMapper, BotFavoriteActionAddMapper botFavoriteActionAddMapper) {
		this.redisCache = redisCache;
		this.botFavoriteManager = botFavoriteManager;
		this.botFavoriteActionAddMapper = botFavoriteActionAddMapper;
		this.random = new Random(System.currentTimeMillis());
		this.botFavoriteMapper = botFavoriteMapper;
		this.botFavoriteTalkMapper = botFavoriteTalkMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotUserDTO botUser = messageAction.getBotUser();
		BotSender botSender = messageAction.getBotSender();
		String action = messageAction.getText();

		// 触发对话的关键词不会太长
		if (action.length() > 5) {
			return null;
		}

		// 其他平台必须绑定主账号
		if (BotUserConstant.USER_TYPE_QQ != botUser.getType()) {
			return null;
		}

		Long userId = botUser.getId();

		// 解锁好感度条件
		if ("你好".equals(action)) {
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
		}

		// 没解锁的跳过判定
		BotFavorite botFavorite = botFavoriteMapper.getBotFavoriteByUserId(userId);
		if (botFavorite == null) {
			return null;
		}
		String level = botFavorite.getLevel();

		// 获取对话
		List<BotFavoriteTalk> favoriteTalkList = botFavoriteTalkMapper.getBotFavoriteTalkByCondition(new BotFavoriteTalkQuery().setAction(action).setLevel(level));
		if (favoriteTalkList.isEmpty()) {
			return null;
		}

		List<BotMessageChain> respChainList = new ArrayList<>();

		BotFavoriteTalk favoriteTalk = favoriteTalkList.get(random.nextInt(favoriteTalkList.size()));
		respChainList.add(BotMessageChain.ofPlain(favoriteTalk.getResp()));

		// 获取好感度增量
		BotFavoriteActionAdd favoriteActionAdd = botFavoriteActionAddMapper.getBotFavoriteActionAddByActionAndLevel(action, level);
		if (favoriteActionAdd != null) {
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
}
