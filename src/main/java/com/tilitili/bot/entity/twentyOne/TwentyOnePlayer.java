package com.tilitili.bot.entity.twentyOne;

import com.tilitili.common.entity.dto.BaseDTO;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.utils.StreamUtil;

import java.util.List;

public class TwentyOnePlayer extends BaseDTO {
	private BotUserDTO botUser;
	private List<TwentyOneCardList> cardListList;

	public Long getPlayerId() {
		return botUser == null? null: botUser.getId();
	}

	public Integer getHaveScore() {
		return botUser == null? null: botUser.getScore();
	}

	public Boolean isPrepare() {
		if (cardListList == null) return false;
		return this.cardListList.stream().allMatch(StreamUtil.isNotNull(TwentyOneCardList::getScore));
	}

	public void addCard(TwentyOneCard card) {
		this.getFirstNoEndCardList().getCardList().add(card);
	}

	public TwentyOneCardList getFirstNoEndCardList() {
		if (cardListList == null) return null;
		return cardListList.stream().filter(StreamUtil.isEqual(TwentyOneCardList::getStatus, 0)).findFirst().orElse(null);
	}


	public List<TwentyOneCardList> getCardListList() {
		return cardListList;
	}

	public TwentyOnePlayer setCardListList(List<TwentyOneCardList> cardListList) {
		this.cardListList = cardListList;
		return this;
	}

	public BotUserDTO getBotUser() {
		return botUser;
	}

	public TwentyOnePlayer setBotUser(BotUserDTO botUser) {
		this.botUser = botUser;
		return this;
	}

	public boolean needEnd(CardResult cardResult) {
		return cardResult.getSuperCard() != null || (cardResult.getSum() == 21 && cardResult.getaCnt() == 0);
	}
}
