package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.mirai.MiraiRequest;
import com.tilitili.bot.service.MiraiSessionService;
import com.tilitili.common.emnus.GroupEmum;
import com.tilitili.common.entity.view.mirai.MiraiMessage;
import com.tilitili.common.manager.MiraiManager;
import com.tilitili.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//@Component
public class RenameHandle implements BaseMessageHandle {
    @Value("${mirai.master-qq}")
    private Long MASTER_QQ;
    private final int waitTime = 10;
    private final Long listenGroup = GroupEmum.QIAN_QIAN_GROUP.value;

    private final String statusKey = "rename.status";
    private final String lastSendTimeKey = "rename.last_send_time";

    private final MiraiManager miraiManager;
    private final ScheduledExecutorService scheduled =  Executors.newSingleThreadScheduledExecutor();

    @Autowired
    public RenameHandle(MiraiManager miraiManager) {
        this.miraiManager = miraiManager;
    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.RenameHandle;
    }

    @Override
    public MiraiMessage handleMessage(MiraiRequest request) {
        MiraiSessionService.MiraiSession session = request.getSession();
        Long group = request.getMessage().getSender().getGroup().getId();
        Long sender = request.getMessage().getSender().getId();
        String name = "<&ÿ?Çý>cirno";

        if (Objects.equals(sender, MASTER_QQ) && Objects.equals(group, listenGroup)) {
            String status = session.getOrDefault(statusKey, "冒泡！");
            String lastSendTimeStr = session.get(lastSendTimeKey);
            boolean isUp = lastSendTimeStr == null || DateUtils.parseDateYMDHMS(lastSendTimeStr).before(getLimitDate());
            if (status.equals("冒泡！") && !isUp) {
                miraiManager.changeGroupNick(listenGroup, MASTER_QQ, name + " | 水群ing");
                session.put(statusKey, "水群ing");
            } else if (isUp) {
                miraiManager.changeGroupNick(listenGroup, MASTER_QQ, name + " | 冒泡！");
                session.put(statusKey, "冒泡！");
            }

            scheduled.schedule(() -> {
                String lastSendTime2Str = session.get(lastSendTimeKey);
                boolean isDown = lastSendTime2Str == null || DateUtils.parseDateYMDHMS(lastSendTime2Str).before(getLimitDate());
                if (isDown) {
                    miraiManager.changeGroupNick(listenGroup, MASTER_QQ, name + " | 潜水。");
                    session.put(statusKey, "潜水。");
                }
            }, waitTime, TimeUnit.MINUTES);

            session.put(lastSendTimeKey, DateUtils.formatDateYMDHMS(new Date()));
        }

        return null;
    }


    private Date getLimitDate() {
        Calendar calstart = Calendar.getInstance();
        calstart.setTime(new Date());
        calstart.add(Calendar.MINUTE, -waitTime);
        return calstart.getTime();
    }
}
