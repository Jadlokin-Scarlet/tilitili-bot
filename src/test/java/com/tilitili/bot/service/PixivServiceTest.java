package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.mapper.mysql.PixivImageMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@Slf4j
@SpringBootTest(classes = StartApplication.class)
public class PixivServiceTest {
	@Resource
	PixivCacheService tester;
	@Resource
	private PixivImageMapper pixivImageMapper;
	@Resource
	private BotSenderCacheManager botSenderCacheManager;

	@Test
	public void sendPixivImage() {
//		PixivImage pixivImage = pixivImageMapper.getPixivImageById(89020L);
//		BotSender botSender = botSenderCacheManager.getBotSenderById(BotSenderConstant.MASTER_SENDER_ID);
//		tester.sendPixivImage(new BotMessage().setSender(botSender).setMessageId("7410"), pixivImage, botSender);
	}
	@Test
	public void downloadPixivImageAndUploadToQQ() {
//		System.out.println(tester.downloadPixivImageAndUploadToOSS("https://i.pximg.net/img-original/img/2021/02/14/16/10/55/87766142_p1.png", 1));
	}
}