package com.tilitili.bot.service;

import com.google.gson.reflect.TypeToken;
import com.tilitili.bot.service.mirai.talk.ReplyHandle;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class AnimeWordsService {
	private final Map<String, List<String>> wordMap;
	private final Random random;

	@Autowired
	public AnimeWordsService() throws IOException {
		String jsonStr = IOUtils.toString(ReplyHandle.class.getResourceAsStream("/word.json"), StandardCharsets.UTF_8);
		wordMap = Gsons.fromJson(jsonStr, new TypeToken<Map<String, List<String>>>() {}.getType());
		random = new Random(System.currentTimeMillis());
	}

	public String getRespByShortContain(String req) {
		for (Map.Entry<String, List<String>> entry : wordMap.entrySet()) {
			String key = entry.getKey();
			List<String> valueList = entry.getValue();
			if (req.length() - 3 <= key.length() && req.contains(key)) {
				return valueList.get(random.nextInt(valueList.size()));
			}
		}
		return null;
	}
}
