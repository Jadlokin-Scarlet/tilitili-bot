package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.mirai.MiraiRequest;
import com.tilitili.common.entity.view.bot.mirai.MiraiMessage;
import com.tilitili.common.entity.view.bot.mirai.Sender;
import com.tilitili.common.manager.MiraiManager;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class NoBakaHandle implements BaseMessageHandle {
    @Value("${mirai.master-qq}")
    private Long MASTER_QQ;

    private final MiraiManager miraiManager;

    @Autowired
    public NoBakaHandle(MiraiManager miraiManager) {
        this.miraiManager = miraiManager;
    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.NoBakaHandle;
    }

    @Override
    public MiraiMessage handleMessage(MiraiRequest request) throws Exception {
        String text = request.getText();
        Sender sender = request.getMessage().getSender();
        Sender sendGroup = sender.getGroup();
        MiraiMessage result = new MiraiMessage();

//        int bdCount = StringUtils.findCount("笨蛋", text);
//        if (bdCount > 0) {
//            String repeat = IntStream.range(0, bdCount).mapToObj(c -> "不笨").collect(Collectors.joining());
//            miraiManager.sendGroupMessage("Plain", repeat, request.getMessage().getSender().getGroup().getId());
//            return result.setMessage("").setMessageType("Plain");
//        }

        int ddCount = StringUtils.findCount("dd|DD|dD|Dd", text);
        if (ddCount > 0) {
            String repeat = IntStream.range(0, ddCount).mapToObj(c -> "bd").collect(Collectors.joining());
            miraiManager.sendGroupMessage("Plain", repeat, sendGroup.getId());
            return result.setMessage("").setMessageType("Plain");
        }

        if (sender.getId().equals(MASTER_QQ) && text.equals("cww")) {
            miraiManager.sendGroupMessage("Plain", "cww", sendGroup.getId());
            return result.setMessage("").setMessageType("Plain");
        }

        if (text.equals("让我看看") || text.equals("让我康康")) {
            miraiManager.sendGroupMessage("Plain", "不要！", sendGroup.getId());
            return result.setMessage("").setMessageType("Plain");
        }

        return null;
    }
}
