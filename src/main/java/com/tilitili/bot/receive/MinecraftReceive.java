package com.tilitili.bot.receive;

import com.google.gson.Gson;
import com.tilitili.bot.service.BotService;
import com.tilitili.common.emnus.MinecraftServerEmum;
import com.tilitili.common.emnus.TaskReason;
import com.tilitili.common.emnus.TaskStatus;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.message.TaskMessage;
import com.tilitili.common.entity.view.request.MinecraftWebHooksRequest;
import com.tilitili.common.manager.MinecraftManager;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.mapper.rank.TaskMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Slf4j
public class MinecraftReceive {
	private final String ip;
	private final Gson gson;
	private final JmsTemplate jmsTemplate;
	private final TaskMapper taskMapper;
	private final MinecraftManager minecraftManager;
	private final BotService botService;
	private final BotSenderMapper botSenderMapper;

	public MinecraftReceive(JmsTemplate jmsTemplate, TaskMapper taskMapper, Environment environment, MinecraftManager minecraftManager, BotService botService, BotSenderMapper botSenderMapper) {
		this.botService = botService;
		this.botSenderMapper = botSenderMapper;
		gson = new Gson();
		this.ip = environment.getProperty("ip");
		this.jmsTemplate = jmsTemplate;
		this.taskMapper = taskMapper;
		this.minecraftManager = minecraftManager;
	}

	@Scheduled(fixedDelay = 1)
	public void receiveMinecraftMessage() {
		TaskMessage taskMessage = null;
		try {
			taskMessage = this.getTaskMessage();
			if (taskMessage == null) {
				return;
			}
			log.debug("Message Received [{}]",taskMessage);
			Long taskId = taskMessage.getId();
			List<String> valueList = taskMessage.getValueList();
			Asserts.isTrue(valueList.size() == 2, "参数异常");
			String requestStr = valueList.get(0);
			String serverName = valueList.get(1);

			MinecraftServerEmum serverEmum = MinecraftServerEmum.getByName(serverName);
			Asserts.notNull(serverEmum, "找不到服务器配置");

			Asserts.notBlank(requestStr, "参数异常");
			MinecraftWebHooksRequest request = gson.fromJson(requestStr, MinecraftWebHooksRequest.class);
			Asserts.notNull(request, "参数异常");
			Asserts.notNull(request.getEventType(), "参数异常");

			BotMessage botMessage = minecraftManager.handleMessage(request, serverEmum);
			if (botMessage == null) return;

			BotSender botSender = botSenderMapper.getValidBotSenderById(4407L);
			Asserts.notNull(botSender, "无权限");
//			botService.syncHandleTextMessage(botMessage, botSender);

			taskMapper.updateStatusById(taskId, TaskStatus.SPIDER.getValue(), TaskStatus.SUCCESS.getValue());
		} catch (Exception e) {
			if (taskMessage != null && taskMessage.getId() != null) {
				taskMapper.updateStatusById(taskMessage.getId(), TaskStatus.SPIDER.getValue(), TaskStatus.FAIL.getValue());
			}
			log.error("消费mc消息异常, message=" + gson.toJson(taskMessage), e);
		}
	}

	private TaskMessage getTaskMessage() {
		TaskMessage taskMessage;
		try {
			taskMessage = (TaskMessage) jmsTemplate.receiveAndConvert(TaskReason.MINECRAFT_MESSAGE.destination);
		} catch (UncategorizedJmsException e) {
			log.warn("消息等待中断", e);
			return null;
		} catch (Exception e) {
			log.error("消息接收异常", e);
			return null;
		}
		if (taskMessage == null) {
			return null;
		}
		taskMapper.updateStatusAndIpById(taskMessage.getId(), TaskStatus.WAIT.getValue(), TaskStatus.SPIDER.getValue(), ip);
		return taskMessage;
	}

}
