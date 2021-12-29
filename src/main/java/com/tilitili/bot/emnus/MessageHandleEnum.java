package com.tilitili.bot.emnus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum MessageHandleEnum {

	AddRecommendHandle(		"AddRecommendHandle",		Arrays.asList("推荐","tj"),				Arrays.asList("FriendMessage"),		0,		""),
	AddSubscriptionHandle(	"AddSubscriptionHandle",	Arrays.asList("关注","gz"),				Arrays.asList("FriendMessage"),		0,		"关注b站up，关注后可以获得开播提醒和动态推送。格式：(gz[换行]uid=1)"),
	BeautifyJsonHandle(		"BeautifyJsonHandle",		Arrays.asList("Json","json"),			Arrays.asList("FriendMessage"),		0,		""),
	CalendarHandle(			"CalendarHandle",			Arrays.asList("日程表","rc"),			Arrays.asList("FriendMessage"),		0,		"日程表，在指定时间提醒做某事。格式：（xxx叫我xxx）"),
	FindImageHandle(		"FindImageHandle",		Arrays.asList("找图","zt"),				Arrays.asList("FriendMessage"),		0,		"查找原图。格式(zt[换行][图片])"),
	FranslateHandle(		"FranslateHandle",		Arrays.asList("翻译","fy"),				Arrays.asList("FriendMessage"),		0,		"翻译文本或图片。格式(fy[换行]hello!)"),
	HelpHandle(				"HelpHandle",				Arrays.asList("帮助","help","?","？"),	Arrays.asList("FriendMessage"),		0,		"获取帮助"),
	PatternStringHandle(	"PatternStringHandle",	Arrays.asList("正则","zz"),				Arrays.asList("FriendMessage"),		0,		""),
	RenameHandle(			"RenameHandle",			Collections.emptyList(),				Arrays.asList("GroupMessage"),		2,		""),
	NoBakaHandle(			"NoBakaHandle",			Collections.emptyList(),				Arrays.asList("GroupMessage"),		1,		""),
	RepeatHandle(			"RepeatHandle",			Collections.emptyList(),				Arrays.asList("GroupMessage"),		-1,	""),
	VoiceHandle(			"VoiceHandle",			Arrays.asList("说","s"),				Arrays.asList("GroupMessage"),		0,		"文本转语音(日语)(群聊)。格式:(s[换行]你好！)"),
	PixivHandle(			"PixivHandle",			Arrays.asList("色图","st","ss","bs"),	Arrays.asList("GroupMessage"),		1,		"色图！(群聊)。格式:(st loli)"),
	RecallHandle(			"RecallHandle",			Arrays.asList("撤回","ch"),				Arrays.asList("FriendMessage"),		0,		""),
	ConfigHandle(			"ConfigHandle",			Arrays.asList("配置","pz"),				Arrays.asList("FriendMessage"),		0,		""),
	CfcxHandle(				"CfcxHandle",				Arrays.asList("成分查询","cfcx"),		Arrays.asList("GroupMessage"),		0,		"成分查询。格式:(cfcx Jadlokin_Scarlet)"),
	NewVideoHandle(			"NewVideoHandle",			Arrays.asList("新视频","nv"),			Arrays.asList("GuildMessage"),		0,		"随机获取昨日新增视频。格式:(.nv)"),
	;

	private final String name;
	private final List<String> keyword;
	private final String description;
	private final List<String> sendType;
	private final Integer sort;

	MessageHandleEnum(String name, List<String> keyword, List<String> sendType, Integer sort, String description) {
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

	public List<String> getSendType() {
		return sendType;
	}

	public Integer getSort() {
		return sort;
	}
}
