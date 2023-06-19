package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.mirai.UploadImageResult;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.GomokuImageManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
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
        if (Pattern.matches("[A-O][0-9]{1,2}", text)) {
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
            default: throw new AssertException();
        }
    }

    private BotMessage handleFall(BotMessageAction messageAction) {
        BotSessionService.MiraiSession session = messageAction.getSession();
        BotRobot bot = messageAction.getBot();
        String value = messageAction.getValueOrVirtualValue();
        BotUserDTO botUser = messageAction.getBotUser();
        int[][] board = Gsons.fromJson(session.get("GomokuHandle.board"), int[][].class);
        Integer flag = session.getInteger("GomokuHandle.flag-" + botUser.getId());

        List<String> indexStrList = StringUtils.extractList("([A-O])([0-9]{1,2})", value);
        Asserts.checkEquals(indexStrList.size(), 2, "坐标不对啦");
        // 第一个坐标
        String index1Str = indexStrList.get(0);
        Asserts.notBlank(index1Str, "坐标不对啦");
        int index1 = 'O' - index1Str.charAt(0);
        Asserts.isRange(0, index1,15 , "坐标不对啦");
        // 第二个坐标
        String index2Str = indexStrList.get(1);
        Asserts.isNumber(index2Str, "坐标不对啦");
        int index2 = Integer.parseInt(index2Str);
        Asserts.isRange(0, index2,15 , "坐标不对啦");

        Asserts.checkEquals(board[index1][index2], 0, "这里已经落子啦");
        board[index1][index2] = flag;

        UploadImageResult result = gomokuImageManager.getGomokuImage(bot, board);
        return BotMessage.simpleImageMessage(result);
    }

    private BotMessage handleStart(BotMessageAction messageAction) {
        BotSessionService.MiraiSession session = messageAction.getSession();
        BotUserDTO botUser = messageAction.getBotUser();
        List<BotUserDTO> atList = messageAction.getAtList();
        Asserts.checkEquals(atList.size(), 1, "和谁？");
        BotUserDTO otherUser = atList.get(0);
        Asserts.isFalse(session.containsKey("GomokuHandle.mapping-"+botUser.getId()), "你已经在下啦");
        Asserts.isFalse(session.containsKey("GomokuHandle.mapping-"+otherUser.getId()), "他还在下啦");

        int[][] board = new int[15][15];
        session.put("GomokuHandle.mapping-"+botUser.getId(), otherUser.getId());
        session.put("GomokuHandle.mapping-"+otherUser.getId(), botUser.getId());
        session.put("GomokuHandle.board", Gsons.toJson(board));
        session.put("GomokuHandle.flag-"+botUser.getId(), 1);
        session.put("GomokuHandle.flag-"+otherUser.getId(), -1);

        return BotMessage.simpleTextMessage(String.format("%s执黑，请落子", otherUser.getName()));
    }
}
