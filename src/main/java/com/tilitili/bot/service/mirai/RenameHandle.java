package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.emnus.GroupEnum;
import com.tilitili.common.emnus.SendTypeEnum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class RenameHandle extends ExceptionRespMessageHandle {
    private final int waitTime = 10;
    private final BotSender listenGroup;

    private final String statusKey = "rename.status";
    private final String lastSendTimeKey = "rename.last_send_time";

    private final BotManager botManager;
    private final ScheduledExecutorService scheduled =  Executors.newSingleThreadScheduledExecutor();

    @Autowired
    public RenameHandle(BotManager botManager, BotSenderMapper botSenderMapper) {
        this.botManager = botManager;
        this.listenGroup = botSenderMapper.getValidBotSenderById(3445L);
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        BotEnum bot = messageAction.getBot();
        BotSessionService.MiraiSession session = messageAction.getSession();
		BotUserDTO botUser = messageAction.getBotUser();
		BotSender botSender = messageAction.getBotSender();
		Long userId = botUser.getId();
        String name = "Jadlokin_Scarlet";

        if (Objects.equals(userId, BotUserConstant.MASTER_USER_ID) && SendTypeEnum.GROUP_MESSAGE_STR.equals(botSender.getSendType())) {
            String status = session.getOrDefault(statusKey, "冒泡！");
            String lastSendTimeStr = session.get(lastSendTimeKey);
            boolean isUp = lastSendTimeStr == null || DateUtils.parseDateYMDHMS(lastSendTimeStr).before(getLimitDate());
            if (status.equals("冒泡！") && !isUp) {
                botManager.changeGroupNick(bot, listenGroup, botUser, name + " | 水群ing");
                session.put(statusKey, "水群ing");
            } else if (isUp) {
                botManager.changeGroupNick(bot, listenGroup, botUser, name + " | 冒泡！");
                session.put(statusKey, "冒泡！");
            }

            scheduled.schedule(() -> {
                String lastSendTime2Str = session.get(lastSendTimeKey);
                boolean isDown = lastSendTime2Str == null || DateUtils.parseDateYMDHMS(lastSendTime2Str).before(getLimitDate());
                if (isDown) {
                    botManager.changeGroupNick(bot, listenGroup, botUser, name + " | 潜水。");
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
