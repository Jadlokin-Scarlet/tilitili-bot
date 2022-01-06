package com.tilitili.bot.service.mirai;

import com.google.common.collect.ImmutableMap;
import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.emnus.GroupEmum;
import com.tilitili.common.entity.PixivImage;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.lolicon.SetuData;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.*;
import com.tilitili.common.mapper.tilitili.PixivImageMapper;
import com.tilitili.common.utils.OSSUtil;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.*;

@Slf4j
@Component
public class PixivHandle extends LockMessageHandle {
    public static final String messageIdKey = "pixiv.messageId";

    private final RedisCache redisCache;
    private final MiraiManager miraiManager;
    private final PixivImageMapper pixivImageMapper;
    private final LoliconManager loliconManager;
    private final PixivManager pixivManager;
    private final BotManager botManager;

    private final Map<String, String> keyMap = ImmutableMap.of("ss", "1", "bs", "0");

    @Autowired
    public PixivHandle(RedisCache redisCache, MiraiManager miraiManager, PixivImageMapper pixivImageMapper, LoliconManager loliconManager, PixivManager pixivManager, BotManager botManager) {
        super("出门找图了，一会儿再来吧Σ（ﾟдﾟlll）");
        this.redisCache = redisCache;
        this.miraiManager = miraiManager;
        this.pixivImageMapper = pixivImageMapper;
        this.loliconManager = loliconManager;
        this.pixivManager = pixivManager;
        this.botManager = botManager;
    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.PIXIV_HANDLE;
    }

    @Override
    public BotMessage handleMessageAfterLock(BotMessageAction messageAction) throws UnsupportedEncodingException, InterruptedException {
        String searchKey = messageAction.getValueOrDefault(messageAction.getParam("tag"));
        String source = messageAction.getParamOrDefault("source", "pixiv");
        String num = messageAction.getParamOrDefault("num", "1");
        String sendMessageId = messageAction.getMessageId();
        BotMessage botMessage = messageAction.getBotMessage();
        Long group = botMessage.getGroup();
        String tinyId = botMessage.getTinyId();
        String titleKey = messageAction.getKey();
        String r18 = keyMap.getOrDefault(titleKey, messageAction.getParamOrDefault("r18", "2"));

        // 群色图限制
        if (group != null) {
            List<Long> groupList = Arrays.asList(GroupEmum.HOMO_LIVE_GROUP.value);
            if (!groupList.contains(group)) {
                if (!r18.equals("0")) {
                    return BotMessage.simpleTextMessage("不准色色o(*////▽////*)q");
                }
            }
        }

        // 频道色图限制
        if (tinyId != null) {
            if (! Objects.equals(tinyId, "144115218678093982")) {
                return null;
            }
        }

        String messageId;
        switch (source) {
            case "lolicon": messageId = sendLoliconImage(botMessage, searchKey == null? "チルノ": searchKey, source, num, r18); break;
            case "pixiv": messageId = pixivManager.sendPixivImage(sendMessageId, searchKey == null? "チルノ 東方Project100users入り": searchKey, source, r18); break;
//            case "powner": messageId = pixivManager.sendPixivImage(sendMessageId, searchKey == null? "チルノ 東方Project100users入り": searchKey, source, r18); break;
            default: throw new AssertException("没有这个平台");
        }
        if (messageId != null) {
            redisCache.setValue(messageIdKey, messageId);
        }
        return BotMessage.emptyMessage();
    }

    private String sendLoliconImage(BotMessage reqBotMessage, String searchKey, String source, String num, String r18) throws InterruptedException, UnsupportedEncodingException {
        List<SetuData> dataList = loliconManager.getAImage(searchKey, num, r18);
        if (dataList.isEmpty()) {
            botManager.sendMessage(BotMessage.simpleTextMessage("没库存啦！", reqBotMessage));
            return null;
        }
        List<BotMessageChain> messageChainList = new ArrayList<>();
        for (int i = 0; i < dataList.size(); i++) {
            SetuData data = dataList.get(i);
            String pid = String.valueOf(data.getPid());
            String imageUrl = data.getUrls().getOriginal();
            boolean isSese = data.getTags().contains("R-18") || data.getR18();
            if (i != 0) {
                messageChainList.add(new BotMessageChain().setType("Plain").setText("\n"));
            }
            if (isSese) {
                String ossUrl = OSSUtil.getCacheOSSOrUploadByUrl(imageUrl);
                messageChainList.add(new BotMessageChain().setType("Plain").setText(ossUrl));
            } else {
                messageChainList.add(new BotMessageChain().setType("Plain").setText(pid + "\n"));
                messageChainList.add(new BotMessageChain().setType("Image").setUrl(imageUrl));
            }
        }

        String messageId = botManager.sendMessage(BotMessage.simpleListMessage(messageChainList, reqBotMessage).setQuote(reqBotMessage.getQuote()));

        for (SetuData data : dataList) {
            String pid = String.valueOf(data.getPid());
            String imageUrl = data.getUrls().getOriginal();

            PixivImage pixivImage = new PixivImage();
            pixivImage.setPid(pid);
            pixivImage.setTitle(data.getTitle());
            pixivImage.setPageCount(1);
            pixivImage.setSmallUrl(imageUrl);
            pixivImage.setUserId(String.valueOf(data.getUid()));
            pixivImage.setUrlList(imageUrl);
            pixivImage.setSearchKey(searchKey);
            pixivImage.setSource(source);
            pixivImage.setMessageId(Integer.valueOf(messageId));
            pixivImage.setStatus(1);
            pixivImageMapper.addPixivImageSelective(pixivImage);
        }
        return messageId;
    }
//
//    private Integer sendPixivMoeImage(Sender sendGroup, String searchKey, String source) throws InterruptedException {
//        PixivImage noUsedImage = pixivImageMapper.getNoUsedImage(new PixivImageQuery().setSearchKey(searchKey).setSource(source));
//        if (noUsedImage == null) {
//            List<SearchIllust> dataList = pixivMoeManager.search(searchKey, 1L);
//            Asserts.isFalse(dataList.isEmpty(), "搜不到tag");
//            List<SearchIllust> filterDataList = dataList.stream().filter(data -> pixivImageMapper.getPixivImageByCondition(new PixivImage().setPid(data.getId())).isEmpty()).collect(Collectors.toList());
//
//            if (filterDataList.isEmpty()) {
//                Long pageNo = redisCache.increment(RedisKeyEnum.SPIDER_PIXIV_PAGENO.getKey(), searchKey);
//                filterDataList = pixivMoeManager.search(searchKey, pageNo);
//                Asserts.isFalse(filterDataList.isEmpty(), "搜不到tag");
//            }
//
//            for (SearchIllust data : filterDataList) {
//                String pid = data.getId();
//
//                PixivImage pixivImage = new PixivImage();
//                pixivImage.setPid(pid);
//                pixivImage.setTitle(data.getTitle());
//                pixivImage.setPageCount(data.getPage_count());
//                pixivImage.setSmallUrl(data.getImage_urls().getOriginal());
//                pixivImage.setUserName(data.getUser().getName());
//                pixivImage.setUserId(data.getUser().getId());
//                pixivImage.setSearchKey(searchKey);
//                pixivImage.setSource(source);
//
//                List<PixivImage> oldDataList = pixivImageMapper.getPixivImageByCondition(new PixivImage().setPid(pid).setSource(source));
//                if (oldDataList.isEmpty()) {
//                    pixivImageMapper.addPixivImageSelective(pixivImage);
//                }
//            }
//            noUsedImage = pixivImageMapper.getNoUsedImage(new PixivImageQuery().setSearchKey(searchKey).setSource(source));
//        }
//
//        String url = noUsedImage.getSmallUrl();
//        String pid = noUsedImage.getPid();
//
//        pixivImageMapper.updatePixivImageSelective(new PixivImage().setId(noUsedImage.getId()).setStatus(1));
//        Integer messageId = miraiManager.sendMessage(new MiraiMessage().setMessageType("ImageText").setSendType("GroupMessage").setUrl(url.replace("https://", "https://api.pixiv.moe/image/")).setMessage("https://pixiv.moe/illust/"+pid+"\n").setGroup(sendGroup.getId()));
//        pixivImageMapper.updatePixivImageSelective(new PixivImage().setId(noUsedImage.getId()).setMessageId(messageId));
//        return messageId;
//    }
}
