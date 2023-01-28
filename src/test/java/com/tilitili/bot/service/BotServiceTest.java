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
	public void syncHandleEvent() {
	}

	@Test
	public void syncHandleTextMessage() {
		tester.testHandleMessage(BotEnum.MINECRAFT, "{\"player\":{\"uuid\":\"19f7f145-1438-35c4-a8ba-8d5c1a926621\",\"displayName\":\"吴旭§r\",\"address\":\"36.101.194.211\",\"port\":31950,\"exhaustion\":2.9327369,\"exp\":0.95652187,\"whitelisted\":false,\"banned\":false,\"op\":false,\"balance\":0.0},\"deathMessage\":\"wuxu was impaled by Drowned\",\"drops\":[{\"count\":37,\"slot\":-1,\"id\":\"minecraft:bread\"},{\"count\":64,\"slot\":-1,\"id\":\"minecraft:smooth_stone\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:iron_shovel\"},{\"count\":64,\"slot\":-1,\"id\":\"minecraft:smooth_stone\"},{\"count\":16,\"slot\":-1,\"id\":\"minecraft:smooth_stone\"},{\"count\":61,\"slot\":-1,\"id\":\"minecraft:dirt\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:netherite_pickaxe\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:wooden_hoe\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:netherite_axe\"},{\"count\":2,\"slot\":-1,\"id\":\"minecraft:peony\"},{\"count\":8,\"slot\":-1,\"id\":\"minecraft:cod\"},{\"count\":8,\"slot\":-1,\"id\":\"minecraft:iron_ingot\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:white_wool\"},{\"count\":3,\"slot\":-1,\"id\":\"minecraft:stone\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:phantom_membrane\"},{\"count\":8,\"slot\":-1,\"id\":\"minecraft:rotten_flesh\"},{\"count\":4,\"slot\":-1,\"id\":\"minecraft:dark_oak_planks\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:pufferfish\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:dirt\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:wheat\"},{\"count\":40,\"slot\":-1,\"id\":\"minecraft:salmon\"},{\"count\":4,\"slot\":-1,\"id\":\"minecraft:chicken\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:iron_shovel\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:iron_shovel\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:iron_boots\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:iron_leggings\"},{\"count\":1,\"slot\":-1,\"id\":\"minecraft:iron_chestplate\"}],\"eventType\":\"PlayerDeath\"}");
	}
}