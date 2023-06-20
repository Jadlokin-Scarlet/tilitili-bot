package com.tilitili.bot.entity;

import com.tilitili.common.entity.dto.BotUserDTO;

public class Gomoku {
    private BotUserDTO playerA;
    private BotUserDTO playerB;
    private int[][] board;
    private int flag;

    public BotUserDTO getNowPlayer() {
        return flag == 1? playerA: playerB;
    }

    public BotUserDTO getLastPlayer() {
        return flag == -1? playerA: playerB;
    }

    public int getBoardCell(int index1, int index2) {
        return board[index2][index1];
    }

    public void setBoardCell(int index1, int index2, int cell) {
        board[index2][index1] = cell;
    }







    public BotUserDTO getPlayerA() {
        return playerA;
    }

    public Gomoku setPlayerA(BotUserDTO playerA) {
        this.playerA = playerA;
        return this;
    }

    public BotUserDTO getPlayerB() {
        return playerB;
    }

    public Gomoku setPlayerB(BotUserDTO playerB) {
        this.playerB = playerB;
        return this;
    }

    public int[][] getBoard() {
        return board;
    }

    public Gomoku setBoard(int[][] board) {
        this.board = board;
        return this;
    }

    public int getFlag() {
        return flag;
    }

    public Gomoku setFlag(int flag) {
        this.flag = flag;
        return this;
    }
}
