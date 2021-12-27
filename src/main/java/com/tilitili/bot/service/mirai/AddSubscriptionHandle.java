package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.mirai.MiraiRequest;
import com.tilitili.common.emnus.TaskReason;
import com.tilitili.common.entity.Subscription;
import com.tilitili.common.entity.view.mirai.MiraiMessage;
import com.tilitili.common.entity.view.mirai.Sender;
import com.tilitili.common.entity.view.SimpleTask;
import com.tilitili.common.manager.TaskManager;
import com.tilitili.common.mapper.tilitili.SubscriptionMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Component
public class AddSubscriptionHandle implements BaseMessageHandle {
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
    public MiraiMessage handleMessage(MiraiRequest request) {
        MiraiMessage result = new MiraiMessage();

        String uid = request.getParam("uid");
        Sender sender = request.getMessage().getSender();
        Long qq = sender.getId();
        Long group = Optional.ofNullable(sender.getGroup()).map(Sender::getId).orElse(null);

        Asserts.notBlank(uid, "格式错啦(uid)");

        int oldCount = subscriptionMapper.countSubscriptionByCondition(new Subscription().setType(1).setValue(uid).setSendQq(qq));
        Asserts.isTrue(oldCount == 0, "已经关注了哦。");

        Subscription add = new Subscription().setValue(uid).setType(1).setSendGroup(group).setSendQq(qq).setSendType(group == null? "FriendMessage": "TempMessage");
        subscriptionMapper.insertSubscription(add);

        SimpleTask simpleTask = new SimpleTask().setValueList(Collections.singletonList(uid)).setReason(TaskReason.SUPPLEMENT_VIDEO_OWNER.value);
        taskManager.simpleSpiderVideo(simpleTask);
        return result.setMessage("关注成功！").setMessageType("Plain");
    }
}
