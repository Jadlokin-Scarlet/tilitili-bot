package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.mirai.MiraiRequest;
import com.tilitili.common.entity.Recommend;
import com.tilitili.common.entity.RecommendVideo;
import com.tilitili.common.entity.view.mirai.MiraiMessage;
import com.tilitili.common.mapper.tilitili.RecommendMapper;
import com.tilitili.common.mapper.tilitili.RecommendVideoMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.BilibiliUtil;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AddRecommendHandle implements BaseMessageHandle {

    private final RecommendMapper recommendMapper;
    private final RecommendVideoMapper recommendVideoMapper;

    @Autowired
    public AddRecommendHandle(RecommendMapper recommendMapper, RecommendVideoMapper recommendVideoMapper) {
        this.recommendMapper = recommendMapper;
        this.recommendVideoMapper = recommendVideoMapper;
    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.AddRecommendHandle;
    }

    @Override
    public MiraiMessage handleMessage(MiraiRequest request) {
        MiraiMessage result = new MiraiMessage();

        String avStr = request.getParam("视频号");
        String operator = request.getParam("推荐人");
        String text = request.getParam("推荐语");
        int startTime = Integer.parseInt(request.getParamOrDefault("开始时间", "0"));
        int endTime = Integer.parseInt(request.getParamOrDefault("结束时间", String.valueOf(startTime + 30)));

        Asserts.notNull(avStr, "格式错啦(视频号)");
        Asserts.notNull(operator, "格式错啦(推荐人)");
        Asserts.notBlank(text, "格式错啦(推荐语)");
        Asserts.isTrue(startTime < endTime, "时间线错乱啦");

        long av;
        if (StringUtils.isNumber(avStr)) {
            av = Long.parseLong(avStr);
        } else {
            av = BilibiliUtil.converseAvToBv(avStr);
        }
        Recommend oldRecommend = recommendMapper.getByAv(av);
        Asserts.checkNull(oldRecommend, "这个视频已经有啦");

        RecommendVideo recommendVideo = recommendVideoMapper.getNew();


        Recommend recommend = new Recommend();
        recommend.setAv(av);
        recommend.setStatus(1);
        recommend.setText(text);
        recommend.setOperator(operator);
        recommend.setIssueId(recommendVideo.getId());
        recommend.setStartTime(startTime);
        recommend.setEndTime(endTime);
        recommendMapper.insert(recommend);

        return result.setMessage("收到！").setMessageType("Plain");
    }
}
