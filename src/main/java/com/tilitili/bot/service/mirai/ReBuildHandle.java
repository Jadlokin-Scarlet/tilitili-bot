package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.GitlabManager;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReBuildHandle extends ExceptionRespMessageHandle {
	private final GitlabManager gitlabManager;

	@Autowired
	public ReBuildHandle(GitlabManager gitlabManager) {
		this.gitlabManager = gitlabManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String name = messageAction.getParam("项目");
		String branches = messageAction.getParam("分支");
		String value = messageAction.getValue();
		if (name == null && value != null) {
			if (value.contains(" ")) {
				name = value.substring(0, value.indexOf(" ")).trim();
			} else {
				name = value;
			}
		}
		if (branches == null && value != null) {
			if (value.contains(" ")) {
				branches = value.substring(value.indexOf(" ") + 1).trim();
			} else {
				branches = "master";
			}
		}
		Asserts.notBlank(name, "格式错啦(项目)");
		Asserts.notBlank(branches, "格式错啦(分支)");

		if (!name.startsWith("tilitili-")) {
			name = "tilitili-" + name;
		}

		gitlabManager.reBuildByName(name, branches);
		return BotMessage.simpleTextMessage("重启中");
	}
}
