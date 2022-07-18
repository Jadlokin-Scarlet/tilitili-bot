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
		Long taskId = null;
		try {
			TaskMessage taskMessage = this.getTaskMessage();
			if (taskMessage == null) {
				return;
			}
			taskId = taskMessage.getId();
			List<String> valueList = taskMessage.getValueList();
			Asserts.isTrue(valueList.size() == 3, "参数异常");
			String requestStr = valueList.get(0);
			String sendTime = valueList.get(1);
			String serverName = valueList.get(2);

//			Asserts.isTrue(DateUtils.(DateUtils.addTime(new Date(), ), sendTime), "");

			taskMapper.updateStatusById(taskId, TaskStatus.SPIDER.getValue(), TaskStatus.SUCCESS.getValue());
		} catch (Exception e) {
			if (taskId != null) {
				taskMapper.updateStatusById(taskId, TaskStatus.SPIDER.getValue(), TaskStatus.FAIL.getValue());
			}
			log.error("消费mc消息异常", e);
		}
	}

	private TaskMessage getTaskMessage() {
		TaskMessage taskMessage = (TaskMessage) jmsTemplate.receiveAndConvert(TaskReason.MINECRAFT_MESSAGE.destination);
		if (taskMessage == null) {
			return null;
		}
		taskMapper.updateStatusAndIpById(taskMessage.getId(), TaskStatus.WAIT.getValue(), TaskStatus.SPIDER.getValue(), ip);
		return taskMessage;
	}

}
