package com.tilitili.bot.emnus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum MessageHandleEnum {

    AddRecommendHandle("AddRecommendHandle", Arrays.asList("推荐", "tj"), "", "FriendMessage", 0),
    AddSubscriptionHandle("AddSubscriptionHandle", Arrays.asList("关注", "gz"), "关注b站up，关注后可以获得开播提醒和动态推送。格式：(gz[换行]uid=1)", "FriendMessage", 0),
    BeautifyJsonHandle("BeautifyJsonHandle", Arrays.asList("Json", "json"), "", "FriendMessage", 0),
    CalendarHandle("CalendarHandle", Arrays.asList("日程表", "rc"), "日程表，在指定时间提醒做某事。格式：（xxx叫我xxx）", "FriendMessage", 0),
    FindImageHandle("FindImageHandle", Arrays.asList("找图", "zt"), "查找原图。格式(zt[换行][图片])", "FriendMessage", 0),
    FranslateHandle("FranslateHandle", Arrays.asList("翻译", "fy"), "翻译文本或图片。格式(fy[换行]hello!)", "FriendMessage", 0),
    HelpHandle("HelpHandle", Arrays.asList("帮助", "help", "?", "？"), "获取帮助", "FriendMessage", 0),
    PatternStringHandle("PatternStringHandle", Arrays.asList("正则", "zz"), "", "FriendMessage", 0),
    RenameHandle("RenameHandle", Collections.emptyList(), "", "GroupMessage", 2),
    NoBakaHandle("NoBakaHandle", Collections.emptyList(), "", "GroupMessage",  1),
    RepeatHandle("RepeatHandle", Collections.emptyList(), "", "GroupMessage", 0),
    VoiceHandle("VoiceHandle", Arrays.asList("说", "s"), "文本转语音(日语)(群聊)。格式:(s[换行]你好！)", "GroupMessage", 0),
    PixivHandle("PixivHandle", Arrays.asList("色图", "st", "ss", "bs"), "色图！(群聊)。格式:(st loli)", "GroupMessage", 1),
    RecallHandle("RecallHandle", Arrays.asList("撤回", "ch"), "", "FriendMessage", 0),
    ConfigHandle("ConfigHandle", Arrays.asList("配置", "pz"), "", "FriendMessage", 0),
    CfcxHandle("CfcxHandle", Arrays.asList("成分查询", "cfcx"), "成分查询。格式:(cfcx Jadlokin_Scarlet)", "GroupMessage", 0),
    ;

    private final String name;
    private final List<String> keyword;
    private final String description;
    private final String sendType;
    private final Integer sort;

    MessageHandleEnum(String name, List<String> keyword, String description, String sendType, Integer sort) {
        this.name = name;
        this.keyword = keyword;
        this.description = description;
        this.sendType = sendType;
        this.sort = sort;
    }

    public String getName() {
        return name;
    }

    public List<String> getKeyword() {
        return keyword;
    }

    public String getDescription() {
        return description;
    }

    public String getSendType() {
        return sendType;
    }

    public Integer getSort() {
        return sort;
    }
}
