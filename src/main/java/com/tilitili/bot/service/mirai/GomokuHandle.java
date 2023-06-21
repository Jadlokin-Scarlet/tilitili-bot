package com.tilitili.bot.service.mirai;

import com.google.common.collect.Lists;
import com.tilitili.bot.entity.Gomoku;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.GomokuImageManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Component
public class GomokuHandle extends ExceptionRespMessageHandle {
    private final GomokuImageManager gomokuImageManager;

    public GomokuHandle(GomokuImageManager gomokuImageManager) {
        this.gomokuImageManager = gomokuImageManager;
    }

    @Override
    public String isThisTask(BotMessageAction messageAction) {
        String text = messageAction.getText();
        if (Pattern.matches("[A-Oa-o][0-9]{1,2}", text)) {
            return "落子";
        } else {
            return null;
        }
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
        switch (messageAction.getVirtualKeyOrDefault(messageAction.getKeyWithoutPrefix())) {
            case "五子棋": return this.handleStart(messageAction);
            case "落子": return this.handleFall(messageAction);
            case "掀桌": return this.handleStop(messageAction);
            default: throw new AssertException();
        }
    }

    private BotMessage handleStop(BotMessageAction messageAction) {
        BotSessionService.MiraiSession session = messageAction.getSession();
        BotUserDTO botUser = messageAction.getBotUser();
        BotRobot bot = messageAction.getBot();
        if (!session.containsKey("GomokuHandle.gomoku")) {
            return null;
        }
        if (!Objects.equals(botUser.getId(), bot.getMasterId())) {
            return null;
        }
        session.remove("GomokuHandle.gomoku");
        return BotMessage.simpleTextMessage("(╯‵□′)╯︵┻━┻");
    }

    private BotMessage handleFall(BotMessageAction messageAction) {
        BotSessionService.MiraiSession session = messageAction.getSession();
        BotRobot bot = messageAction.getBot();
        String value = messageAction.getValueOrVirtualValue();
        BotUserDTO botUser = messageAction.getBotUser();
        Gomoku gomoku = Gsons.fromJson(session.get("GomokuHandle.gomoku"), Gomoku.class);
        BotUserDTO nowPlayer = gomoku.getNowPlayer();
        Asserts.checkEquals(nowPlayer.getId(), botUser.getId(), "还没轮到你");
        BotUserDTO lastPlayer = gomoku.getLastPlayer();

        List<String> indexStrList = StringUtils.extractList("([A-Oa-o])([0-9]{1,2})", value);
        Asserts.checkEquals(indexStrList.size(), 2, "坐标不对啦");
        // 第一个坐标
        String index1Str = indexStrList.get(0);
        Asserts.notBlank(index1Str, "坐标不对啦");
        char c = index1Str.charAt(0);
        int index1;
        if (c >= 'A') {
            index1 = c - 'A';
        } else {
            index1 = c - 'a';
        }
        Asserts.isRange(0, index1,15 , "坐标不对啦");
        // 第二个坐标
        String index2Str = indexStrList.get(1);
        Asserts.isNumber(index2Str, "坐标不对啦");
        int index2 = Integer.parseInt(index2Str) - 1;
        Asserts.isRange(0, index2,15 , "坐标不对啦");

        int flag = gomoku.getFlag();
        int[][] board = gomoku.getBoard();
        Asserts.checkEquals(gomoku.getBoardCell(index1, index2), 0, "这里已经落子啦");
        gomoku.setBoardCell(index1, index2, flag);

        Boolean end = this.checkEnd(gomoku);

        gomoku.setFlag(-flag);
        session.put("GomokuHandle.gomoku", Gsons.toJson(gomoku));
        return BotMessage.simpleListMessage(Lists.newArrayList(
                BotMessageChain.ofPlain(String.format("%s请落子", lastPlayer.getName())),
                BotMessageChain.ofImage(gomokuImageManager.getGomokuImage(bot, board))
        ));
    }

    private final int length = 15;
    private Boolean checkEnd(Gomoku gomoku) {
        int flag = gomoku.getFlag();
        int[][] board = gomoku.getBoard();
        boolean[][] over = new boolean[length][length];
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                Queue<Point> queue = new LinkedList<>();
                queue.add(new Point(i, j));
                while (!queue.isEmpty()) {
                    Point now = queue.poll();
                    over[i][j] = true;

                }
            }
        }
        return false;
    }

    private BotMessage handleStart(BotMessageAction messageAction) {
        BotSessionService.MiraiSession session = messageAction.getSession();
        BotRobot bot = messageAction.getBot();
        BotUserDTO botUser = messageAction.getBotUser();
        List<BotUserDTO> atList = messageAction.getAtList();
        Asserts.checkEquals(atList.size(), 1, "和谁？");
        BotUserDTO otherUser = atList.get(0);
        Asserts.isFalse(session.containsKey("GomokuHandle.gomoku"), "已经有人在下啦");

        int[][] board = new int[15][15];
        int flag = ThreadLocalRandom.current().nextInt(2) * 2 - 1;
        Gomoku gomoku = new Gomoku().setBoard(board).setFlag(flag).setPlayerA(botUser).setPlayerB(otherUser);
        session.put("GomokuHandle.gomoku", Gsons.toJson(gomoku));

        BotUserDTO nowPlayer = gomoku.getNowPlayer();
        return BotMessage.simpleListMessage(Lists.newArrayList(
                BotMessageChain.ofPlain(String.format("%s执黑，请落子", nowPlayer.getName())),
                BotMessageChain.ofImage(gomokuImageManager.getGomokuImage(bot, board))
        ));
    }
}
