package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.RandomResp;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotPlace;
import com.tilitili.common.entity.BotUserMapMapping;
import com.tilitili.common.entity.FishPlayer;
import com.tilitili.common.entity.query.BotPlaceQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.mysql.BotPlaceMapper;
import com.tilitili.common.mapper.mysql.BotUserMapMappingMapper;
import com.tilitili.common.mapper.mysql.FishPlayerMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MapHandle extends ExceptionRespMessageHandle {
	private final BotPlaceMapper botPlaceMapper;
	private final BotUserMapMappingMapper botUserMapMappingMapper;
	private final FishPlayerMapper fishPlayerMapper;

	private final RandomResp randomResp1 = new RandomResp("没听说过诶。", "那是哪？", "没去过，你要带我去吗？");

	public MapHandle(BotPlaceMapper botPlaceMapper, BotUserMapMappingMapper botUserMapMappingMapper, FishPlayerMapper fishPlayerMapper) {
		this.botPlaceMapper = botPlaceMapper;
		this.fishPlayerMapper = fishPlayerMapper;
		this.botUserMapMappingMapper = botUserMapMappingMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		Long userId = messageAction.getBotUser().getId();

		String place = messageAction.getValue();
		if (StringUtils.isBlank(place)) {
			List<BotPlace> placeList = botPlaceMapper.getBotPlaceByCondition(new BotPlaceQuery().setStatus(0));
			return BotMessage.simpleTextMessage("目前可去的区域有："+ placeList.stream().map(BotPlace::getPlace).collect(Collectors.joining(",")));
		}
		Asserts.notBlank(place, "去哪？");
		BotPlace botPlace = botPlaceMapper.getBotPlaceByPlace(place);
		Asserts.notNull(botPlace, randomResp1.getResp());
		Long placeId = botPlace.getId();
		BotUserMapMapping userMapMapping = botUserMapMappingMapper.getBotUserMapMappingByUserId(userId);
		if (userMapMapping != null) {
			Asserts.notEquals(userMapMapping.getPlaceId(), botPlace.getId(), "你已经在%s啦。", botPlace.getPlace());
		}

		FishPlayer fishPlayer = fishPlayerMapper.getValidFishPlayerByUserId(userId);
		Asserts.checkNull(fishPlayer, "还在钓鱼中，不要分心鸭。");
		if (userMapMapping == null) {
			botUserMapMappingMapper.addBotUserMapMappingSelective(new BotUserMapMapping().setUserId(userId).setPlaceId(placeId));
		} else {
			botUserMapMappingMapper.updateBotUserMapMappingSelective(new BotUserMapMapping().setId(userMapMapping.getId()).setPlaceId(placeId));
		}

		return BotMessage.simpleTextMessage(String.format("抵达了%s，%s", botPlace.getPlace(), botPlace.getDescription()));
	}
}
