package com.tilitili.bot.controller.pub;

import com.google.common.collect.ImmutableList;
import com.tilitili.bot.controller.BaseController;
import com.tilitili.common.emnus.TaskReason;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.message.SimpleTask;
import com.tilitili.common.manager.TaskManager;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/api/pub/mc")
public class MinecraftController extends BaseController {
	private final TaskManager taskManager;

	public MinecraftController(TaskManager taskManager) {
		this.taskManager = taskManager;
	}

	@PostMapping("/report/{senderId}")
	@ResponseBody
	public BaseModel<String> webHooksOther(@RequestBody String requestStr, @PathVariable String senderId) {
		Asserts.isNumber(senderId, "参数异常");
		Asserts.notNull(requestStr, "参数异常");
		Asserts.isTrue(requestStr.endsWith("}"), "参数异常");
		try {
			taskManager.simpleSpiderVideo(new SimpleTask().setReason(TaskReason.MINECRAFT_MESSAGE.value).setValueList(ImmutableList.of(requestStr, String.valueOf(senderId))));
		} catch (Exception e) {
			log.warn("发送mc消息异常", e);
		}
		return BaseModel.success();
	}

}
