package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.emnus.TaskReason;
import com.tilitili.common.entity.Subscription;
import com.tilitili.common.entity.query.SubscriptionQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.message.SimpleTask;
import com.tilitili.common.manager.TaskManager;
import com.tilitili.common.mapper.tilitili.SubscriptionMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
public class AddSubscriptionHandle extends ExceptionRespMessageHandle {
    private final SubscriptionMapper subscriptionMapper;
    private final TaskManager taskManager;

    @Autowired
    public AddSubscriptionHandle(SubscriptionMapper subscriptionMapper, TaskManager taskManager) {
        this.subscriptionMapper = subscriptionMapper;
        this.taskManager = taskManager;
    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.AddSubscriptionHandle;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String uid = messageAction.getParamOrDefault("uid", messageAction.getValue());
        BotMessage botMessage = messageAction.getBotMessage();

        Long qq = botMessage.getQq();
        Long group = botMessage.getGroup();
        String guildId = botMessage.getChannel() == null? botMessage.getGuildId(): botMessage.getChannel().guildId;
        String channelId = botMessage.getChannelId() == null? botMessage.getChannelId(): botMessage.getChannel().channelId;
        String sendType = botMessage.getSendType();

        Asserts.notBlank(uid, "格式错啦(uid)");

        Long qqWithoutGroup = SendTypeEmum.GROUP_MESSAGE.equals(sendType)? null: qq;
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery().setType(1).setValue(uid).setSendType(sendType).setSendGroup(group).setSendQq(qqWithoutGroup).setSendGuild(guildId).setSendChannel(channelId);
        int oldCount = subscriptionMapper.countSubscriptionByCondition(subscriptionQuery);
        Asserts.isTrue(oldCount == 0, "已经关注了哦。");

        Subscription add = new Subscription().setValue(uid).setType(1).setSendType(sendType).setSendGroup(group).setSendQq(qqWithoutGroup).setSendGuild(guildId).setSendChannel(channelId);
        subscriptionMapper.addSubscriptionSelective(add);

        SimpleTask simpleTask = new SimpleTask().setValueList(Collections.singletonList(uid)).setReason(TaskReason.SUPPLEMENT_VIDEO_OWNER.value);
        taskManager.simpleSpiderVideo(simpleTask);
        return BotMessage.simpleTextMessage("关注成功！");
    }
}
