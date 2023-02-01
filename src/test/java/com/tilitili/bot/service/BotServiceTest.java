package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import com.tilitili.common.emnus.BotEnum;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = StartApplication.class)
public class BotServiceTest {
	@Resource
	BotService tester;

	@Test
	public void syncHandleTextMessage() {
		tester.testHandleMessage(BotEnum.MINECRAFT, "{\"player\":{\"uuid\":\"ccb182ef-08d0-33f0-85af-ac4d7b4f3e52\",\"displayName\":\"§bJS§r\",\"port\":3599,\"exhaustion\":0.278598,\"exp\":0.0,\"whitelisted\":false,\"banned\":false,\"op\":false,\"balance\":1.0},\"deathMessage\":\"Jadlokin_Scarlet fell from a high place\",\"drops\":[{\"count\":1,\"slot\":-1,\"id\":\"minecraft:stone_sword\"},{\"count\":10,\"slot\":-1,\"id\":\"minecraft:redstone_torch\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:iron_pickaxe\"},{\"count\":64,\"slot\":-1,\"id\":\"minecraft:iron_ingot\"},{\"count\":64,\"slot\":-1,\"id\":\"minecraft:redstone\"},{\"count\":32,\"slot\":-1,\"id\":\"minecraft:diamond\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:white_stained_glass\"},{\"count\":60,\"slot\":-1,\"id\":\"minecraft:quartz\"},{\"count\":37,\"slot\":-1,\"id\":\"minecraft:redstone\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:potion\"},{\"count\":64,\"slot\":-1,\"id\":\"minecraft:cooked_cod\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:stone_shovel\"},{\"count\":49,\"slot\":-1,\"id\":\"minecraft:cooked_cod\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:phantom_membrane\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:stone_axe\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:leather_boots\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:iron_chestplate\"}],\"eventType\":\"PlayerDeath\"}");
	}
}