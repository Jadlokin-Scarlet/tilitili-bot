package com.tilitili.bot.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class GameClock {
	private final List<Game> gameList;

	@Autowired
	public GameClock(List<Game> gameList) {
		this.gameList = gameList;
	}

//	@Scheduled(fixedDelay = 100)
	public void run() {
		try {
			long startTime = System.currentTimeMillis();
			for (Game game : gameList) {
				game.run();
			}
			long endTime = System.currentTimeMillis();
			long time = endTime - startTime;
			if (time >= 50) {
				log.error("帧率过低");
			}
		} catch (Exception e) {
			log.error("帧异常", e);
		}
	}
}
