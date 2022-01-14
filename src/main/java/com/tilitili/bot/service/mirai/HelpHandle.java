package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotTask;
import com.tilitili.common.entity.dto.BotTaskDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotSenderManager;
import com.tilitili.common.mapper.mysql.BotTaskMapper;
import com.tilitili.common.utils.Asserts;
import org.jsoup.helper.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HelpHandle extends ExceptionRespMessageHandle {
//    private final List<Pair<MessageHandleEnum, String>> handleDescMap = new ArrayList<>();
//    private final Map<String, String> keyHelpMap = new HashMap<>();
//    private final Map<String, String> paramHelpMap = new HashMap<>();

    private final BotSenderManager botSenderManager;
    private final BotTaskMapper botTaskMapper;

    @Autowired
    public HelpHandle(BotSenderManager botSenderManager, BotTaskMapper botTaskMapper) {
        this.botSenderManager = botSenderManager;
        this.botTaskMapper = botTaskMapper;
//        handleList.put(this.getClass().getSimpleName(), this);
//
//        Map<String, String> handleDescMap = new HashMap<>();
//
//        // 同时添加到所有帮助列表和单个命令查询
//        handleDescMap.put(ADD_SUBSCRIPTION_HANDLE, "关注b站up，使用uid，关注后可以获得动态推送(私聊限定)和开播提醒。格式：(s.gz 114514)");
//        handleDescMap.put(DELETE_SUBSCRIPTION_HANDLE, "取关b站up，使用uid。格式：(s.qg 114514)");
//        handleDescMap.put(CALENDAR_HANDLE, "日程表，在指定时间提醒做某事。格式：（xxx叫我xxx）");
//        handleDescMap.put(FIND_IMAGE_HANDLE, "查找原图。格式(zt[换行][图片])");
//        handleDescMap.put(FRANSLATE_HANDLE, "翻译文本或图片。格式(fy[换行]hello!)");
//        handleDescMap.put(HELP_HANDLE, "获取帮助");
//        handleDescMap.put(VOICE_HANDLE, "文本转语音(日语)。格式:(s[换行]你好！)");
//        handleDescMap.put(PIXIV_HANDLE, "色图！ss是r18，bs是非r18，st是混合，但是不准色色，只准用bs！格式:(st tag1 1000users入り tag3)");
//        handleDescMap.put(CFCX_HANDLE, "成分查询。格式:(cfcx Jadlokin_Scarlet)");
//        handleDescMap.put(NEW_VIDEO_HANDLE, "随机获取昨日新增视频。格式:(nv)");
//        handleDescMap.put(TAG_HANDLE, "查询指定pid的tag。格式:(tag 1231)");
//        handleDescMap.put(TALK_HANDLE, "设定对话。格式:(对话 提问 回答)或者(对话[回车]提问=hello？[回车]回答=hi？)");
//
//        for (BaseMessageHandle handle : handleList) {
//            MessageHandleEnum e = handle.getType();
//            String desc = handleDescMap.get(e);
//            if (desc == null) continue;
//            this.handleDescMap.add(Pair.of(e, desc));
//        }
//
//        // 添加到单个命令查询，或覆盖
//        handleDescMap.put(ADD_RECOMMEND_HANDLE, "推荐指令向推荐池添加推荐(自荐类推)，推荐人默认使用群昵称，必填参数为：视频号(av或bv)，推荐语。" +
//                "选填参数为开始时间(单位秒)(默认0)，结束时间(单位秒)(默认30)，推荐人(填了就不使用群昵称)。" +
//                "格式例子：推荐[回车]视频号=12[回车]开始时间=0[回车]结束时间=30[回车]推荐人=Jadlokin_Scarlet[回车]推荐语=好！");
//        handleDescMap.put(DELETE_RECOMMEND_HANDLE, "移除推荐池或当期推荐中的视频(自荐类推)，往期不能删");
//        handleDescMap.put(DELETE_CALENDAR_HANDLE, "移除日程表，使用日程码，例子：移除日程 123");
//        handleDescMap.put(TALK_HANDLE, "设定对话，匹配方式为全文匹配，想要其他条件需要定制，\n" +
//                "格式1主要方便设置词语的问答，不支持使用空白字符和等于号，格式2可以设置问答句，可以使用空白字符，但也不能使用等于号。\n" +
//                "格式:(对话 提问 回答)或者(对话[回车]提问=hello？[回车]回答=hi？)");
//        handleDescMap.put(DELETE_TALK_HANDLE, "移除对话。格式:(移除对话 提问)");
//
//        for (BaseMessageHandle handle : handleList) {
//            MessageHandleEnum e = handle.getType();
//            String desc = handleDescMap.get(e);
//            if (desc == null) continue;
//            for (String key : e.getKeyword()) {
//                keyHelpMap.put(key, desc);
//            }
//        }
    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.HELP_HANDLE;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String paramListStr = messageAction.getValueOrDefault("").replaceAll("\\s+", " ").trim();;
        String sendType = messageAction.getBotMessage().getSendType();
        String guildPrefix = sendType.equals(SendTypeEmum.Guild_Message.sendType)? ".": "";

        BotSender botSender = botSenderManager.getSenderByBotMessage(messageAction.getBotMessage());
        if (StringUtil.isBlank(paramListStr)) {
            List<BotTaskDTO> botTaskDTOList = botTaskMapper.getBotTaskListBySenderId(botSender.getId());

            StringBuilder reply = new StringBuilder("咱可以帮你做这些事！查看详情发送（帮助 [指令]）\n");
            for (int i = 0; i < botTaskDTOList.size(); i++) {
                BotTaskDTO botTask = botTaskDTOList.get(i);
                String key = botTask.getKeyListStr() == null ? "" : botTask.getKeyListStr();
                reply.append(String.format("%s.%s: %s\n", i + 1, botTask.getNick(), key));
            }
//
//            for (Pair<MessageHandleEnum, String> entry : handleDescMap) {
//                MessageHandleEnum handle = entry.getFirst();
//                String desc = entry.getSecond();
//                if (! handle.getSendType().contains(sendType)) continue;
//                String key = handle.getKeyword().stream().map(a -> guildPrefix + a).collect(Collectors.joining(","));
//                reply.append(String.format("%s：%s\n", key, desc));
//            }
            if (reply.charAt(reply.length() - 1) == '\n') {
                reply.deleteCharAt(reply.length() - 1);
            }
            return BotMessage.simpleTextMessage(reply.toString());
        } else if (! paramListStr.contains(" ")) {
            List<BotTask> botTaskList = botTaskMapper.getBotTaskListBySenderIdAndKey(botSender.getId(), paramListStr, guildPrefix);
            Asserts.isTrue(botTaskList.size() < 2, "不对劲");
            String reply;
            if (botTaskList.isEmpty()) {
                reply = String.format("[%s]的作用是[%s]。", paramListStr, paramListStr);
            } else {
                BotTask botTask = botTaskList.get(0);
                reply = String.format("[%s]的作用是[%s]。", paramListStr, botTask.getDescription());
            }
            return BotMessage.simpleTextMessage(reply);
        } else {
            return null;
        }
    }
}
