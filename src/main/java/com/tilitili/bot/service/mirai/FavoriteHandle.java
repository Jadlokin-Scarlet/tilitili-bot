package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.BaseMessageHandleAdapt;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.emnus.FavoriteEmum;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.BotFavorite;
import com.tilitili.common.entity.BotFavoriteTalk;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotFavoriteTalkQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.mysql.BotFavoriteMapper;
import com.tilitili.common.mapper.mysql.BotFavoriteTalkMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class FavoriteHandle extends BaseMessageHandleAdapt {
	private final Random random;
	private final BotFavoriteMapper botFavoriteMapper;
	private final BotFavoriteTalkMapper botFavoriteTalkMapper;

	@Autowired
	public FavoriteHandle(BotFavoriteMapper botFavoriteMapper, BotFavoriteTalkMapper botFavoriteTalkMapper) {
		this.random = new Random(System.currentTimeMillis());
		this.botFavoriteMapper = botFavoriteMapper;
		this.botFavoriteTalkMapper = botFavoriteTalkMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotUserDTO botUser = messageAction.getBotUser();
		BotSender botSender = messageAction.getBotSender();
		String text = messageAction.getText();

		// 其他平台必须绑定主账号
		if (BotUserConstant.USER_TYPE_QQ != botUser.getType()) {
			return null;
		}

		Long userId = botUser.getId();

		// 解锁好感度条件
		if ("你好".equals(text)) {
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
		BotFavorite favorite = botFavoriteMapper.getBotFavoriteByUserId(userId);
		if (favorite == null) {
			return null;
		}

		// 触发对话的关键词不会太长
		if (text.length() > 5) {
			return null;
		}

		List<BotFavoriteTalk> favoriteTalkList = botFavoriteTalkMapper.getBotFavoriteTalkByCondition(new BotFavoriteTalkQuery().setAction(text).setLevel(favorite.getLevel()));
		if (favoriteTalkList.isEmpty()) {
			return null;
		}

		BotFavoriteTalk favoriteTalk = favoriteTalkList.get(random.nextInt(favoriteTalkList.size()));
		return BotMessage.simpleTextMessage(favoriteTalk.getResp());
	}
}
