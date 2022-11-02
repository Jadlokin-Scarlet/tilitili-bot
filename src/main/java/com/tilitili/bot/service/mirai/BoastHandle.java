package com.tilitili.bot.service.mirai;

import com.alibaba.fastjson.JSONPath;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotBoast;
import com.tilitili.common.entity.query.BotBoastQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.mapper.mysql.BotBoastMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

@Component
public class BoastHandle extends ExceptionRespMessageHandle {
	@Value("${mirai.bot-guild-qq}")
	private String BOT_GUILD_QQ;
	private final BotBoastMapper botBoastMapper;

	@Autowired
	public BoastHandle(BotBoastMapper botBoastMapper) {
		this.botBoastMapper = botBoastMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		Long botQQ = messageAction.getBotSender().getBot();
		String key = messageAction.getKeyWithoutPrefix();
		boolean isOther = Arrays.asList("夸夸他", "kkt").contains(key);
		List<Long> atList = messageAction.getAtList();
		String result = HttpClientUtil.httpGet("https://api.shadiao.app/chp");
		Asserts.notBlank(result, "网络异常");
		String text = JSONPath.read(result, "$.data.text", String.class);
		Asserts.notBlank(text, "网络异常");

		if (botBoastMapper.getBotBoastByCondition(new BotBoastQuery().setText(text)).isEmpty()) {
			botBoastMapper.addBotBoastSelective(new BotBoast().setText(text));
		}

		if (isOther) {
			Long firstAt = atList.stream().filter(Predicate.isEqual(botQQ).or(Predicate.isEqual(BOT_GUILD_QQ)).negate()).findFirst().orElse(null);
			Asserts.notNull(firstAt, "想我夸谁鸭");
			return BotMessage.simpleListMessage(Arrays.asList(
					BotMessageChain.ofAt(firstAt),
					BotMessageChain.ofPlain(" "),
					BotMessageChain.ofPlain(text)
			));
		} else {
			return BotMessage.simpleTextMessage(text);
		}
	}
}
