package com.tilitili.bot.service;

import com.google.gson.Gson;
import com.tilitili.bot.entity.mirai.MiraiRequest;
import com.tilitili.bot.service.mirai.BaseMessageHandle;
import com.tilitili.common.entity.view.bot.mirai.MiraiMessage;
import com.tilitili.common.entity.view.bot.mirai.MiraiMessageView;
import com.tilitili.common.entity.view.bot.mirai.MiraiMessageViewRequest;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.MiraiManager;
import com.tilitili.common.manager.ResourcesManager;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

import static com.tilitili.common.utils.AsciiUtil.sbc2dbcCase;

@Slf4j
@Service
public class MiraiService {
    @Value("${mirai.master-qq}")
    private Long MASTER_QQ;

    private final List<BaseMessageHandle> messageHandleList;
    private final ResourcesManager resourcesManager;
    private final MiraiManager miraiManager;
    private final MiraiSessionService miraiSessionService;

    @Autowired
    public MiraiService(List<BaseMessageHandle> messageHandleList, ResourcesManager resourcesManager, MiraiManager miraiManager, MiraiSessionService miraiSessionService) {
        this.messageHandleList = messageHandleList;
        this.resourcesManager = resourcesManager;
        this.miraiManager = miraiManager;
        this.miraiSessionService = miraiSessionService;
        this.messageHandleList.sort(Comparator.comparing(a -> a.getType().getSort(), Comparator.reverseOrder()));
    }

    @Async
    public void syncHandleTextMessage(String message) {
        try {
            MiraiMessageViewRequest miraiMessageViewRequest = new Gson().fromJson(message, MiraiMessageViewRequest.class);
            Asserts.notNull(miraiMessageViewRequest, "未获取到消息");
            Asserts.notNull(miraiMessageViewRequest.getData(), "未获取到消息");
            Asserts.notBlank(miraiMessageViewRequest.getData().getType(), "未获取到消息");
            Asserts.notNull(miraiMessageViewRequest.getData().getSender(), "未获取到消息");
            Asserts.notNull(miraiMessageViewRequest.getData().getMessageChain(), "未获取到消息");
            Asserts.notNull(miraiMessageViewRequest.getData().getSender().getId(), "未获取到消息");

            MiraiMessageView miraiMessage = miraiMessageViewRequest.getData();
            MiraiMessage result;

//            Asserts.isFalse(miraiMessage.getType().equals("GroupMessage"), "不支持群聊回复");
            if (miraiMessage.getType().equals("GroupMessage")) {
                Long sender = miraiMessage.getSender().getGroup().getId();
                MiraiSessionService.MiraiSession miraiSession = miraiSessionService.getSession("group-" + sender);
                result = handleGroupMessage(miraiMessage, miraiSession);
            } else {
                Long sender = miraiMessage.getSender().getId();
                MiraiSessionService.MiraiSession miraiSession = miraiSessionService.getSession("friend-" + sender);
                result = handleMessage(miraiMessage, miraiSession);
            }
            Asserts.notNull(result, "无回复");
            Asserts.notBlank((""+result.getMessage()+result.getUrl()+result.getVoiceId()).replaceAll("null", ""), "回复为空");
            if (miraiMessage.getType().equals("FriendMessage")) {
                miraiManager.sendMessage(result.setSendType("FriendMessage").setQq(miraiMessage.getSender().getId()));
            } else if (miraiMessage.getType().equals("TempMessage")){
                miraiManager.sendMessage(result.setSendType("TempMessage").setQq(miraiMessage.getSender().getId()).setGroup(miraiMessage.getSender().getGroup().getId()));
            } else {
                miraiManager.sendMessage(result.setSendType("GroupMessage").setGroup(miraiMessage.getSender().getGroup().getId()));
            }
        } catch (AssertException e) {
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error("异常", e);
        }
    }

    public MiraiMessage handleGroupMessage(MiraiMessageView message, MiraiSessionService.MiraiSession miraiSession) {
        try {
            MiraiRequest miraiRequest = new MiraiRequest(message, miraiSession);

            for (BaseMessageHandle handle : messageHandleList) {
                if (handle.getType().getSendType().equals("GroupMessage")) {
                    if (handle.getType().getKeyword().contains(miraiRequest.getTitleKey()) || handle.getType().getKeyword().isEmpty()) {
                        MiraiMessage result = handle.handleMessage(miraiRequest);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
            return new MiraiMessage().setMessage("").setMessageType("Plain");
        } catch (AssertException e) {
            log.error(e.getMessage());
            return new MiraiMessage().setMessage("").setMessageType("Plain");
        } catch (Exception e) {
            log.error("处理消息回调失败", e);
            return new MiraiMessage().setMessage("").setMessageType("Plain");
        }

    }

    public MiraiMessage handleMessage(MiraiMessageView message, MiraiSessionService.MiraiSession miraiSession) {
        try {
            Long sender = message.getSender().getId();
            MiraiRequest miraiRequest = new MiraiRequest(message, miraiSession);

            if (message.getType().equals("TempMessage")) {
                if (resourcesManager.isForwardTempMessage()) {
                    miraiManager.sendMessage(new MiraiMessage().setMessage(String.format("%s\n%s", sender, miraiRequest.getText())).setMessageType("Plain").setSendType("FriendMessage").setUrl(miraiRequest.getUrl()).setQq(MASTER_QQ));
                }
            }

            for (BaseMessageHandle messageHandle : messageHandleList) {
                if (messageHandle.getType().getSendType().equals("FriendMessage")) {
                    if (messageHandle.getType().getKeyword().contains(sbc2dbcCase(miraiRequest.getTitle()))) {
                        return messageHandle.handleMessage(miraiRequest);
                    }
                }
            }

            return new MiraiMessage().setUrl("http://m.qpic.cn/psc?/V53UUlnk2IehYn4WcXfY2dBFO92OvB1L/TmEUgtj9EK6.7V8ajmQrEPBYbjL66rmGmhZeULQk5K23cRElRpiBGW67YBgbgQxSQQ*jZ1sT2lB3FSogwc0t5DyuSeiAT17yAwmaSTNULPo!/b&bo=aABPAAAAAAABFxc!&rf=viewer_4").setMessageType("Image");
        } catch (AssertException e) {
            log.error(e.getMessage());
            return new MiraiMessage().setMessage(e.getMessage()).setMessageType("Plain");
        } catch (Exception e) {
            log.error("处理消息回调失败",e);
            return new MiraiMessage().setUrl("http://m.qpic.cn/psc?/V53UUlnk2IehYn4WcXfY2dBFO92OvB1L/TmEUgtj9EK6.7V8ajmQrENdFC7iq*X8AsvjACl.g*DjfOPu0Ohw4r47052XDpNQGtOBy0dw5ZNtRggzAZvOvUBGBlTjwCDv4o3k*J7IWang!/b&bo=eABcAAAAAAABFxQ!&rf=viewer_4").setMessageType("Image");
        }
    }
}
