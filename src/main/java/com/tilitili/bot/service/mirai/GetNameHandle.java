package com.tilitili.bot.service.mirai;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.mirai.UploadImageResult;
import com.tilitili.common.manager.TemplateImageManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class GetNameHandle extends ExceptionRespMessageHandle {
	private List<String> wordList1 = new ArrayList<>();
	private List<String> wordList2 = new ArrayList<>();
	private List<String> wordList3 = new ArrayList<>();
	private final TemplateImageManager templateImageManager;

	public GetNameHandle(TemplateImageManager templateImageManager) {
		this.templateImageManager = templateImageManager;
	}

	@Value("${GetNameHandle.wordList1:}")
	public void setWordList1(String wordList) {
		try {
			this.wordList1 = Gsons.fromJson(wordList, new TypeToken<List<String>>(){}.getType());
			log.info("刷新 wordList1 成功，{}", this.wordList1);
		} catch (Exception e) {
			log.error("刷新 wordList1 异常", e);
		}
	}

	@Value("${GetNameHandle.wordList2:}")
	public void setWordList2(String wordList) {
		try {
			this.wordList2 = Gsons.fromJson(wordList, new TypeToken<List<String>>(){}.getType());
			log.info("刷新 wordList2 成功，{}", this.wordList2);
		} catch (Exception e) {
			log.error("刷新 wordList2 异常", e);
		}
	}

	@Value("${GetNameHandle.wordList3:}")
	public void setWordList3(String wordList) {
		try {
			this.wordList3 = Gsons.fromJson(wordList, new TypeToken<List<String>>(){}.getType());
			log.info("刷新 wordList3 成功，{}", this.wordList3);
		} catch (Exception e) {
			log.error("刷新 wordList3 异常", e);
		}
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotRobot bot = messageAction.getBot();
		Asserts.notEmpty(wordList1);
		Asserts.notEmpty(wordList2);
		Asserts.notEmpty(wordList3);
		String word1 = wordList1.get(ThreadLocalRandom.current().nextInt(wordList1.size()));
		String word2 = wordList2.get(ThreadLocalRandom.current().nextInt(wordList2.size()));
		String word3 = wordList3.get(ThreadLocalRandom.current().nextInt(wordList3.size()));
		String name = word1 + word2 + word3;

		UploadImageResult result = templateImageManager.getLongStringImage(bot, name, 200);
		return BotMessage.simpleListMessage(Lists.newArrayList(
				BotMessageChain.ofPlain("你的新名字是："),
				BotMessageChain.ofImage(result)
		)).setQuote(messageAction.getMessageId());
	}
}
