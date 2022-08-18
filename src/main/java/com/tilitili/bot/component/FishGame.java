package com.tilitili.bot.component;

import com.tilitili.bot.entity.FishPlayer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FishGame {
	private Boolean updateRun = false;
	private final List<FishPlayer> playerList = new ArrayList<>();

	@Scheduled(fixedDelay = 1000 * 60)
	public void run() {
		try {
			this.updateRun = true;
		} catch (Exception e) {
			log.error("钓鱼异常", e);
		} finally {
			this.updateRun = false;
		}
	}

}
