package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;

//@Component
public class RenameHandle extends ExceptionRespMessageHandle {
//    @Value("${mirai.master-qq}")
//    private Long MASTER_QQ;
//    private final int waitTime = 10;
//    private final Long listenGroup = GroupEmum.QIAN_QIAN_GROUP.value;
//
//    private final String statusKey = "rename.status";
//    private final String lastSendTimeKey = "rename.last_send_time";
//
//    private final BotManager botManager;
//    private final ScheduledExecutorService scheduled =  Executors.newSingleThreadScheduledExecutor();
//
//    @Autowired
//    public RenameHandle(BotManager botManager) {
//        this.botManager = botManager;
//    }
//
	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
//        BotEmum bot = messageAction.getBot();
//        BotSessionService.MiraiSession session = messageAction.getSession();
//        Long group = messageAction.getBotMessage().getGroup();
//        Long qq = messageAction.getBotMessage().getQq();
//        String name = "<&ÿ?Çý>cirno";
//
//        if (Objects.equals(qq, MASTER_QQ) && Objects.equals(group, listenGroup)) {
//            String status = session.getOrDefault(statusKey, "冒泡！");
//            String lastSendTimeStr = session.get(lastSendTimeKey);
//            boolean isUp = lastSendTimeStr == null || DateUtils.parseDateYMDHMS(lastSendTimeStr).before(getLimitDate());
//            if (status.equals("冒泡！") && !isUp) {
//                botManager.changeGroupNick(bot, listenGroup, MASTER_QQ, name + " | 水群ing");
//                session.put(statusKey, "水群ing");
//            } else if (isUp) {
//                botManager.changeGroupNick(bot, listenGroup, MASTER_QQ, name + " | 冒泡！");
//                session.put(statusKey, "冒泡！");
//            }
//
//            scheduled.schedule(() -> {
//                String lastSendTime2Str = session.get(lastSendTimeKey);
//                boolean isDown = lastSendTime2Str == null || DateUtils.parseDateYMDHMS(lastSendTime2Str).before(getLimitDate());
//                if (isDown) {
//                    botManager.changeGroupNick(bot, listenGroup, MASTER_QQ, name + " | 潜水。");
//                    session.put(statusKey, "潜水。");
//                }
//            }, waitTime, TimeUnit.MINUTES);
//
//            session.put(lastSendTimeKey, DateUtils.formatDateYMDHMS(new Date()));
//        }
//
        return null;
    }
//
//
//    private Date getLimitDate() {
//        Calendar calstart = Calendar.getInstance();
//        calstart.setTime(new Date());
//        calstart.add(Calendar.MINUTE, -waitTime);
//        return calstart.getTime();
//    }
}
