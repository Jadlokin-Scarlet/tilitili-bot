package com.tilitili.bot.service;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.BaseMessageHandle;
import com.tilitili.common.emnus.ChannelEmum;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BotService {
    private final BotManager botManager;
    private final List<BaseMessageHandle> messageHandleList;
    private final BotSessionService botSessionService;

    public BotService(BotManager botManager, List<BaseMessageHandle> messageHandleList, BotSessionService botSessionService) {
        this.botManager = botManager;
        this.messageHandleList = messageHandleList;
        this.botSessionService = botSessionService;
        this.messageHandleList.sort(Comparator.comparing(a -> a.getType().getSort(), Comparator.reverseOrder()));
    }

    @Async
    public void syncHandleTextMessage(BotMessage botMessage) {
        String sendType = botMessage.getSendType();
        List<String> alwaysReplySendTypeList = Arrays.asList(SendTypeEmum.Friend_Message.sendType, SendTypeEmum.Temp_Message.sendType);
        boolean alwaysReply = alwaysReplySendTypeList.contains(sendType);
        try {
            BotSessionService.MiraiSession session = botSessionService.getSession(getSessionKey(botMessage));
            BotMessageAction botMessageAction = new BotMessageAction(botMessage, session);
            String actionKey = botMessageAction.getKey();

            if (Objects.equals(sendType, SendTypeEmum.Guild_Message.sendType)) {
                Asserts.isTrue(ChannelEmum.channelIds().contains(botMessage.getChannelId()), "不在可用频道");
                if (actionKey != null) {
                    String prefix = "^[.。]";
                    if (!Pattern.compile(prefix).matcher(actionKey).find()) {
                        throw new AssertException("频道命令以[" + prefix + "]开头");
                    } else {
                        actionKey = actionKey.substring(1);
                    }
                }
            }

            BotMessage respMessage = null;
            for (BaseMessageHandle messageHandle : messageHandleList) {
                MessageHandleEnum handleType = messageHandle.getType();
                if (handleType.getSendType().contains(sendType)) {
                    if (handleType.getKeyword().contains(actionKey) || handleType.getKeyword().isEmpty()) {
                        try {
                            respMessage = messageHandle.handleMessage(botMessageAction);
                        } catch (AssertException e) {
                            log.debug(e.getMessage());
                            respMessage = messageHandle.handleAssertException(botMessageAction, e);
                        }
                        if (respMessage != null) {
                            break;
                        }
                    }
                }
            }
            Asserts.notNull(respMessage, "无回复");
            Asserts.notEmpty(respMessage.getBotMessageChainList(), "回复为空");

            if (respMessage.getSendType() == null) {
                respMessage.setSendType(sendType);
                respMessage.setQq(botMessage.getQq());
                respMessage.setGroup(botMessage.getGroup());
                respMessage.setGuildId(botMessage.getGuildId());
                respMessage.setChannelId(botMessage.getChannelId());
            }

            botManager.sendMessage(respMessage);
        } catch (AssertException e) {
            log.debug(e.getMessage());
            if (alwaysReply) {
                botManager.sendMessage(BotMessage.simpleImageMessage("http://m.qpic.cn/psc?/V53UUlnk2IehYn4WcXfY2dBFO92OvB1L/TmEUgtj9EK6.7V8ajmQrEPBYbjL66rmGmhZeULQk5K23cRElRpiBGW67YBgbgQxSQQ*jZ1sT2lB3FSogwc0t5DyuSeiAT17yAwmaSTNULPo!/b&bo=aABPAAAAAAABFxc!&rf=viewer_4", botMessage));
            }
        } catch (Exception e) {
            log.error("异步消息处理异常", e);
            if (alwaysReply) {
                botManager.sendMessage(BotMessage.simpleImageMessage("http://m.qpic.cn/psc?/V53UUlnk2IehYn4WcXfY2dBFO92OvB1L/TmEUgtj9EK6.7V8ajmQrENdFC7iq*X8AsvjACl.g*DjfOPu0Ohw4r47052XDpNQGtOBy0dw5ZNtRggzAZvOvUBGBlTjwCDv4o3k*J7IWang!/b&bo=eABcAAAAAAABFxQ!&rf=viewer_4", botMessage));
            }
        }
    }

    public String getSessionKey(BotMessage botMessage) {
        String sendType = botMessage.getSendType();
        switch (sendType) {
            case SendTypeEmum.TEMP_MESSAGE: Asserts.notNull(botMessage.getGroup(), "找不到发送对象");
            case SendTypeEmum.FRIEND_MESSAGE: Asserts.notNull(botMessage.getQq(), "找不到发送对象"); return SendTypeEmum.FRIEND_MESSAGE + "-" + botMessage.getQq();
            case SendTypeEmum.GROUP_MESSAGE: Asserts.notNull(botMessage.getGroup(), "找不到发送对象"); return SendTypeEmum.GROUP_MESSAGE + "-" + botMessage.getGroup();
            case SendTypeEmum.GUILD_MESSAGE: {
                ChannelEmum channel = botMessage.getChannel();
                String guildId = channel != null? channel.guildId: botMessage.getGuildId();
                String channelId = channel != null? channel.channelId: botMessage.getChannelId();
                Asserts.notBlank(guildId, "找不到发送对象");
                Asserts.notBlank(channelId, "找不到发送对象");
                return SendTypeEmum.GUILD_MESSAGE + "-" + guildId + "-" + channelId;
            }
            default: throw new AssertException("未知发送类型："+sendType);
        }
    }

}
