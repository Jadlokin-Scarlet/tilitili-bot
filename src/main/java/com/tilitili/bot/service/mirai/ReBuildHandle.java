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
		String key = messageAction.getKeyWithoutPrefix();
		String name = messageAction.getBodyOrDefault("项目", messageAction.getValue());
		String branches = messageAction.getBodyOrDefault("分支", "发布".equals(key)? "master": "reload");
		Asserts.notBlank(name, "格式错啦(项目)");
		Asserts.notBlank(branches, "格式错啦(分支)");

		if (!name.startsWith("tilitili-")) {
			name = "tilitili-" + name;
		}

		gitlabManager.reBuildByName(name, branches);
		return BotMessage.simpleTextMessage("重启中");
	}
}
