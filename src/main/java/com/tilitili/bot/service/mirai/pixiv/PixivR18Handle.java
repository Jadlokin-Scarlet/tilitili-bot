package com.tilitili.bot.service.mirai.pixiv;

import com.google.common.collect.ImmutableMap;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.PixivService;
import com.tilitili.bot.service.mirai.LockMessageHandle;
import com.tilitili.common.emnus.GroupEmum;
import com.tilitili.common.entity.PixivImage;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.lolicon.SetuData;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.LoliconManager;
import com.tilitili.common.manager.PixivManager;
import com.tilitili.common.mapper.tilitili.PixivImageMapper;
import com.tilitili.common.utils.OSSUtil;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.*;

import static com.tilitili.common.utils.StringUtils.isNotBlank;

@Slf4j
@Component
public class PixivR18Handle extends LockMessageHandle {
    public static final String messageIdKey = "pixiv.messageId";

    private final RedisCache redisCache;
    private final PixivImageMapper pixivImageMapper;
    private final LoliconManager loliconManager;
    private final PixivManager pixivManager;
    private final BotManager botManager;
    private final PixivService pixivService;

    private final Map<String, String> keyMap = ImmutableMap.of("ss", "1", "bs", "0");

    @Autowired
    public PixivR18Handle(RedisCache redisCache, PixivImageMapper pixivImageMapper, LoliconManager loliconManager, PixivManager pixivManager, BotManager botManager, PixivService pixivService) {
        super("出门找图了，一会儿再来吧Σ（ﾟдﾟlll）");
        this.redisCache = redisCache;
        this.pixivImageMapper = pixivImageMapper;
        this.loliconManager = loliconManager;
        this.pixivManager = pixivManager;
        this.botManager = botManager;
        this.pixivService = pixivService;
    }

	@Override
    public BotMessage handleMessageAfterLock(BotMessageAction messageAction) throws UnsupportedEncodingException, InterruptedException {
        String searchKey = messageAction.getValueOrDefault(messageAction.getParam("tag"));
        String user = messageAction.getParam("u");
        String source = messageAction.getParamOrDefault("source", "pixiv");
        String num = messageAction.getParamOrDefault("num", "1");
        String sendMessageId = messageAction.getMessageId();
        BotMessage botMessage = messageAction.getBotMessage();
        Long group = botMessage.getGroup();
        String tinyId = botMessage.getTinyId();
        String titleKey = messageAction.getKeyWithoutPrefix();
        String r18 = keyMap.getOrDefault(titleKey, messageAction.getParamOrDefault("r18", "2"));

        // 群色图限制
        if (group != null) {
            List<Long> groupList = Arrays.asList(GroupEmum.HOMO_LIVE_GROUP.value, 759168424L);
            if (!groupList.contains(group)) {
                if (!r18.equals("0")) {
                    return BotMessage.simpleTextMessage("不准色色o(*////▽////*)q");
                }
            }
        }

        // 频道色图限制
        if (tinyId != null) {
            if (!r18.equals("0")) {
                return null;
            }
        }

        pixivService.handlePixiv(botMessage, sendMessageId, source, searchKey, user, r18, num);
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
                String ossUrl = OSSUtil.getCacheSOSSOrUploadByUrl(imageUrl);
                messageChainList.add(new BotMessageChain().setType("Plain").setText(ossUrl));
            } else {
                String ossUrl = OSSUtil.getCacheSOSSOrUploadByUrl(imageUrl);
                messageChainList.add(new BotMessageChain().setType("Plain").setText(pid + "\n"));
                messageChainList.add(new BotMessageChain().setType("Image").setUrl(ossUrl));
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
            pixivImage.setMessageId(messageId);
            pixivImage.setStatus(1);
            pixivImageMapper.addPixivImageSelective(pixivImage);
        }
        return messageId;
    }
}
