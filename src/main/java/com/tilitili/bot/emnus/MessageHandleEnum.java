package com.tilitili.bot.emnus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.tilitili.common.emnus.SendTypeEmum.*;

public enum MessageHandleEnum {

	AddRecommendHandle(			"AddRecommendHandle",				Arrays.asList("推荐","自荐","tj", "zj"),	0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE), ""),
	DeleteRecommendHandle(		"DeleteRecommendHandle",				Arrays.asList("移除推荐/自荐"),		0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE), ""),
	AddSubscriptionHandle(		"AddSubscriptionHandle",			Arrays.asList("关注","s.gz"),			0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE, GUILD_MESSAGE), "关注b站up，使用uid，关注后可以获得动态推送(私聊限定)和开播提醒。格式：(s.gz 114514)"),
	DeleteSubscriptionHandle(	"DeleteSubscriptionHandle",		Arrays.asList("取关","s.qg"),			0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE, GUILD_MESSAGE), "取关b站up，使用uid。格式：(s.qg 114514)"),
	BeautifyJsonHandle(			"BeautifyJsonHandle",				Arrays.asList("Json","json"),			0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE), ""),
	CalendarHandle(				"CalendarHandle",					Arrays.asList("日程表","rc"),			0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE), "日程表，在指定时间提醒做某事。格式：（xxx叫我xxx）"),
	FindImageHandle(			"FindImageHandle",				Arrays.asList("找图","zt"),				0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE), "查找原图。格式(zt[换行][图片])"),
	FranslateHandle(			"FranslateHandle",				Arrays.asList("翻译","fy"),				0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE), "翻译文本或图片。格式(fy[换行]hello!)"),
	HelpHandle(					"HelpHandle",						Arrays.asList("帮助","help"),			0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE, GUILD_MESSAGE), "获取帮助"),
	PatternStringHandle(		"PatternStringHandle",			Arrays.asList("正则","zz"),				0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE), ""),
	RenameHandle(				"RenameHandle",					Collections.emptyList(),				2,		Arrays.asList(GROUP_MESSAGE), ""),
	NoBakaHandle(				"NoBakaHandle",					Collections.emptyList(),				1,		Arrays.asList(GROUP_MESSAGE), ""),
	RepeatHandle(				"RepeatHandle",					Collections.emptyList(),				-1,	Arrays.asList(GROUP_MESSAGE), ""),
	VoiceHandle(				"VoiceHandle",					Arrays.asList("说","s"),				0,		Arrays.asList(GROUP_MESSAGE), "文本转语音(日语)(群聊)。格式:(s[换行]你好！)"),
	PixivHandle(				"PixivHandle",					Arrays.asList("色图","st","ss","bs"),	1,		Arrays.asList(GROUP_MESSAGE), "色图！(群聊)。格式:(st loli)"),
	RecallHandle(				"RecallHandle",					Arrays.asList("撤回","ch"),				0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE), ""),
	ConfigHandle(				"ConfigHandle",					Arrays.asList("配置","pz"),				0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE), ""),
	CfcxHandle(					"CfcxHandle",						Arrays.asList("成分查询","cfcx"),		0,		Arrays.asList(GROUP_MESSAGE), "成分查询。格式:(cfcx Jadlokin_Scarlet)"),
//	NewVideoHandle(				"NewVideoHandle",						Arrays.asList("新视频","nv"),			0,		Arrays.asList(GUILD_MESSAGE), "随机获取昨日新增视频。格式:(nv)"),
	TagHandle(					"TagHandle",						Arrays.asList("tag"),					0,		Arrays.asList(GROUP_MESSAGE), "查询指定pid的tag。格式:(tag 1231)"),
	;

	private final String name;
	private final List<String> keyword;
	private final String description;
	private final List<String> sendType;
	private final Integer sort;

	MessageHandleEnum(String name, List<String> keyword, Integer sort, List<String> sendType, String description) {
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
