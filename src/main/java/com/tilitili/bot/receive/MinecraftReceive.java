package com.tilitili.bot.receive;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MinecraftReceive {
//	private final String ip;
//	private final Gson gson;
//	private final JmsTemplate jmsTemplate;
//	private final TaskMapper taskMapper;
//	private final BotService botService;
//	private final BotSenderCacheManager botSenderCacheManager;
//	private final BotRobotCacheManager botRobotCacheManager;
//
//
//	public MinecraftReceive(JmsTemplate jmsTemplate, TaskMapper taskMapper, Environment environment, BotService botService, BotSenderCacheManager botSenderCacheManager, BotRobotCacheManager botRobotCacheManager) {
//		this.botService = botService;
//		this.botSenderCacheManager = botSenderCacheManager;
//		this.botRobotCacheManager = botRobotCacheManager;
//		gson = new Gson();
//		this.ip = environment.getProperty("ip");
//		this.jmsTemplate = jmsTemplate;
//		this.taskMapper = taskMapper;
//	}
//
//	@Scheduled(fixedDelay = 1)
//	public void  receiveMinecraftMessage() {
//		TaskMessage taskMessage = null;
//		try {
//			taskMessage = this.getTaskMessage();
//			if (taskMessage == null) {
//				return;
//			}
//			Long taskId = taskMessage.getId();
//			List<String> valueList = taskMessage.getValueList();
//			Asserts.isTrue(valueList.size() == 2, "参数异常");
//			String requestStr = valueList.get(0);
//			String senderIdStr = valueList.get(1);
//			Asserts.isNumber(senderIdStr, "参数异常");
//			log.info("Message Received {}",requestStr);
//			long senderId = Long.parseLong(senderIdStr);
//			BotSender botSender = botSenderCacheManager.getValidBotSenderById(senderId);
//			Asserts.notNull(botSender, "权限不足");
//			BotRobot bot = botRobotCacheManager.getValidBotRobotById(botSender.getBot());
//			Asserts.notNull(bot, "权限不足");
//			botService.syncHandleMessage(bot, requestStr);
//			taskMapper.updateStatusById(taskId, TaskStatus.SPIDER.getValue(), TaskStatus.SUCCESS.getValue());
//		} catch (AssertException e) {
//			if (taskMessage != null && taskMessage.getId() != null) {
//				taskMapper.updateStatusById(taskMessage.getId(), TaskStatus.SPIDER.getValue(), TaskStatus.FAIL.getValue());
//			}
//			log.warn("消费mc消息断言异常, message=" + gson.toJson(taskMessage), e);
//		} catch (Exception e) {
//			if (taskMessage != null && taskMessage.getId() != null) {
//				taskMapper.updateStatusById(taskMessage.getId(), TaskStatus.SPIDER.getValue(), TaskStatus.FAIL.getValue());
//			}
//			log.error("消费mc消息异常, message=" + gson.toJson(taskMessage), e);
//		}
//	}
//
//	private TaskMessage getTaskMessage() {
//		TaskMessage taskMessage;
//		try {
//			taskMessage = (TaskMessage) jmsTemplate.receiveAndConvert(TaskReason.MINECRAFT_MESSAGE.destination);
//		} catch (UncategorizedJmsException e) {
//			log.warn("消息等待中断", e);
//			return null;
//		} catch (Exception e) {
//			log.error("消息接收异常", e);
//			return null;
//		}
//		if (taskMessage == null) {
//			return null;
//		}
//		taskMapper.updateStatusAndIpById(taskMessage.getId(), TaskStatus.WAIT.getValue(), TaskStatus.SPIDER.getValue(), ip);
//		return taskMessage;
//	}

}
