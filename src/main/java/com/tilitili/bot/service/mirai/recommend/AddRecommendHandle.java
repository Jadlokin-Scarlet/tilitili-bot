package com.tilitili.bot.service.mirai.recommend;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.TaskReason;
import com.tilitili.common.entity.Recommend;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.message.SimpleTask;
import com.tilitili.common.manager.TaskManager;
import com.tilitili.common.mapper.tilitili.RecommendMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.BilibiliUtil;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Objects;

@Component
public class AddRecommendHandle extends ExceptionRespMessageHandle {
    private final TaskManager taskManager;
    private final RecommendMapper recommendMapper;

    @Autowired
    public AddRecommendHandle(TaskManager taskManager, RecommendMapper recommendMapper) {
        this.taskManager = taskManager;
        this.recommendMapper = recommendMapper;
    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.ADD_RECOMMEND_HANDLE;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String titleKey = messageAction.getKey().replaceAll("tj", "推荐").replaceAll("zj", "自荐");

        int type = Objects.equals("自荐", titleKey) ? 1 : 0;
        String avStr = messageAction.getBody("视频号");
        String operator = messageAction.getBodyOrDefault("推荐人", messageAction.getBotMessage().getGroupNickName());
        String text = messageAction.getBody("推荐语");
        int startTime = Integer.parseInt(messageAction.getBodyOrDefault("开始时间", "0"));
        int endTime = Integer.parseInt(messageAction.getBodyOrDefault("结束时间", String.valueOf(startTime + 30)));

        Asserts.notNull(operator, "格式错啦(推荐人)");
        Asserts.notNull(avStr, "格式错啦(视频号)");
        Asserts.notBlank(text, "格式错啦(推荐语)");
        Asserts.isTrue(startTime < endTime, "时间线错乱啦");

        long av;
        if (StringUtils.isNumber(avStr)) {
            av = Long.parseLong(avStr);
        } else {
            av = BilibiliUtil.converseBvToAv(avStr);
        }
        Recommend oldRecommend = recommendMapper.getByAv(av);
        Asserts.checkNull(oldRecommend, "这个视频已经有啦");

        Recommend recommend = new Recommend();
        recommend.setAv(av);
        recommend.setStatus(0);
        recommend.setText(text);
        recommend.setOperator(operator);
        recommend.setStartTime(startTime);
        recommend.setEndTime(endTime);
        recommend.setType(type);
        recommendMapper.insert(recommend);

        taskManager.simpleSpiderVideo(new SimpleTask().setReason(TaskReason.SUPPLEMENT_VIDEO.value).setValueList(Collections.singletonList(String.valueOf(av))));

        return BotMessage.simpleTextMessage(String.format("收到！已存至%s池。", titleKey));
    }
}
