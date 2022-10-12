package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import com.tilitili.common.constant.BotSenderConstant;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.PixivImage;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.mapper.mysql.PixivImageMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = StartApplication.class)
public class PixivServiceTest {
	@Resource
	PixivService tester;
	@Resource
	private PixivImageMapper pixivImageMapper;
	@Resource
	private BotSenderMapper botSenderMapper;

	@Test
	public void sendPixivImage() {
		PixivImage pixivImage = pixivImageMapper.getPixivImageById(89020L);
		BotSender botSender = botSenderMapper.getBotSenderById(BotSenderConstant.MASTER_SENDER_ID);
		tester.sendPixivImage(new BotMessage().setSender(botSender).setMessageId("7410"), pixivImage, botSender);
	}
}