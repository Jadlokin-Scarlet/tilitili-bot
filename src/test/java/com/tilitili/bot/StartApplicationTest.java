package com.tilitili.bot;

import com.google.gson.reflect.TypeToken;
import com.tilitili.bot.service.mirai.HelpHandle;
import com.tilitili.bot.service.mirai.base.BaseMessageHandle;
import com.tilitili.common.entity.BotIcePrice;
import com.tilitili.common.entity.BotItem;
import com.tilitili.common.entity.dto.BotItemDTO;
import com.tilitili.common.entity.view.bot.mirai.MiraiBaseRequest;
import com.tilitili.common.entity.view.bot.mirai.event.MiraiBotInvitedJoinGroupRequestEvent;
import com.tilitili.common.manager.BotIcePriceManager;
import com.tilitili.common.manager.GoCqhttpManager;
import com.tilitili.common.manager.MiraiManager;
import com.tilitili.common.mapper.mysql.BotItemMapper;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.mapper.mysql.BotSenderTaskMappingMapper;
import com.tilitili.common.mapper.mysql.BotTaskMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.DateUtils;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    @Test
    void main() {
        System.out.println(messageHandleMap);
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
        if (BotItemDTO.ICE_NAME.equals(itemName)) {
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
//            BotSender friendSender = new BotSender().setSendType(SendTypeEmum.FRIEND_MESSAGE_STR).setQq(friend.getId()).setName(friend.getNickname());
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
//            BotSender groupSender = new BotSender().setSendType(SendTypeEmum.GROUP_MESSAGE_STR).setGroup(group.getId()).setName(group.getName());
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
//                BotSender tempSender = new BotSender().setSendType(SendTypeEmum.TEMP_MESSAGE_STR).setGroup(group.getId()).setQq(temp.getId()).setName(temp.getMemberName());
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
//                BotSender guildSender = new BotSender().setSendType(SendTypeEmum.GUILD_MESSAGE_STR).setGuildId(guild.getGuildId()).setChannelId(channel.getChannelId()).setName(channel.getChannelName());
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
    public static void main(String[] args) {
        String s = "{\"syncId\":\"-1\",\"data\":{\"type\":\"BotInvitedJoinGroupRequestEvent\",\"eventId\":1661416058802410,\"message\":\"\",\"fromId\":545459363,\"groupId\":458232866,\"groupName\":\"【东方恋迷踪】超异域恋恋连结\",\"nick\":\"Jadlokin_Scarlet\"}}";
        StartApplicationTest.<MiraiBotInvitedJoinGroupRequestEvent>run(s);
    }

    private static <T> T run(String s) {
        MiraiBaseRequest<T> request = Gsons.fromJson(s, new TypeToken<MiraiBaseRequest<T>>(){}.getType());
        Asserts.isTrue(request.getData() instanceof MiraiBotInvitedJoinGroupRequestEvent, "?");
        return null;
    }

}