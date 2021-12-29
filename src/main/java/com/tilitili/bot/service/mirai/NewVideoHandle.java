package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.tilitili.VideoInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;


public class NewVideoHandle extends ExceptionRespMessageHandle {
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
    public BotMessage handleMessage(BotMessageAction messageAction) {

        return null;
    }
}
