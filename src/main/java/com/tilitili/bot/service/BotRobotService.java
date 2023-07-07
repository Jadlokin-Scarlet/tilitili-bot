package com.tilitili.bot.service;

import com.tilitili.bot.config.WebSocketConfig;
import com.tilitili.bot.entity.BotRobotDTO;
import com.tilitili.bot.socket.BotWebSocketHandler;
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
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotRobotIndexMapper;
import com.tilitili.common.mapper.mysql.BotRobotMapper;
import com.tilitili.common.mapper.mysql.BotRoleAdminMappingMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class BotRobotService {
    private final BotRobotMapper botRobotMapper;
    private final WebSocketConfig webSocketConfig;
    private final BotManager botManager;
    private final BotRobotIndexMapper botRobotIndexMapper;
    private final BotUserManager botUserManager;
    private final BotRoleAdminMappingMapper botRoleAdminMappingMapper;

    public BotRobotService(BotRobotMapper botRobotMapper, @Nullable WebSocketConfig webSocketConfig, BotManager botManager, BotRobotIndexMapper botRobotIndexMapper, BotUserManager botUserManager, BotRoleAdminMappingMapper BotRoleAdminMappingMapper) {
        this.botRobotMapper = botRobotMapper;
        this.webSocketConfig = webSocketConfig;
        this.botManager = botManager;
        this.botRobotIndexMapper = botRobotIndexMapper;
        this.botUserManager = botUserManager;
        this.botRoleAdminMappingMapper = BotRoleAdminMappingMapper;
    }

    public BaseModel<PageModel<BotRobotDTO>> list(BotAdmin botAdmin, BotRobotQuery query) throws InvocationTargetException, IllegalAccessException {
        BotRoleAdminMapping adminMapping = botRoleAdminMappingMapper.getBotRoleAdminMappingByAdminIdAndRoleId(botAdmin.getId(), BotRoleConstant.adminRole);
        if (adminMapping == null) {
            query.setAdminId(botAdmin.getId());
        }
        int total = botRobotMapper.countBotRobotByCondition(query);
        List<BotRobot> list = botRobotMapper.getBotRobotByCondition(query);
        List<BotRobotDTO> result = new ArrayList<>();
        for (BotRobot robot : list) {
            BotRobotDTO robotDTO = new BotRobotDTO(robot);
            if (Objects.equals(robot.getPushType(), "ws")) {
                if (webSocketConfig != null) {
                    BotWebSocketHandler handler = webSocketConfig.getBotWebSocketHandlerMap().get(robot.getId());
                    robotDTO.setWsStatus(handler == null? -1: handler.getStatus());
                }
            } else if (Objects.equals(robot.getPushType(), "hook")) {
                robotDTO.setHookUrl("https://api.bot.tilitili.club/pub/botReport/" + robot.getId());
            }
            result.add(robotDTO);
        }
        return PageModel.of(total, query.getPageSize(), query.getCurrent(), result);
    }

    public void upBot(BotAdmin botAdmin, Long id) {
        BotRobot bot = botRobotMapper.getBotRobotById(id);
        Asserts.notNull(bot, "参数异常");
        BotRoleAdminMapping adminMapping = botRoleAdminMappingMapper.getBotRoleAdminMappingByAdminIdAndRoleId(botAdmin.getId(), BotRoleConstant.adminRole);
        if (adminMapping == null) {
            Asserts.checkEquals(bot.getAdminId(), botAdmin.getId(), "权限异常");
        }
        List<BotSender> senderList = botManager.getBotSenderDTOList(bot);
        Asserts.notEmpty(senderList, "bot验证失败");
        int cnt = botRobotMapper.updateBotRobotSelective(new BotRobot().setId(id).setStatus(0));
        Asserts.checkEquals(cnt, 1, "上线失败");
        if (Objects.equals(bot.getPushType(), "ws") && webSocketConfig != null) {
            webSocketConfig.upBot(id);
        }
    }

    public void downBot(BotAdmin botAdmin, Long id) {
        BotRobot bot = botRobotMapper.getBotRobotById(id);
        Asserts.notNull(bot, "参数异常");
        BotRoleAdminMapping adminMapping = botRoleAdminMappingMapper.getBotRoleAdminMappingByAdminIdAndRoleId(botAdmin.getId(), BotRoleConstant.adminRole);
        if (adminMapping == null) {
            Asserts.checkEquals(bot.getAdminId(), botAdmin.getId(), "权限异常");
        }
        int cnt = botRobotMapper.updateBotRobotSelective(new BotRobot().setId(id).setStatus(-1));
        Asserts.checkEquals(cnt, 1, "下线失败");
        if (Objects.equals(bot.getPushType(), "ws") && webSocketConfig != null) {
            webSocketConfig.downBot(id);
        }
    }

    public void addBot(BotAdmin botAdmin, BotRobot bot) {
        Asserts.notNull(bot, "参数异常");
        Asserts.notNull(bot.getType(), "参数异常");
        bot.setStatus(-1);
        bot.setAdminId(botAdmin.getId());
        bot.setMasterId(botAdmin.getUserId());
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
        bot.setPushType("ws");
        bot.setHost("api.sgroup.qq.com");

        BotRobot botInfo = botManager.getBotInfo(bot);
        Asserts.notNull(botInfo, "参数异常");
        bot.setName(botInfo.getName());
        bot.setTinyId(botInfo.getTinyId());

        BotUserDTO botUser = botUserManager.addOrUpdateBotUser(bot, new BotSender().setSendType(SendTypeEnum.KOOK_MESSAGE_STR), new BotUserDTO(BotUserConstant.USER_TYPE_KOOK, botInfo.getAuthorId()).setName(botInfo.getName()));
        bot.setUserId(botUser.getId());

        this.addBotRobot(bot);
    }

    private void handleKookBot(BotRobot bot) {
        Asserts.notNull(bot.getVerifyKey(), "请输入api秘钥");
        bot.setPushType("ws");
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
        bot.setPushType("hook");
        String host = bot.getHost();
        if (!host.startsWith("http")) {
            host = "http://"+host;
        }
        bot.setHost(host);
        this.addBotRobot(bot);
    }

    private void addBotRobot(BotRobot bot) {
        this.suppleHost(bot);
        botRobotMapper.addBotRobotSelective(bot);

        List<Integer> indexTypeList = botRobotIndexMapper.listIndexType();
        for (Integer indexType : indexTypeList) {
            botRobotIndexMapper.addBotRobotIndexSelective(new BotRobotIndex().setBotIndexType(indexType).setBot(bot.getId()).setIndex((int) (bot.getId() * 10)));
        }
    }

    public void deleteBot(BotAdmin botAdmin, Long id) {
        BotRobot bot = botRobotMapper.getBotRobotById(id);
        Asserts.notNull(bot, "参数异常");
        BotRoleAdminMapping adminMapping = botRoleAdminMappingMapper.getBotRoleAdminMappingByAdminIdAndRoleId(botAdmin.getId(), BotRoleConstant.adminRole);
        if (adminMapping == null) {
            Asserts.checkEquals(bot.getAdminId(), botAdmin.getId(), "权限异常");
        }
        botRobotMapper.deleteBotRobotByPrimary(id);
        List<BotRobotIndex> indexList = botRobotIndexMapper.getBotRobotIndexByCondition(new BotRobotIndexQuery().setBot(id));
        for (BotRobotIndex index : indexList) {
            botRobotIndexMapper.deleteBotRobotIndexByPrimary(index.getId());
        }
    }

    public void editBot(BotAdmin botAdmin, BotRobot bot) {
        BotRoleAdminMapping adminMapping = botRoleAdminMappingMapper.getBotRoleAdminMappingByAdminIdAndRoleId(botAdmin.getId(), BotRoleConstant.adminRole);
        if (adminMapping == null) {
            Asserts.checkEquals(bot.getAdminId(), botAdmin.getId(), "权限异常");
        }
        this.suppleHost(bot);

        BotRobot updBot = new BotRobot().setId(bot.getId());
        BotRobot dbBot = botRobotMapper.getBotRobotById(bot.getId());
        if (bot.getName() != null && !bot.getName().equals(dbBot.getName())) {
            updBot.setName(bot.getName());
        }
        if (bot.getHost() != null && !bot.getHost().equals(dbBot.getHost())) {
            updBot.setHost(bot.getHost());
        }
        if (bot.getVerifyKey() != null && !bot.getVerifyKey().equals(dbBot.getVerifyKey())) {
            updBot.setVerifyKey(bot.getVerifyKey());
        }
        if (bot.getQq() != null && !bot.getQq().equals(dbBot.getQq())) {
            updBot.setQq(bot.getQq());
        }
        int cnt = botRobotMapper.updateBotRobotSelective(updBot);
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

    public BotRobot getBot(BotAdmin botAdmin, Long botId) {
        BotRobot bot = botRobotMapper.getValidBotRobotById(botId);
        Asserts.notNull(bot, "参数异常");
        BotRoleAdminMapping adminMapping = botRoleAdminMappingMapper.getBotRoleAdminMappingByAdminIdAndRoleId(botAdmin.getId(), BotRoleConstant.adminRole);
        if (adminMapping == null) {
            Asserts.checkEquals(bot.getAdminId(), botAdmin.getId(), "权限异常");
        }
        return bot;
    }
}
