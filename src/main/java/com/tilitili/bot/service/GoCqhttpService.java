package com.tilitili.bot.service;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tilitili.bot.entity.mirai.MiraiRequest;
import com.tilitili.bot.service.mirai.BaseMessageHandle;
import com.tilitili.common.emnus.ChannelEmum;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.gocqhttp.GoCqhttpWsMessage;
import com.tilitili.common.entity.view.bot.mirai.MiraiMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.GoCqhttpManager;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class GoCqhttpService {
//    private final GoCqhttpManager goCqhttpManager;
//    private final BotSessionService botSessionService;
//    private final List<BaseMessageHandle> messageHandleList;
//
//    @Autowired
//    public GoCqhttpService(GoCqhttpManager goCqhttpManager, BotSessionService botSessionService, List<BaseMessageHandle> messageHandleList) {
//        this.goCqhttpManager = goCqhttpManager;
//        this.botSessionService = botSessionService;
//        this.messageHandleList = messageHandleList;
//        this.messageHandleList.sort(Comparator.comparing(a -> a.getType().getSort(), Comparator.reverseOrder()));
//    }
//
//    public void syncHandleTextMessage(String requestStr) {
//        try {
//            Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
//            GoCqhttpWsMessage wsMessage = gson.fromJson(requestStr, GoCqhttpWsMessage.class);
//            Asserts.notNull(wsMessage, "未获取到消息");
//
//            String messageType = wsMessage.getMessageType();
//            String guildId = wsMessage.getGuildId();
//            String channelId = wsMessage.getChannelId();
//
//            Asserts.notBlank(messageType, "未获取到消息");
//            Asserts.notBlank(channelId, "未获取到消息");
//
//            ChannelEmum channel = ChannelEmum.getChannelByChannelId(channelId);
//
//            BotMessage result = null;
//            if (messageType.equals("guild")) {
//                Asserts.isTrue(ChannelEmum.channelIds().contains(channelId), "不在可用频道");
//
//                BotSessionService.MiraiSession miraiSession = botSessionService.getSession("guild-" + guildId + "-" + channelId);
//                result = handleGuildMessage(wsMessage, miraiSession);
//            }
//
//            Asserts.notNull(result, "无回复");
//            Asserts.notBlank(result.getMessage(), "无回复");
//            if (messageType.equals("guild")) {
//                goCqhttpManager.sendGuildChannelMsg(channel, result.getMessage());
//            }
//        } catch (AssertException e) {
//            log.error(e.getMessage());
//        } catch (Exception e) {
//            log.error("异常", e);
//        }
//    }
//
//    public BotMessage handleGuildMessage(GoCqhttpWsMessage wsMessage, BotSessionService.MiraiSession miraiSession) {
//        try {
//            MiraiRequest miraiRequest = new MiraiRequest(wsMessage, miraiSession);
//
//            for (BaseMessageHandle handle : messageHandleList) {
//                if (handle.getType().getSendType().equals("guild")) {
//                    if (handle.getType().getKeyword().contains(miraiRequest.getTitleKey()) || handle.getType().getKeyword().isEmpty()) {
//                        BotMessage result = handle.handleMessage(miraiRequest);
//                        if (result != null) {
//                            return result;
//                        }
//                    }
//                }
//            }
//            return null;
//        } catch (AssertException e) {
//            log.error(e.getMessage());
//            return null;
//        } catch (Exception e) {
//            log.error("处理消息回调失败", e);
//            return new MiraiMessage().setMessage("").setMessageType("Plain");
//        }
//
//    }
}
