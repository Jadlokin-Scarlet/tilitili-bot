package com.tilitili.bot.receive;

import com.tilitili.common.emnus.TaskReason;
import com.tilitili.common.emnus.TaskStatus;
import com.tilitili.common.entity.view.message.TaskMessage;
import com.tilitili.common.mapper.rank.TaskMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Slf4j
public class MinecraftReceive {
	private final JmsTemplate jmsTemplate;
	private final TaskMapper taskMapper;
	private final String ip;

	public MinecraftReceive(JmsTemplate jmsTemplate, TaskMapper taskMapper, Environment environment) {
		this.ip = environment.getProperty("ip");
		this.jmsTemplate = jmsTemplate;
		this.taskMapper = taskMapper;
	}

	@Scheduled(fixedDelay = 1)
	public void receiveMinecraftMessage() {
		TaskMessage taskMessage = (TaskMessage) jmsTemplate.receiveAndConvert(TaskReason.MINECRAFT_MESSAGE.destination);
		if (taskMessage == null) { return; }
		Long taskId = taskMessage.getId();
		List<String> valueList = taskMessage.getValueList();
		Asserts.isTrue(valueList.size() == 3, "参数异常");
		taskMapper.updateStatusAndIpById(taskId, TaskStatus.WAIT.getValue(), TaskStatus.SPIDER.getValue(), ip);

		taskMapper.updateStatusById(taskId, TaskStatus.SPIDER.getValue(), TaskStatus.SUCCESS.getValue());

	}

}
