package com.tilitili.bot.entity.twentyOne;

import com.tilitili.common.entity.BotUser;
import com.tilitili.common.utils.StreamUtil;

import java.util.List;
import java.util.stream.Collectors;

public class TwentyOnePlayer {
	private BotUser botUser;
	private List<TwentyOneCardList> cardListList;

	@Override
	public String toString() {
		return cardListList.stream().map(cardList -> String.format("%s：%s", botUser.getName(), cardList.getCardList().stream().map(TwentyOneCard::toString).collect(Collectors.joining(",")))).collect(Collectors.joining("\n"));
	}

	public Long getPlayerId() {
		return botUser == null? null: botUser.getExternalId();
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

	public BotUser getBotUser() {
		return botUser;
	}

	public TwentyOnePlayer setBotUser(BotUser botUser) {
		this.botUser = botUser;
		return this;
	}

	public boolean needEnd(CardResult cardResult) {
		return cardResult.getSuperCard() != null || (cardResult.getSum() == 21 && cardResult.getaCnt() == 0);
	}
}
