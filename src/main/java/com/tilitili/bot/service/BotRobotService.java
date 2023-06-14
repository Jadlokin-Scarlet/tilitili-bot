package com.tilitili.bot.service;

import com.tilitili.bot.config.WebSocketConfig;
import com.tilitili.bot.entity.BotRobotDTO;
import com.tilitili.bot.socket.BotWebSocketHandler;
import com.tilitili.common.constant.BotRobotConstant;
import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotRobotIndex;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.query.BotRobotIndexQuery;
import com.tilitili.common.entity.query.BotRobotQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotRobotIndexMapper;
import com.tilitili.common.mapper.mysql.BotRobotMapper;
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

    public BotRobotService(BotRobotMapper botRobotMapper, @Nullable WebSocketConfig webSocketConfig, BotManager botManager, BotRobotIndexMapper botRobotIndexMapper) {
        this.botRobotMapper = botRobotMapper;
        this.webSocketConfig = webSocketConfig;
        this.botManager = botManager;
        this.botRobotIndexMapper = botRobotIndexMapper;
    }

    public BaseModel<PageModel<BotRobotDTO>> list(BotAdmin botAdmin, BotRobotQuery query) throws InvocationTargetException, IllegalAccessException {
        query.setAdminId(botAdmin.getId());
        int total = botRobotMapper.countBotRobotByCondition(query);
        List<BotRobot> list = botRobotMapper.getBotRobotByCondition(query.setAdminId(botAdmin.getId()));
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
        Asserts.checkEquals(bot.getAdminId(), botAdmin.getId(), "权限异常");
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
        Asserts.checkEquals(bot.getAdminId(), botAdmin.getId(), "权限异常");
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
                bot.setHost("www.kookapp.cn");

                BotRobot botInfo = botManager.getBotInfo(bot);
                bot.setName(botInfo.getName());
                if (botInfo.getQq() != null) {
                    bot.setQq(botInfo.getQq());
                }
                bot.setTinyId(botInfo.getTinyId());
                bot.setAuthorId(botInfo.getAuthorId());
                break;
            }
            case BotRobotConstant.TYPE_MINECRAFT: {
                handleMinecraftBot(bot);
                break;
            }
            case BotRobotConstant.TYPE_QQ_GUILD: {
                bot.setHost("api.sgroup.qq.com");

                Asserts.isTrue(Pattern.matches("\\d+\\.\\w+", bot.getVerifyKey()), "ggGuild的秘钥由appId点secret构成");
                BotRobot botInfo = botManager.getBotInfo(bot);
                bot.setName(botInfo.getName());
                if (botInfo.getQq() != null) {
                    bot.setQq(botInfo.getQq());
                }
                bot.setTinyId(botInfo.getTinyId());
                bot.setAuthorId(botInfo.getAuthorId());
                break;
            }
            default: throw new AssertException("参数异常");
        }

    }

    private void handleMinecraftBot(BotRobot bot) {
        Asserts.notBlank(bot.getName(), "请输入昵称");
        Asserts.notNull(bot.getHost(), "请输入服务器地址");
        Asserts.notNull(bot.getVerifyKey(), "请输入api秘钥");
        bot.setPushType("hook");
        botRobotMapper.addBotRobotSelective(bot);

        List<Integer> indexTypeList = botRobotIndexMapper.listIndexType();
        for (Integer indexType : indexTypeList) {
            botRobotIndexMapper.addBotRobotIndexSelective(new BotRobotIndex().setBotIndexType(indexType).setBot(bot.getId()).setIndex((int) (bot.getId() * 10)));
        }
    }

    public void deleteBot(BotAdmin botAdmin, Long id) {
        BotRobot bot = botRobotMapper.getBotRobotById(id);
        Asserts.notNull(bot, "参数异常");
        Asserts.checkEquals(bot.getAdminId(), botAdmin.getId(), "权限异常");
        botRobotMapper.deleteBotRobotByPrimary(id);
        List<BotRobotIndex> indexList = botRobotIndexMapper.getBotRobotIndexByCondition(new BotRobotIndexQuery().setBot(id));
        for (BotRobotIndex index : indexList) {
            botRobotIndexMapper.deleteBotRobotIndexByPrimary(index.getId());
        }
    }
}
