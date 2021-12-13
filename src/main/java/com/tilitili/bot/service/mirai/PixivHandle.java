package com.tilitili.bot.service.mirai;

import com.google.common.collect.ImmutableMap;
import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.mirai.MiraiRequest;
import com.tilitili.common.emnus.GroupEmum;
import com.tilitili.common.entity.PixivImage;
import com.tilitili.common.entity.lolicon.SetuData;
import com.tilitili.common.entity.mirai.MessageChain;
import com.tilitili.common.entity.mirai.MiraiMessage;
import com.tilitili.common.entity.mirai.Sender;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.LoliconManager;
import com.tilitili.common.manager.MiraiManager;
import com.tilitili.common.manager.PixivManager;
import com.tilitili.common.manager.PixivMoeManager;
import com.tilitili.common.mapper.tilitili.PixivImageMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.OSSUtil;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class PixivHandle implements BaseMessageHandle {
    public static final String messageIdKey = "pixiv.messageId";

    private final AtomicBoolean lockFlag = new AtomicBoolean(false);

    private final RedisCache redisCache;
    private final MiraiManager miraiManager;
    private final PixivMoeManager pixivMoeManager;
    private final PixivImageMapper pixivImageMapper;
    private final LoliconManager loliconManager;
    private final PixivManager pixivManager;

    private final Map<String, String> keyMap = ImmutableMap.of("ss", "1", "bs", "0");

    @Autowired
    public PixivHandle(RedisCache redisCache, MiraiManager miraiManager, PixivMoeManager pixivMoeManager, PixivImageMapper pixivImageMapper, LoliconManager loliconManager, PixivManager pixivManager) {
        this.redisCache = redisCache;
        this.miraiManager = miraiManager;
        this.pixivMoeManager = pixivMoeManager;
        this.pixivImageMapper = pixivImageMapper;
        this.loliconManager = loliconManager;
        this.pixivManager = pixivManager;
    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.PixivHandle;
    }

    @Override
    public MiraiMessage handleMessage(MiraiRequest request) {
        MiraiMessage result = new MiraiMessage();
        if (!lockFlag.compareAndSet(false, true)) {
            return result.setMessage("出门找图了，一会儿再来吧Σ（ﾟдﾟlll）").setMessageType("Plain");
        }
        try {
            String searchKey = request.getTitleValueOrDefault(request.getParamOrDefault("tag", "チルノ 東方Project100users入り"));
            String source = request.getParamOrDefault("source", "pixiv");
            String num = request.getParamOrDefault("num", "1");
            Long sendMessageId = request.getMessageId();
            Sender sender = request.getMessage().getSender();
            Sender sendGroup = sender.getGroup();
            String titleKey = request.getTitleKey();
            String r18 = keyMap.getOrDefault(titleKey, request.getParamOrDefault("r18", "2"));

            List<Long> groupList = Arrays.asList(GroupEmum.HOMO_LIVE_GROUP.value, GroupEmum.TEST_GROUP.value, 588572960L, 759168424L);
            if (! groupList.contains(sendGroup.getId())) {
                if (! r18.equals("0")) {
                    return result.setMessage("不准色色o(*////▽////*)q").setMessageType("Plain");
                }
            }

            Integer messageId;
            switch (source) {
                case "lolicon": messageId = sendLoliconImage(sendGroup, searchKey, source, num, r18); break;
                case "pixiv": messageId = pixivManager.sendPixivImage(sendMessageId, searchKey, source, r18); break;
                default: throw new AssertException("不支持的平台");
            }
            Asserts.notNull(messageId, "发送失败");
            redisCache.setValue(messageIdKey, String.valueOf(messageId));
            return result.setMessage("").setMessageType("Plain");
        } catch (AssertException e) {
            log.error(e.getMessage());
            lockFlag.set(false);
            throw e;
        } catch (Exception e) {
            log.error("找色图失败",e);
            lockFlag.set(false);
            return null;
        } finally {
            lockFlag.set(false);
        }
    }

    private Integer sendLoliconImage(Sender sendGroup, String searchKey, String source, String num, String r18) throws InterruptedException, UnsupportedEncodingException {
        List<SetuData> dataList = loliconManager.getAImage(searchKey, num, r18);
        if (dataList.isEmpty()) {
            return miraiManager.sendGroupMessage("Plain", "没库存啦！", sendGroup.getId());
        }
        List<MessageChain> messageChainList = new ArrayList<>();
        for (int i = 0; i < dataList.size(); i++) {
            SetuData data = dataList.get(i);
            String pid = String.valueOf(data.getPid());
            String imageUrl = data.getUrls().getOriginal();
            boolean isSese = data.getTags().contains("R-18") || data.getR18();
            if (i != 0) {
                messageChainList.add(new MessageChain().setType("Plain").setText("\n"));
            }
            if (isSese) {
                String ossUrl = OSSUtil.getCacheOSSOrUploadByUrl(imageUrl);
                messageChainList.add(new MessageChain().setType("Plain").setText(ossUrl));
            } else {
                messageChainList.add(new MessageChain().setType("Plain").setText(pid + "\n"));
                messageChainList.add(new MessageChain().setType("Image").setUrl(imageUrl));
            }
        }

        Integer messageId = miraiManager.sendMessage(new MiraiMessage().setMessageType("List").setSendType("GroupMessage").setMessageChainList(messageChainList).setGroup(sendGroup.getId()));

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
            pixivImage.setMessageId(messageId);
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
