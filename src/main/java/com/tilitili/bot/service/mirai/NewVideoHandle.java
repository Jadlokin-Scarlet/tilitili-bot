package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.VideoInfo;
import com.tilitili.common.entity.query.VideoInfoQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.rank.VideoInfoMapper;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class NewVideoHandle extends ExceptionRespMessageHandle {
    private final VideoInfoMapper videoInfoMapper;

    @Autowired
    public NewVideoHandle(VideoInfoMapper videoInfoMapper) {
        this.videoInfoMapper = videoInfoMapper;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        VideoInfo video = videoInfoMapper.getRandomYesterdayVideo(new VideoInfoQuery());
        String reply = String.format("随机推荐昨日新增车万视频。如有误判，不要在意%n%s%nhttps://www.bilibili.com/video/%s", video.getName(), video.getBv());
        return BotMessage.simpleImageTextMessage(reply, video.getImg());
    }
}
