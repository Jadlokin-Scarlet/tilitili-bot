package com.tilitili.bot.service;

import com.tilitili.bot.entity.BotRobotDTO;
import com.tilitili.bot.socket.NewWebSocketFactory;
import com.tilitili.common.constant.BotRobotConstant;
import com.tilitili.common.constant.BotRoleConstant;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.emnus.SendTypeEnum;
import com.tilitili.common.entity.*;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotRobotIndexQuery;
import com.tilitili.common.entity.query.BotRobotQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotAdminMapper;
import com.tilitili.common.mapper.mysql.BotRobotIndexMapper;
import com.tilitili.common.mapper.mysql.BotRoleUserMappingMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class BotRobotService {
    private final BotRobotCacheManager botRobotCacheManager;
    private final BotManager botManager;
    private final BotRobotIndexMapper botRobotIndexMapper;
    private final BotUserManager botUserManager;
    private final BotRoleUserMappingMapper botRoleUserMappingMapper;
    private final NewWebSocketFactory webSocketFactory;
    private final BotAdminMapper botAdminMapper;

    public BotRobotService(BotRobotCacheManager botRobotCacheManager, BotManager botManager, BotRobotIndexMapper botRobotIndexMapper, BotUserManager botUserManager, BotRoleUserMappingMapper BotRoleUserMappingMapper, NewWebSocketFactory webSocketFactory, BotAdminMapper botAdminMapper) {
        this.botRobotCacheManager = botRobotCacheManager;
        this.botManager = botManager;
        this.botRobotIndexMapper = botRobotIndexMapper;
        this.botUserManager = botUserManager;
        this.botRoleUserMappingMapper = BotRoleUserMappingMapper;
        this.webSocketFactory = webSocketFactory;
        this.botAdminMapper = botAdminMapper;
    }

    public BaseModel<PageModel<BotRobotDTO>> list(BotUserDTO botUser, BotRobotQuery query) throws InvocationTargetException, IllegalAccessException {
        BotRoleUserMapping adminMapping = botRoleUserMappingMapper.getBotRoleUserMappingByUserIdAndRoleId(botUser.getId(), BotRoleConstant.adminRole);
        if (adminMapping == null) {
            query.setMasterId(botUser.getId());
        }
        int total = botRobotCacheManager.countBotRobotByCondition(query);
        List<BotRobot> list = botRobotCacheManager.getBotRobotByCondition(query);
        List<BotRobotDTO> result = new ArrayList<>();
        for (BotRobot robot : list) {
            BotRobotDTO robotDTO = new BotRobotDTO(robot);
            if (Objects.equals(robot.getPushType(), BotRobotConstant.PUSH_TYPE_WS)) {
                robotDTO.setWsStatus(webSocketFactory.getWsStatus(robot));
            } else if (Objects.equals(robot.getPushType(), BotRobotConstant.PUSH_TYPE_HOOK)) {
                robotDTO.setHookUrl("https://api.bot.tilitili.club/pub/botReport/" + robot.getId());
            }
            result.add(robotDTO);
        }
        return PageModel.of(total, query.getPageSize(), query.getCurrent(), result);
    }

    public void upBot(Long botId) {
        BotRobot bot = botRobotCacheManager.getBotRobotById(botId);
        Asserts.notNull(bot, "参数异常");
        List<BotSender> senderList = botManager.getBotSenderDTOList(bot);
        Asserts.notEmpty(senderList, "bot验证失败");
        int cnt = botRobotCacheManager.updateBotRobotSelective(new BotRobot().setId(botId).setStatus(0));
        Asserts.checkEquals(cnt, 1, "上线失败");
        if (Objects.equals(bot.getPushType(), BotRobotConstant.PUSH_TYPE_WS)) {
            webSocketFactory.upBotBlocking(bot.getId());
        }
    }

    public void downBot(Long botId) {
        BotRobot bot = botRobotCacheManager.getBotRobotById(botId);
        Asserts.notNull(bot, "参数异常");
        int cnt = botRobotCacheManager.updateBotRobotSelective(new BotRobot().setId(botId).setStatus(-1));
        Asserts.checkEquals(cnt, 1, "下线失败");
        if (Objects.equals(bot.getPushType(), BotRobotConstant.PUSH_TYPE_WS)) {
            webSocketFactory.downBotBlocking(botId);
        }
    }

    public void addBot(BotUserDTO botUser, BotRobot bot) {
        Asserts.notNull(bot, "参数异常");
        Asserts.notNull(bot.getType(), "参数异常");
        bot.setStatus(-1);
        bot.setMasterId(botUser.getId());
        BotAdmin botAdmin = botAdminMapper.getBotAdminByUserId(botUser.getId());
        bot.setUserId(botAdmin.getId());
        switch (bot.getType()) {
            case BotRobotConstant.TYPE_MIRAI: {
                BotRobot botInfo = botManager.getBotInfo(bot);
                bot.setName(botInfo.getName());
                if (botInfo.getQq() != null) {
                    bot.setQq(botInfo.getQq());
                }
                bot.setTinyId(botInfo.getTinyId());
                bot.setAuthorId(botInfo.getAuthorId());
                break;
            }
            case BotRobotConstant.TYPE_GOCQ: {
                BotRobot botInfo = botManager.getBotInfo(bot);
                bot.setName(botInfo.getName());
                if (botInfo.getQq() != null) {
                    bot.setQq(botInfo.getQq());
                }
                bot.setTinyId(botInfo.getTinyId());
                bot.setAuthorId(botInfo.getAuthorId());
                break;
            }
            case BotRobotConstant.TYPE_KOOK: {
                this.handleKookBot(bot);
                break;
            }
            case BotRobotConstant.TYPE_MINECRAFT: {
                this.handleMinecraftBot(bot);
                break;
            }
            case BotRobotConstant.TYPE_QQ_GUILD: {
                this.handleQQGuildBot(bot);
                break;
            }
            default: throw new AssertException("参数异常");
        }

    }

    private void handleQQGuildBot(BotRobot bot) {
        Asserts.notNull(bot.getVerifyKey(), "请输入api秘钥");
        Asserts.isTrue(Pattern.matches("\\d+\\.\\w+", bot.getVerifyKey()), "ggGuild的秘钥由appId点secret构成");
        bot.setPushType(BotRobotConstant.PUSH_TYPE_WS);
        bot.setHost("https://api.sgroup.qq.com/");
        bot.setIntents(1073741827);

        BotRobot botInfo = botManager.getBotInfo(bot);
        Asserts.notNull(botInfo, "参数异常");
        bot.setName(botInfo.getName());
        bot.setQqGuildUserId(botInfo.getQqGuildUserId());

        BotUserDTO botUser = botUserManager.addOrUpdateBotUser(bot, new BotSender().setSendType(SendTypeEnum.GUILD_MESSAGE_STR), new BotUserDTO(BotUserConstant.USER_TYPE_QQ_GUILD, botInfo.getQqGuildUserId()).setName(botInfo.getName()));
        bot.setUserId(botUser.getId());

        this.addBotRobot(bot);
    }

    private void handleKookBot(BotRobot bot) {
        Asserts.notNull(bot.getVerifyKey(), "请输入api秘钥");
        bot.setPushType(BotRobotConstant.PUSH_TYPE_WS);
        bot.setHost("https://www.kookapp.cn/");

        BotRobot botInfo = botManager.getBotInfo(bot);
        Asserts.notNull(botInfo, "参数异常");
        bot.setName(botInfo.getName());
        bot.setAuthorId(botInfo.getAuthorId());

        BotUserDTO botUser = botUserManager.addOrUpdateBotUser(bot, new BotSender().setSendType(SendTypeEnum.KOOK_MESSAGE_STR), new BotUserDTO(BotUserConstant.USER_TYPE_KOOK, botInfo.getAuthorId()).setName(botInfo.getName()));
        bot.setUserId(botUser.getId());

        this.addBotRobot(bot);
    }

    private void handleMinecraftBot(BotRobot bot) {
        Asserts.notBlank(bot.getName(), "请输入昵称");
        Asserts.notNull(bot.getHost(), "请输入服务器地址");
        Asserts.notNull(bot.getVerifyKey(), "请输入api秘钥");
        bot.setPushType(BotRobotConstant.PUSH_TYPE_HOOK);
        String host = bot.getHost();
        if (!host.startsWith("http")) {
            host = "http://"+host;
        }
        bot.setHost(host);
        this.addBotRobot(bot);
    }

    private void addBotRobot(BotRobot bot) {
        this.suppleHost(bot);
        botRobotCacheManager.addBotRobotSelective(bot);

        List<Integer> indexTypeList = botRobotIndexMapper.listIndexType();
        for (Integer indexType : indexTypeList) {
            botRobotIndexMapper.addBotRobotIndexSelective(new BotRobotIndex().setBotIndexType(indexType).setBot(bot.getId()).setIndex((int) (bot.getId() * 10)));
        }
    }

    public void deleteBot(Long botId) {
        BotRobot bot = botRobotCacheManager.getBotRobotById(botId);
        Asserts.notNull(bot, "参数异常");
        botRobotCacheManager.deleteBotRobotByPrimary(botId);
        List<BotRobotIndex> indexList = botRobotIndexMapper.getBotRobotIndexByCondition(new BotRobotIndexQuery().setBot(botId));
        for (BotRobotIndex index : indexList) {
            botRobotIndexMapper.deleteBotRobotIndexByPrimary(index.getId());
        }
    }

    public void editBot(BotRobot bot) {
        BotRobot updBot = new BotRobot().setId(bot.getId());
        BotRobot dbBot = botRobotCacheManager.getBotRobotById(bot.getId());
        if (bot.getName() != null && !bot.getName().equals(dbBot.getName())) {
            updBot.setName(bot.getName());
        }
        if (bot.getHost() != null) {
            this.suppleHost(bot);
            if (!bot.getHost().equals(dbBot.getHost())) {
                updBot.setHost(bot.getHost());
            }
        }
        if (bot.getVerifyKey() != null && !bot.getVerifyKey().equals(dbBot.getVerifyKey())) {
            updBot.setVerifyKey(bot.getVerifyKey());
        }
        if (bot.getQq() != null && !bot.getQq().equals(dbBot.getQq())) {
            updBot.setQq(bot.getQq());
        }
        if (bot.getDefaultTaskIdList() != null && !bot.getDefaultTaskIdList().equals(dbBot.getDefaultTaskIdList())) {
            updBot.setDefaultTaskIdList(bot.getDefaultTaskIdList());
        }
        int cnt = botRobotCacheManager.updateBotRobotSelective(updBot);
        Asserts.checkEquals(cnt, 1, "更新失败");
    }

    private void suppleHost(BotRobot bot) {
        String host = bot.getHost();
        if (!host.startsWith("http")) {
            host = "https://"+host;
        }
        if (!host.endsWith("/")) {
            host += "/";
        }
        bot.setHost(host);
    }

    public BotRobot getBot(Long botId) {
        BotRobot bot = botRobotCacheManager.getBotRobotById(botId);
        Asserts.notNull(bot, "参数异常");
        return bot;
    }

//    public void updateTaskList(BotRobot bot) {
//        BotRobot updBot = new BotRobot().setId(bot.getId());
//        BotRobot dbBot = botRobotCacheManager.getBotRobotById(bot.getId());
//        if (bot.getDefaultTaskIdList() != null && !bot.getDefaultTaskIdList().equals(dbBot.getDefaultTaskIdList())) {
//            updBot.setDefaultTaskIdList(bot.getDefaultTaskIdList());
//        }
//        int cnt = botRobotCacheManager.updateBotRobotSelective(updBot);
//        Asserts.checkEquals(cnt, 1, "更新失败");
//    }
}
