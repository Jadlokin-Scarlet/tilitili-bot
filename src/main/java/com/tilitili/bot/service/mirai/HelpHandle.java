package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.view.bot.BotMessage;
import org.jsoup.helper.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tilitili.bot.emnus.MessageHandleEnum.*;

@Component
public class HelpHandle extends ExceptionRespMessageHandle {
    private final Map<MessageHandleEnum, String> handleDescMap = new HashMap<>();
    private final Map<String, String> keyHelpMap = new HashMap<>();
    private final Map<String, String> paramHelpMap = new HashMap<>();

    @Autowired
    public HelpHandle(List<BaseMessageHandle> handleList) {
        handleList.add(this);

        handleDescMap.put(AddSubscriptionHandle, "关注b站up，使用uid，关注后可以获得动态推送(私聊限定)和开播提醒。格式：(s.gz 114514)");
        handleDescMap.put(DeleteSubscriptionHandle, "取关b站up，使用uid。格式：(s.qg 114514)");
        handleDescMap.put(CalendarHandle, "日程表，在指定时间提醒做某事。格式：（xxx叫我xxx）");
        handleDescMap.put(FindImageHandle, "查找原图。格式(zt[换行][图片])");
        handleDescMap.put(FranslateHandle, "翻译文本或图片。格式(fy[换行]hello!)");
        handleDescMap.put(HelpHandle, "获取帮助");
        handleDescMap.put(VoiceHandle, "文本转语音(日语)(群聊)。格式:(s[换行]你好！)");
        handleDescMap.put(PixivHandle, "色图！(群聊)。格式:(st loli)");
        handleDescMap.put(CfcxHandle, "成分查询。格式:(cfcx Jadlokin_Scarlet)");
        handleDescMap.put(NewVideoHandle, "随机获取昨日新增视频。格式:(nv)");
        handleDescMap.put(TagHandle, "查询指定pid的tag。格式:(tag 1231)");

        // 命令简介默认使用desc
        for (BaseMessageHandle handle : handleList) {
            MessageHandleEnum e = handle.getType();
            String desc = handleDescMap.get(e);
            if (desc == null) continue;
            for (String key : e.getKeyword()) {
                keyHelpMap.put(key, desc);
            }
        }
        // 推荐功能自定义desc
        for (String key : AddRecommendHandle.getKeyword()) {
            keyHelpMap.put(key, "推荐指令向推荐池添加推荐(自荐类推)，推荐人默认使用群昵称，必填参数为：视频号(av或bv)，推荐语。" +
                    "选填参数为开始时间(单位秒)(默认0)，结束时间(单位秒)(默认30)，推荐人(填了就不使用群昵称)。" +
                    "格式例子：推荐[回车]视频号=12[回车]开始时间=0[回车]结束时间=30[回车]推荐人=Jadlokin_Scarlet[回车]推荐语=好！");
        }
        // 移除推荐自定义desc
        for (String key : DeleteRecommendHandle.getKeyword()) {
            keyHelpMap.put(key, "移除推荐池或当期推荐中的视频(自荐类推)，往期不能删");
        }

    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.HelpHandle;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String paramListStr = messageAction.getValue().replaceAll("\\s+", " ");;
        String sendType = messageAction.getBotMessage().getSendType();
        String guildprefix = sendType.equals(SendTypeEmum.Guild_Message.sendType)? ".": "";

        if (StringUtil.isBlank(paramListStr)) {
            StringBuilder reply = new StringBuilder("咱可以帮你做这些事！\n");
            for (Map.Entry<MessageHandleEnum, String> entry : handleDescMap.entrySet()) {
                MessageHandleEnum handle = entry.getKey();
                String desc = entry.getValue();
                if (handle.getSendType().contains(sendType)) continue;
                String key = handle.getKeyword().stream().map(a -> guildprefix + a).collect(Collectors.joining(","));
                reply.append(String.format("%s：%s\n", key, desc));
            }
            if (reply.charAt(reply.length() - 1) == '\n') {
                reply.deleteCharAt(reply.length() - 1);
            }
            return BotMessage.simpleTextMessage(reply.toString());
        } else if (! paramListStr.contains(" ")) {
            String keyDesc = keyHelpMap.get(paramListStr);
            String reply = String.format("[%s]的作用是[%s]。", paramListStr, keyDesc);
            return BotMessage.simpleTextMessage(reply);
        } else {
            return null;
        }
    }
}
