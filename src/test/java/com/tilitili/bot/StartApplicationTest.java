package com.tilitili.bot;

import com.google.common.collect.ImmutableMap;
import com.tilitili.bot.entity.ExcelResult;
import com.tilitili.bot.entity.FishConfigDTO;
import com.tilitili.bot.service.BotService;
import com.tilitili.bot.service.MusicService;
import com.tilitili.bot.service.PixivCacheService;
import com.tilitili.bot.service.mirai.HelpHandle;
import com.tilitili.bot.service.mirai.base.BaseMessageHandle;
import com.tilitili.bot.socket.BotWebSocketHandler;
import com.tilitili.bot.socket.QQGuildWebSocketHandler;
import com.tilitili.bot.util.ExcelUtil;
import com.tilitili.common.constant.BotItemConstant;
import com.tilitili.common.entity.BotIcePrice;
import com.tilitili.common.entity.BotItem;
import com.tilitili.common.entity.FishConfig;
import com.tilitili.common.entity.query.FishConfigQuery;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.*;
import com.tilitili.common.mapper.mysql.*;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.DateUtils;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
class StartApplicationTest {
    @Autowired
    private HelpHandle helpHandle;
    @Autowired
    private RedisCache redisCache;
    @Autowired
    private MiraiManager miraiManager;
    @Autowired
    private GoCqhttpManager goCqhttpManager;
    @Autowired
    private BotTaskMapper botTaskMapper;
    @Autowired
    private BotSenderMapper botSenderMapper;
    @Autowired
    private BotSenderTaskMappingMapper botSenderTaskMappingMapper;
    @Autowired
    private Map<String, BaseMessageHandle> messageHandleMap;
    @Autowired
    private BotUserMapper botUserMapper;
    @Autowired
    private PixivCacheService pixivCacheService;
    @Autowired
    private FishConfigMapper fishConfigMapper;
    @Resource
    private MusicService musicService;
    @Autowired
    private BotRobotMapper botRobotMapper;
    @Autowired
    private BotService botService;
    @Autowired
    private SendMessageManager sendMessageManager;
    @Autowired
    private BotManager botManager;

    @Test
    public void qqGuildWebsocketTest() throws URISyntaxException {
        QQGuildWebSocketHandler qqGuildWebSocketHandler = new QQGuildWebSocketHandler(
                new URI("wss://api.sgroup.qq.com/websocket/"),
                botRobotMapper.getValidBotRobotById(9L),
                botService, sendMessageManager
        );
        qqGuildWebSocketHandler.connect();
        BotWebSocketHandler gocq = new BotWebSocketHandler(
                new URI(botManager.getWebSocketUrl(botRobotMapper.getValidBotRobotById(3L))),
                botRobotMapper.getValidBotRobotById(3L),
                botService, sendMessageManager
        );
        gocq.connect();
        TimeUtil.millisecondsSleep(1000* 60 * 60);
    }

    @Test
    public void musicService() throws UnsupportedEncodingException {
//        musicService.asyncPushVideoAsRTSP(4504L, "http://music.163.com/song/media/outer/url?sc=wmv&id=28018303");
//        TimeUtil.millisecondsSleep(9999999);
    }

    public static void main(String[] args) {
        OkHttpClient client = new OkHttpClient.Builder().pingInterval(30, TimeUnit.SECONDS).build();
    }

    private final static Map<String, Integer> map = ImmutableMap.of("石头", 0, "剪刀", 1, "布", 2);
    private final static Map<String, Integer> map2 = new HashMap<String, Integer>(){{
        put("石头", 0);
        put("剪刀", 1);
        put("布", 2);
    }};
    private static Integer compare(String as, String bs) {
        Integer a = map.get(as);
        Integer b = map.get(bs);
        return Integer.compare((a + (4 - b)) % 3, 1);
    }

    @Test
    void main() throws UnsupportedEncodingException {
//        BotSender botSender = botSenderMapper.getBotSenderById(BotSenderConstant.TEST_SENDER_ID);
//        BotUser botUser = botUserMapper.getBotUserById(BotUserConstant.MASTER_USER_ID);
//        BotMessageAction messageAction = new BotMessageAction(BotMessage.emptyMessage().setSender(botSender), null, botSender, botUser, BotEnum.CIRNO_QQ);
//        BotMessage botMessage = pixivCacheService.handlePixiv(messageAction, "pixiv", "碧蓝档案", null, "safe", null);
//        System.out.println(Gsons.toJson(botMessage));
    }

    @Test
    public void test3() {
        File file = new File("/Users/admin/Downloads/钓鱼奖励配置.xlsx");
        ExcelResult<FishConfigDTO> excelResult = ExcelUtil.getListFromExcel(file, FishConfigDTO.class);
        List<FishConfigDTO> resultList = excelResult.getResultList();
        log.info("{}", resultList);
        List<FishConfig> newFishConfigList = new ArrayList<>();
        for (FishConfigDTO config : resultList) {
            try {
                int scale = "小".equals(config.getScaleStr()) ? 0 : 1;
                Integer cost = config.getCost();
                Asserts.notNull(cost, "格式错啦(cost)");
                Integer rate = config.getRate();
                Asserts.notNull(rate, "格式错啦(rate)");
                Integer price = config.getPrice();
                String type = config.getType();
                Asserts.notNull(type, "格式错啦(type)");
                if ("事件".equals(type)) {
                    String desc = config.getDesc();
                    newFishConfigList.add(new FishConfig().setDescription(desc).setScale(scale).setCost(cost).setRate(rate).setPrice(price));
                } else {
                    Asserts.notNull(price, "格式错啦(price)");
                    String itemName = config.getItemName();
                    String itemDesc = config.getItemDesc();
                    String itemGrade = config.getItemGrade();
                    Asserts.notNull(itemName, "格式错啦(itemName)");
                    Asserts.notNull(itemDesc, "格式错啦(itemDesc)");
                    Asserts.notNull(itemGrade, "格式错啦(itemGrade)");
                    BotItem botItem = botItemMapper.getBotItemByName(itemName);
                    if (botItem == null) {
                        botItem = new BotItem().setName(itemName).setDescription(itemDesc).setSellPrice(price).setGrade(itemGrade);
                        botItemMapper.addBotItemSelective(botItem);
                    } else {
                        botItemMapper.updateBotItemSelective(new BotItem().setId(botItem.getId()).setDescription(itemDesc).setSellPrice(price).setGrade(itemGrade));
                    }
                    newFishConfigList.add(new FishConfig().setItemId(botItem.getId()).setScale(scale).setCost(cost).setRate(rate));
                }
            } catch (AssertException e) {
                log.info(e.getMessage());
            } catch (Exception e) {
                log.info("格式不对");
            }
        }
        for (FishConfig fishConfig : fishConfigMapper.getFishConfigByCondition(new FishConfigQuery())) {
            fishConfigMapper.deleteFishConfigByPrimary(fishConfig.getId());
        }
        for (FishConfig fishConfig : newFishConfigList) {
            fishConfigMapper.addFishConfigSelective(fishConfig);
        }
    }

    @Test
    public void test2() {
        String itemName = "蓝冰";
        BotItem botItem = this.getBotItemByNameOrIce(itemName);
        Asserts.notNull(botItem, "那是啥");
        List<String> resultList = new ArrayList<>();
        resultList.add("*" + botItem.getName() + "*");
        resultList.add(botItem.getDescription());
        if (Objects.equals(botItem.getPrice(), botItem.getSellPrice())) {
            if (botItem.getPrice() == null) {
                resultList.add("无法交易");
            } else {
                resultList.add("价值：" + botItem.getPrice());
            }
        } else {
            if (botItem.getPrice() == null) {
                resultList.add("无法兑换");
            } else {
                resultList.add("兑换价：" + botItem.getPrice());
            }
            if (botItem.getSellPrice() == null) {
                resultList.add("无法回收");
            } else {
                resultList.add("回收价：" + botItem.getSellPrice());
            }
        }
        if (botItem.getMaxLimit() != null) {
            resultList.add("最大持有：" + botItem.getMaxLimit());
        }
        if (botItem.getEndTime() != null) {
            resultList.add("有效期至：" + DateUtils.formatDateYMDHMS(botItem.getEndTime()));
        }
        log.info(String.join("\n", resultList));
    }

    @Autowired
    private BotItemMapper botItemMapper;
    @Autowired
    private BotIcePriceManager botIcePriceManager;

    private BotItem getBotItemByNameOrIce(String itemName) {
        BotItem botItem = botItemMapper.getBotItemByName(itemName);
        if (BotItemConstant.ICE_NAME.equalsIgnoreCase(itemName)) {
            BotIcePrice icePrice = botIcePriceManager.getIcePrice();
            botItem.setPrice(icePrice.getBasePrice());
            botItem.setSellPrice(icePrice.getPrice());
        }
        return botItem;
    }

    @Test
    void test() {
//        botSenderMapper.deleteAllBotSender();
//        botSenderTaskMappingMapper.deleteAllMapping();
//        // friend message
//        List<MiraiFriend> friendList = miraiManager.getFriendList();
//        List<Long> qqList = friendList.stream().map(MiraiFriend::getId).collect(Collectors.toList());
//        for (MiraiFriend friend : friendList) {
//            BotSender friendSender = new BotSender().setSendType(SendTypeEnum.FRIEND_MESSAGE_STR).setQq(friend.getId()).setName(friend.getNickname());
//            botSenderMapper.addBotSenderSelective(friendSender);
//            List<Integer> list = new ArrayList<>();
//            if (Arrays.asList(66600000L, 1701008067L).contains(friendSender.getQq())) continue;
//
//            if (friendSender.getQq().equals(545459363L)) {
//                list.addAll(Arrays.asList(1,2,3,4,5,6,7,8,9,10,11,12,14,15,17,18,19,20,21,22,23,24,25));
//            } else {
//                list.addAll(Arrays.asList(3,4,6,7,8,9,10,11,14,17,20,21,22,23,24,25));
//            }
//            list.stream().map(Long::valueOf).forEach(taskId -> botSenderTaskMappingMapper.addBotSenderTaskMappingSelective(new BotSenderTaskMapping().setSenderId(friendSender.getId()).setTaskId(taskId)));
//        }
//        // group message
//        List<MiraiGroup> groupList = miraiManager.getGroupList();
//        for (MiraiGroup group : groupList) {
//            BotSender groupSender = new BotSender().setSendType(SendTypeEnum.GROUP_MESSAGE_STR).setGroup(group.getId()).setName(group.getName());
//            botSenderMapper.addBotSenderSelective(groupSender);
//            List<Integer> list = new ArrayList<>();
//            if (groupSender.getGroup().equals(RANK_GROUP.value)) {
//                list.addAll(Arrays.asList(1,2,3,4,6,7,8,9,10,11,14,15,16,17,20,21,22,23,24,25));
//            } else if (groupSender.getGroup().equals(TEST_GROUP.value)) {
//                list.addAll(Arrays.asList(1,2,3,4,5,6,7,8,9,10,11,12,14,15,16,17,20,21,22,23,24,25));
//            } else {
//                list.addAll(Arrays.asList(3,4,6,7,8,9,10,11,14,15,16,17,20,21,22,23,24,25));
//            }
//            list.stream().map(Long::valueOf).forEach(taskId -> botSenderTaskMappingMapper.addBotSenderTaskMappingSelective(new BotSenderTaskMapping().setSenderId(groupSender.getId()).setTaskId(taskId)));
//
//            // temp message
//            List<MiraiFriend> tempList = miraiManager.getMemberList(group.getId());
//            for (MiraiFriend temp : tempList) {
//                if (qqList.contains(temp.getId())) continue;
//
//                BotSender tempSender = new BotSender().setSendType(SendTypeEnum.TEMP_MESSAGE_STR).setGroup(group.getId()).setQq(temp.getId()).setName(temp.getMemberName());
//                botSenderMapper.addBotSenderSelective(tempSender);
//
//                List<Integer> list2 = new ArrayList<>(Arrays.asList(3,4,6,7,8,9,10,11,14,15,17,20,21,22,23,24,25));
//                list2.stream().map(Long::valueOf).forEach(taskId -> botSenderTaskMappingMapper.addBotSenderTaskMappingSelective(new BotSenderTaskMapping().setSenderId(tempSender.getId()).setTaskId(taskId)));
//            }
//        }
//        // guild message
//        List<GoCqhttpGuild> guildList = goCqhttpManager.getGuildList();
//        for (GoCqhttpGuild guild : guildList) {
//            List<GoCqhttpChannel> channelList = goCqhttpManager.getGuildChannelList(guild.getGuildId());
//            for (GoCqhttpChannel channel : channelList) {
//                BotSender guildSender = new BotSender().setSendType(SendTypeEnum.GUILD_MESSAGE_STR).setGuildId(guild.getGuildId()).setChannelId(channel.getChannelId()).setName(channel.getChannelName());
//                botSenderMapper.addBotSenderSelective(guildSender);
//                List<Integer> list = new ArrayList<>();
//                if (channel.getChannelId().equals(CAI_HONG_GUANZHU_CHANNEL.channelId)) {// 转播自助用
//                    list.addAll(Arrays.asList(3,4,11));
//                } else if (channel.getChannelId().equals(OUR_HOMO_BOT_CHANNEL.channelId)) {// Bot灌水
//                    list.addAll(Arrays.asList(3,4,6,7,8,9,10,11,14,15,17,20,21,22,23,24,25));
//                } else if (channel.getChannelId().equals(OUR_HOMO_TEST_CHANNEL.channelId)) {// test
//                    list.addAll(Arrays.asList(3,4,6,7,8,9,10,11,14,15,17,18,20,21,22,23,24,25));
//                } else if (channel.getChannelId().equals(TOUHOU_GUILD_WATCH_CHANNEL.channelId)) {// 石油之海 | 查级灌水
//                    list.addAll(Arrays.asList(3,4,6,7,8,9,10,11,14,15,17,20,21,22,23,24,25));
//                } else if (channel.getChannelId().equals(TOUHOU_GUILD_VIDEO_CHANNEL.channelId)) {// 伏瓦鲁魔法图书馆
//                    list.addAll(Arrays.asList(11,21));
//                } else if (channel.getChannelId().equals(CAI_HONG_IMAGE_CHANNEL.channelId)) {// 不可以色色的女V图片区
//                    list.addAll(Arrays.asList(11,17,22));
//                } else if (channel.getChannelId().equals(CAI_HONG_GAME_CHANNEL.channelId)) {// 游戏讨论
//                    list.addAll(Arrays.asList(11,25));
//                } else {
//                    list.addAll(Arrays.asList(11));
//                }
//                list.stream().map(Long::valueOf).forEach(taskId -> botSenderTaskMappingMapper.addBotSenderTaskMappingSelective(new BotSenderTaskMapping().setSenderId(guildSender.getId()).setTaskId(taskId)));
//            }
//        }
    }

    @Test
    public void test4() {
        redisCache.addMapValue("test", "test", "lock");
        System.out.println(redisCache.removeMapValue("test", "test"));
        System.out.println(redisCache.removeMapValue("test", "test"));
    }

}