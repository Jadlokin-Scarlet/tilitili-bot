package com.tilitili.bot.component;

import com.tilitili.bot.StartApplication;
import com.tilitili.bot.entity.twentyOne.CardResult;
import com.tilitili.bot.entity.twentyOne.TwentyOneCard;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = StartApplication.class)
public class TwentyOneTableTest {

	public static final String STATUS_WAIT = "wait";
	public static final String STATUS_PLAYING = "playing";
	public static final String FIVE_CARD = "五龙";
	public static final String BLACK_JACK = "黑杰克";
	public static final String BOOM_CARD = "爆pai";
	@Resource
	private BotUserMapper botUserMapper;
	@Resource
	private BotManager botManager;

	@Test
	public void getCardResult() {
//		TwentyOneTable tester = new TwentyOneTable(botUserMapper, botManager, null);
		CardResult cardResult = this.getCardResult(Arrays.asList(
				new TwentyOneCard(1),
				new TwentyOneCard(2),
				new TwentyOneCard(0),
				new TwentyOneCard(4),
				new TwentyOneCard(0)
		));
	}

	public CardResult getCardResult(List<TwentyOneCard> cardList) {
		if (cardList.size() == 2) {
			TwentyOneCard card1 = cardList.get(0);
			TwentyOneCard card2 = cardList.get(1);
			if (card1.getPoint() == 1 && card2.getPoint() == 10) {
				return new CardResult(21, 1, BLACK_JACK);
			}
			if (card1.getPoint() == 10 && card2.getPoint() == 1) {
				return new CardResult(21, 1, BLACK_JACK);
			}
		}

		int sum = 0;
		int aCnt = 0;
		for (TwentyOneCard card : cardList) {
			sum += card.getPoint();
			if (card.getPoint() == 1) {
				sum += 10;
				aCnt += 1;
			} else {
				aCnt += 0;
			}
			if (sum > 21 && aCnt > 0) {
				sum -= 10;
				aCnt--;
			}
		}

		if (cardList.size() >= 5 && sum <= 21) {
			return new CardResult(sum, aCnt, FIVE_CARD);
		}

		if (sum > 21) {
			return new CardResult(sum, aCnt, BOOM_CARD);
		}
		return new CardResult(sum, aCnt, null);
	}
}