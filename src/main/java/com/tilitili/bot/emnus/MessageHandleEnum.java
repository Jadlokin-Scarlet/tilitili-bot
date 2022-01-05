package com.tilitili.bot.emnus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.tilitili.common.emnus.SendTypeEmum.*;

public enum MessageHandleEnum {

	ADD_RECOMMEND_HANDLE(		"AddRecommendHandle",			Arrays.asList("推荐","自荐","tj", "zj"),	0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE)),
	DELETE_RECOMMEND_HANDLE(	"DeleteRecommendHandle",		Arrays.asList("移除推荐", "移除自荐"),		0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE)),
	ADD_SUBSCRIPTION_HANDLE(	"AddSubscriptionHandle",		Arrays.asList("关注","s.gz"),			0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE, GUILD_MESSAGE)),
	DELETE_SUBSCRIPTION_HANDLE(	"DeleteSubscriptionHandle",	Arrays.asList("取关","s.qg"),			0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE, GUILD_MESSAGE)),
	BEAUTIFY_JSON_HANDLE(		"BeautifyJsonHandle",			Arrays.asList("Json","json"),			0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE)),
	CALENDAR_HANDLE(			"CalendarHandle",				Arrays.asList("日程表","rc"),			0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE)),
	DELETE_CALENDAR_HANDLE(		"DeleteCalendarHandle",		Arrays.asList("移除日程"),				0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE)),
	FIND_IMAGE_HANDLE(			"FindImageHandle",			Arrays.asList("找图","zt"),				0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE, GUILD_MESSAGE)),
	FRANSLATE_HANDLE(			"FranslateHandle",			Arrays.asList("翻译","fy"),				0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE)),
	HELP_HANDLE(				"HelpHandle",					Arrays.asList("帮助","help"),			0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE, GUILD_MESSAGE)),
	PATTERN_STRING_HANDLE(		"PatternStringHandle",		Arrays.asList("正则","zz"),				0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE)),
	RENAME_HANDLE(				"RenameHandle",				Collections.emptyList(),				2,		Arrays.asList(GROUP_MESSAGE)),
	REPLY_HANDLE(				"ReplyHandle",				Collections.emptyList(),				-1,	Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE, GUILD_MESSAGE)),
	REPEAT_HANDLE(				"RepeatHandle",				Collections.emptyList(),				-2,	Arrays.asList(GROUP_MESSAGE)),
	VOICE_HANDLE(				"VoiceHandle",				Arrays.asList("说","s"),				0,		Arrays.asList(GROUP_MESSAGE)),
	PIXIV_HANDLE(				"PixivHandle",				Arrays.asList("色图","st","ss","bs"),	1,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE)),
	RECALL_HANDLE(				"RecallHandle",				Arrays.asList("撤回","ch"),				0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE)),
	CONFIG_HANDLE(				"ConfigHandle",				Arrays.asList("配置","pz"),				0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE)),
	CFCX_HANDLE(				"CfcxHandle",					Arrays.asList("成分查询","cfcx"),		0,		Arrays.asList(GROUP_MESSAGE)),
	NEW_VIDEO_HANDLE(			"NewVideoHandle",				Arrays.asList("新视频","nv"),			0,		Arrays.asList(GUILD_MESSAGE)),
	TAG_HANDLE(					"TagHandle",					Arrays.asList("tag"),					0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE)),
	TALK_HANDLE(				"TalkHandle",					Arrays.asList("对话"),					0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE, GUILD_MESSAGE)),
	DELETE_TALK_HANDLE(			"DeleteTalkHandle",			Arrays.asList("移除对话"),				0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE, GUILD_MESSAGE)),
	;

	private final String name;
	private final List<String> keyword;
	private final List<String> sendType;
	private final Integer sort;

	MessageHandleEnum(String name, List<String> keyword, Integer sort, List<String> sendType) {
		this.name = name;
		this.keyword = keyword;
		this.sendType = sendType;
		this.sort = sort;
	}

	public String getName() {
		return name;
	}

	public List<String> getKeyword() {
		return keyword;
	}

	public List<String> getSendType() {
		return sendType;
	}

	public Integer getSort() {
		return sort;
	}
}
