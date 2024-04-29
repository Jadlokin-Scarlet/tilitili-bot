package com.tilitili.bot.service.mirai.pixiv;

import com.google.common.collect.ImmutableMap;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotMessageService;
import com.tilitili.bot.service.PixivCacheService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotTask;
import com.tilitili.common.entity.PixivLoginUser;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoIllust;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoTag;
import com.tilitili.common.entity.view.bot.pixiv.PixivRecommendIllust;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.PixivCacheManager;
import com.tilitili.common.mapper.mysql.BotTaskMapper;
import com.tilitili.common.mapper.mysql.PixivLoginUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.CollectionUtils;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PixivHandle extends ExceptionRespMessageHandle {
    private static final String pixivLikeRecommendKey = "pixivLikeRecommendKey-";
    private static final String pixivLikeRecommendIndexKey = "pixivLikeRecommendIndexKey-";

    private final static String pixivImageKey = "pixivImageKey-";
    public final static String pixivImageListKey = "pixivImageListKey-";
    public final static String pixivImageListPageNoKey = "pixivImageListPageNoKey-";

    private final PixivCacheService pixivService;
    private final BotTaskMapper botTaskMapper;
    private final BotMessageService botMessageService;
    private final PixivCacheManager pixivManager;
    private final PixivLoginUserMapper pixivLoginUserMapper;
    private final RedisCache redisCache;

    private final Map<String, String> keyModeMap = ImmutableMap.of("推荐色图", "all", "推荐色色", "r18", "推荐不色", "safe", "tjst", "all", "tjss", "r18", "tjbs", "safe");

    @Autowired
    public PixivHandle(PixivCacheService pixivService, BotTaskMapper botTaskMapper, BotMessageService botMessageService, PixivCacheManager pixivManager, PixivLoginUserMapper pixivLoginUserMapper, RedisCache redisCache) {
        this.pixivService = pixivService;
        this.botTaskMapper = botTaskMapper;
        this.botMessageService = botMessageService;
        this.pixivManager = pixivManager;
        this.pixivLoginUserMapper = pixivLoginUserMapper;
        this.redisCache = redisCache;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws UnsupportedEncodingException, InterruptedException {
        switch (messageAction.getKeyWithoutPrefix()) {
            case "bs": case "不色": return handlePixiv(messageAction);
            case "pid": return handlePid(messageAction);
            case "scst": case "收藏色图": return handleBookmark(messageAction);
            case "xstj": case "相似推荐": return handleLikeRecommend(messageAction);
            case "推荐色图": case "推荐不色": case "推荐色色": case "tjst": case "tjss": case "tjbs": case "好": case "不": return handleRecommend(messageAction);
            case "puser": return handleUser(messageAction);
            case "tag": return handleTag(messageAction);
            default: throw new AssertException();
        }
    }

    private BotMessage handleTag(BotMessageAction messageAction) {
        String pid = messageAction.getBodyOrDefault("pid", messageAction.getValue());
        if (StringUtils.isBlank(pid)) {
            pid = botMessageService.getQuotePid(messageAction);
        }
        if (StringUtils.isBlank(pid)) {
            pid = pixivService.findPixivImage(botMessageService.getFirstImageListOrQuoteImage(messageAction));
        }
        Asserts.isNumber(pid, "格式错啦(pid)");
        PixivInfoIllust info = pixivManager.getInfoProxy(pid);
        String tagList = info.getTags().getTags().stream().map(PixivInfoTag::getTag).collect(Collectors.joining("    "));
        Asserts.notBlank(tagList, "没找到图片");
        return BotMessage.simpleTextMessage(String.format("[%s]的tag有：%s", pid, tagList));
    }

    private BotMessage handleUser(BotMessageAction messageAction) {
        String pid = messageAction.getBodyOrDefault("pid", messageAction.getValue());
        if (StringUtils.isBlank(pid)) {
            pid = botMessageService.getQuotePid(messageAction);
        }
        if (StringUtils.isBlank(pid)) {
            pid = pixivService.findPixivImage(botMessageService.getFirstImageListOrQuoteImage(messageAction));
        }
        Asserts.isNumber(pid, "格式错啦(pid)");

        PixivInfoIllust info = pixivManager.getInfoProxy(pid);
        String userName = info.getUserName();

        return BotMessage.simpleTextMessage(String.format("[%s]的作者是 %s", pid, userName));
    }

    private BotMessage handleRecommend(BotMessageAction messageAction) {
        BotRobot bot = messageAction.getBot();
        BotMessage botMessage = messageAction.getBotMessage();
        BotUserDTO botUser = messageAction.getBotUser();
        Long userId = botUser.getId();
        log.debug("PixivRecommendHandle start");

        String key = messageAction.getKeyWithoutPrefix();
        boolean isBookmark = Objects.equals(key, "好");
        boolean isNoBookmark = Objects.equals(key, "不");
        String mode = keyModeMap.getOrDefault(key, "all");
        log.debug("PixivRecommendHandle get cookie");


        BotSender botSender = messageAction.getBotSender();
        List<BotTask> botTaskList = botTaskMapper.getBotTaskListBySenderIdAndKey(botSender.getId(), "ss", "");
        boolean canSS = !botTaskList.isEmpty();

        if (!canSS) {
            Asserts.notEquals(mode, "r18", "不准色色");
        }

        String pixivImageRedisKey = pixivImageKey + userId;

        PixivLoginUser pixivLoginUser = pixivLoginUserMapper.getPixivLoginUserByUserId(userId);
        if ((isBookmark || isNoBookmark) && !redisCache.exists(pixivImageRedisKey)) {
            log.info("无可收藏的pid");
            return null;
        }
        Asserts.notNull(pixivLoginUser, "先私聊绑定pixiv账号吧。");
        String cookie = pixivLoginUser.getCookie();
        Asserts.notBlank(cookie, "先私聊绑定pixiv账号吧。");

        log.debug("PixivRecommendHandle bookmark");
        if (isBookmark) {
            String pidAndMode = (String) redisCache.getValue(pixivImageRedisKey);
            String pid = pidAndMode.split("_")[0];
            mode = pidAndMode.split("_")[1];
            if (pid != null) {
                String token = pixivManager.getPixivToken(userId, cookie);
                pixivManager.bookmarkImageForCookie(pid, cookie, token);
            } else {
                return null;
            }
            redisCache.delete(pixivImageListKey + userId + mode);
            redisCache.delete(pixivImageListPageNoKey + userId + mode);
        }
        if (isNoBookmark) {
            String pidAndMode = (String) redisCache.getValue(pixivImageRedisKey);
            mode = pidAndMode.split("_")[1];
        }

        String pixivImageListRedisKey = pixivImageListKey + userId + mode;
        String pixivImageListPageNoRedisKey = pixivImageListPageNoKey + userId + mode;

        log.debug("PixivRecommendHandle get info");
        PixivRecommendIllust illust;
        while (true) {
            List<PixivRecommendIllust> illustList;
            if (redisCache.exists(pixivImageListRedisKey)) {
                illustList = (List<PixivRecommendIllust>) redisCache.getValue(pixivImageListRedisKey);
            } else {
                redisCache.delete(pixivImageListPageNoRedisKey);
                illustList = pixivManager.getRecommendImageByCookie(cookie, mode);
                redisCache.setValue(pixivImageListRedisKey, illustList);
            }
            if (CollectionUtils.isEmpty(illustList)) {
                return BotMessage.simpleTextMessage("啊嘞，怎么会找不到呢。");
            }
            int pageNo = Math.toIntExact(redisCache.increment(pixivImageListPageNoRedisKey)) - 1;
            if (pageNo >= illustList.size()) {
                redisCache.delete(pixivImageListRedisKey);
                redisCache.delete(pixivImageListPageNoRedisKey);
                continue;
            }
            illust = illustList.get(pageNo);
            boolean isSese = illust.getSl() > 5;
            // 推荐无法完全过滤色色和不色
            if ("safe".equals(mode) && isSese) {
                continue;
            } else if ("r18".equals(mode) && !isSese) {
                continue;
            }
            break;
        }

        String pid = illust.getId();
        Integer sl = illust.getSl();
        Integer pageCount = illust.getPageCount();

        log.debug("PixivRecommendHandle get make messageChainList");
        List<BotMessageChain> messageChainList = pixivService.getImageChainList(bot, botSender, illust.getTitle(), illust.getUserName(), pid, sl, pageCount, canSS);

        log.debug("PixivRecommendHandle save result");
        redisCache.setValue(pixivImageRedisKey, pid + "_" + mode, 120);
        log.debug("PixivRecommendHandle send");
        return BotMessage.simpleListMessage(messageChainList);
    }

    private BotMessage handleLikeRecommend(BotMessageAction messageAction) {
        BotRobot bot = messageAction.getBot();
        String pid = messageAction.getBodyOrDefault("pid", messageAction.getValue());
        if (StringUtils.isBlank(pid)) {
            pid = botMessageService.getQuotePid(messageAction);
        }
        if (StringUtils.isBlank(pid)) {
            pid = pixivService.findPixivImage(botMessageService.getFirstImageListOrQuoteImage(messageAction));
        }
        Asserts.isNumber(pid, "格式错啦(pid)");

        BotMessage botMessage = messageAction.getBotMessage();

        BotSender botSender = messageAction.getBotSender();
        List<BotTask> botTaskList = botTaskMapper.getBotTaskListBySenderIdAndKey(botSender.getId(), "ss", "");
        boolean canSS = !botTaskList.isEmpty();

        log.debug("PixivRecommendHandle get info");
        PixivInfoIllust illust;
        while (true) {
            int index = Math.toIntExact(redisCache.increment(pixivLikeRecommendIndexKey + pid)) - 1;
            List<String> pidList;
            if (redisCache.exists(pixivLikeRecommendKey + pid)) {
                pidList = (List<String>) redisCache.getValue(pixivLikeRecommendKey + pid);
            } else {
                pidList = pixivManager.getLikeImageRecommend(pid);
                redisCache.setValue(pixivLikeRecommendKey + pid, pidList);
            }
            if (pidList == null || index >= pidList.size()) {
                if (index < 2) return BotMessage.simpleTextMessage("啊嘞，没找到。");
                return BotMessage.simpleTextMessage("已经看光光了。");
            }
            String recommendPid = pidList.get(index);
            try {
                illust = pixivManager.getInfoProxy(recommendPid);
            } catch (AssertException e) {
                log.error("获取相关推荐失败, pid={}, message={}", recommendPid, e.getMessage());
                continue;
            }
            if (!canSS && illust.getSl() > 3) {
                continue;
            }
            break;
        }

        String recommendPid = illust.getId();
        Integer sl = illust.getSl();
        Integer pageCount = illust.getPageCount();

        log.debug("PixivRecommendHandle get make messageChainList");
        List<BotMessageChain> messageChainList = pixivService.getImageChainList(bot, botSender, illust.getTitle(), illust.getUserName(), recommendPid, sl, pageCount, canSS);
        return BotMessage.simpleListMessage(messageChainList);
    }

    private BotMessage handleBookmark(BotMessageAction messageAction) {
        BotMessage botMessage = messageAction.getBotMessage();
        BotUserDTO botUser = messageAction.getBotUser();
        Long userId = botUser.getId();

        String pid = messageAction.getBodyOrDefault("pid", messageAction.getValue());
        if (StringUtils.isBlank(pid)) {
            pid = botMessageService.getQuotePid(messageAction);
        }
        if (StringUtils.isBlank(pid)) {
            String url = botMessageService.getFirstImageListOrQuoteImage(messageAction);
            pid = pixivService.findPixivImage(url);
        }
        Asserts.isNumber(pid, "格式错啦(pid)");

        Asserts.notNull(pixivManager.getPageListProxy(pid), "啊嘞，不对劲");

        PixivLoginUser pixivLoginUser = pixivLoginUserMapper.getPixivLoginUserByUserId(userId);
        Asserts.notNull(pixivLoginUser, "先私聊绑定pixiv账号吧。");
        String cookie = pixivLoginUser.getCookie();
        Asserts.notBlank(cookie, "先私聊绑定pixiv账号吧。");

        String token = pixivManager.getPixivToken(userId, cookie);
        pixivManager.bookmarkImageForCookie(pid, cookie, token);

        for (String mode : Arrays.asList("all", "safe", "r18")) {
            redisCache.delete(pixivImageListKey + userId + mode);
            redisCache.delete(pixivImageListPageNoKey + userId + mode);
        }
        return BotMessage.simpleTextMessage("搞定！");
    }

    private BotMessage handlePid(BotMessageAction messageAction) {
        BotRobot bot = messageAction.getBot();
        String pid = messageAction.getBodyOrDefault("pid", messageAction.getValue());
        if (StringUtils.isBlank(pid)) {
            pid = botMessageService.getQuotePid(messageAction);
        }
        if (StringUtils.isBlank(pid)) {
            pid = pixivService.findPixivImage(botMessageService.getFirstImageListOrQuoteImage(messageAction));
        }
        Asserts.isNumber(pid, "格式错啦(pid)");

        BotSender botSender = messageAction.getBotSender();
        List<BotTask> botTaskList = botTaskMapper.getBotTaskListBySenderIdAndKey(botSender.getId(), "ss", "");
        boolean canSS = !botTaskList.isEmpty();

        PixivInfoIllust info = pixivManager.getInfoProxy(pid);
        String title = info.getTitle();
        Integer sl = info.getSl();
        Integer pageCount = info.getPageCount();
        String userName = info.getUserName();
        List<BotMessageChain> messageChainList = pixivService.getImageChainList(bot, botSender, title, userName, pid, sl, pageCount, canSS);
        return BotMessage.simpleListMessage(messageChainList);
    }

    private BotMessage handlePixiv(BotMessageAction messageAction) throws UnsupportedEncodingException {
        String searchKey = messageAction.getValueOrDefault(messageAction.getBody("tag"));
        String user = messageAction.getBody("u");
        String source = messageAction.getBodyOrDefault("source", "pixiv");
        String num = messageAction.getBodyOrDefault("num", "1");
        String r18 = "safe";

        return pixivService.handlePixiv(messageAction, source, searchKey, user, r18, num);
    }

}
