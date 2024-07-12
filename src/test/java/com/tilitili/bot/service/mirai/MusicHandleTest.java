package com.tilitili.bot.service.mirai;

import com.tilitili.bot.BotApplication;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.util.BotMessageActionUtil;
import com.tilitili.common.entity.PlayerMusicList;
import com.tilitili.common.entity.query.PlayerMusicListQuery;
import com.tilitili.common.mapper.mysql.PlayerMusicListMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest(classes = BotApplication.class)
class MusicHandleTest {
	@Autowired
	private MusicHandle tester;
	@Autowired
	private PlayerMusicListMapper playerMusicListMapper;

	@Test
	void handleMessage() {
		List<Long> userIdList = playerMusicListMapper.getPlayerMusicListByCondition(new PlayerMusicListQuery()).stream().map(PlayerMusicList::getUserId).distinct().collect(Collectors.toList());
		for (Long userId : userIdList) {
			BotMessageAction messageAction = BotMessageActionUtil.buildEmptyAction("歌单 同步", userId);
			System.out.println(ReflectionTestUtils.invokeMethod(tester, "handleSyncList", messageAction).toString());
		}
	}
}