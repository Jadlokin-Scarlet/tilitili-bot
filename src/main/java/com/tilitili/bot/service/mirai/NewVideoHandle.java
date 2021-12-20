package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.mirai.MiraiRequest;
import com.tilitili.common.entity.mirai.MiraiMessage;
import com.tilitili.common.mapper.tilitili.VideoInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NewVideoHandle implements BaseMessageHandle {
    private final VideoInfoMapper videoInfoMapper;

    @Autowired
    public NewVideoHandle(VideoInfoMapper videoInfoMapper) {
        this.videoInfoMapper = videoInfoMapper;
    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.NewVideoHandle;
    }

    @Override
    public MiraiMessage handleMessage(MiraiRequest request) {

        return null;
    }
}
