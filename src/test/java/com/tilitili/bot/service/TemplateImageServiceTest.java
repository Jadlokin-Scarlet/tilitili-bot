package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = StartApplication.class)
public class TemplateImageServiceTest {
	@Resource
	TemplateImageService tester;

	@Test
	public void getLongStringImage() throws Exception {
		String imageUrl = tester.getLongStringImage("鱼竿，鱼饵*2，雪的胖次，烤鱼，木鱼，墨鱼*2，螃蟹，草鱼*2，河豚，比目鱼*2，墨西哥钝口螈，猫鱼，鲸鱼，雪鱼，咸鱼*3，FFF团鱼，隐形鱼，鱼人，刘醒鱼，ikun鱼*2，虎纹鲨鱼，鲶鱼，水母*3，水猴子，血鱼*2，大鱼人*2，梭子蟹*2，溪蟹*3，黄鱼");
		System.out.println(imageUrl);
	}
}