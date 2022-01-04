package com.tilitili.bot.emnus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.tilitili.common.emnus.SendTypeEmum.*;

public enum MessageHandleEnum {

	AddRecommendHandle(			"AddRecommendHandle",				Arrays.asList("推荐","自荐","tj", "zj"),	0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE)),
	DeleteRecommendHandle(		"DeleteRecommendHandle",			Arrays.asList("移除推荐", "移除自荐"),		0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE)),
	AddSubscriptionHandle(		"AddSubscriptionHandle",			Arrays.asList("关注","s.gz"),			0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE, GUILD_MESSAGE)),
	DeleteSubscriptionHandle(	"DeleteSubscriptionHandle",		Arrays.asList("取关","s.qg"),			0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE, GUILD_MESSAGE)),
	BeautifyJsonHandle(			"BeautifyJsonHandle",				Arrays.asList("Json","json"),			0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE)),
	CalendarHandle(				"CalendarHandle",					Arrays.asList("日程表","rc"),			0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE)),
	FindImageHandle(			"FindImageHandle",				Arrays.asList("找图","zt"),				0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE)),
	FranslateHandle(			"FranslateHandle",				Arrays.asList("翻译","fy"),				0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE)),
	HelpHandle(					"HelpHandle",						Arrays.asList("帮助","help"),			0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE, GROUP_MESSAGE, GUILD_MESSAGE)),
	PatternStringHandle(		"PatternStringHandle",			Arrays.asList("正则","zz"),				0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE)),
	RenameHandle(				"RenameHandle",					Collections.emptyList(),				2,		Arrays.asList(GROUP_MESSAGE)),
	NoBakaHandle(				"NoBakaHandle",					Collections.emptyList(),				1,		Arrays.asList(GROUP_MESSAGE)),
	RepeatHandle(				"RepeatHandle",					Collections.emptyList(),				-1,	Arrays.asList(GROUP_MESSAGE)),
	VoiceHandle(				"VoiceHandle",					Arrays.asList("说","s"),				0,		Arrays.asList(GROUP_MESSAGE)),
	PixivHandle(				"PixivHandle",					Arrays.asList("色图","st","ss","bs"),	1,		Arrays.asList(GROUP_MESSAGE)),
	RecallHandle(				"RecallHandle",					Arrays.asList("撤回","ch"),				0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE)),
	ConfigHandle(				"ConfigHandle",					Arrays.asList("配置","pz"),				0,		Arrays.asList(FRIEND_MESSAGE, TEMP_MESSAGE)),
	CfcxHandle(					"CfcxHandle",						Arrays.asList("成分查询","cfcx"),		0,		Arrays.asList(GROUP_MESSAGE)),
	NewVideoHandle(				"NewVideoHandle",					Arrays.asList("新视频","nv"),			0,		Arrays.asList(GUILD_MESSAGE)),
	TagHandle(					"TagHandle",						Arrays.asList("tag"),					0,		Arrays.asList(GROUP_MESSAGE)),
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
