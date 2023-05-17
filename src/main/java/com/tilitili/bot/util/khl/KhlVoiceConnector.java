package com.tilitili.bot.util.khl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.dto.PlayerMusic;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a connector with a voice channel. <p>
 * You can use this instance again and again.
 */
@Slf4j
public class KhlVoiceConnector {
    private WebSocket webSocket;

    private static final ScheduledExecutorService scheduled =  Executors.newScheduledThreadPool(3);

    private final Queue<PlayerMusic> playerQueue = new LinkedList<>();
    private Process musicProcess;
    private Process playerProcess;
    private Long playerChannelId;
    private PlayerMusic thePlayerMusic;
    private ScheduledFuture<?> musicFuture;
    private boolean stop;
    private final BotRobot bot;

    public KhlVoiceConnector(BotRobot bot) {
        this.bot = bot;
    }


    public List<PlayerMusic> pushFileToQueue(String token, Long channelId, PlayerMusic playerMusic) {
        try {
            this.checkPlayerProcess(token, channelId);
        } catch (Exception e) {
            log.warn("播放器启动失败", e);
            throw new AssertException("播放器启动失败");
        }

        log.info("添加播放列表{}", playerMusic.getName());
        this.playerQueue.add(playerMusic);
        return this.getPlayerMusicList();
    }

    public List<PlayerMusic> lastMusic() {
        List<PlayerMusic> playerMusicList = this.getPlayerMusicList();
        thePlayerMusic.setRollPlayer(false);
        musicProcess.destroy();
        if (playerMusicList.size() < 2) {
            return Collections.emptyList();
        }
        return playerMusicList.subList(1, playerMusicList.size());
    }

    public List<PlayerMusic> stopMusic() {
        this.stop = true;
        List<PlayerMusic> playerMusicList = this.getPlayerMusicList();
        thePlayerMusic.setRollPlayer(false);
        musicProcess.destroy();
        if (playerMusicList.size() < 2) {
            this.stop = false;
            return Collections.emptyList();
        }
        return playerMusicList.subList(1, playerMusicList.size());
    }

    public List<PlayerMusic> startMusic() {
        List<PlayerMusic> playerMusicList = this.getPlayerMusicList();
        this.stop = false;
        return playerMusicList;
    }

    public List<PlayerMusic> listMusic() {
        List<PlayerMusic> playerMusicList = new ArrayList<>(playerQueue);
        playerMusicList.add(0, thePlayerMusic);
        return playerMusicList.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private List<PlayerMusic> getPlayerMusicList() {
        PlayerMusic lastPlayerMusic = playerQueue.peek();
        PlayerMusic thePlayerMusic = this.thePlayerMusic;
        return Stream.of(thePlayerMusic, lastPlayerMusic, playerQueue.peek()).filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    public Boolean loopPlayer() {
        if (thePlayerMusic == null) {
            return false;
        }
        thePlayerMusic.setRollPlayer(true);
        return true;
    }

    private void checkPlayerProcess(String token, Long channelId) throws ExecutionException, InterruptedException, IOException {
        if(Objects.equals(this.playerChannelId, channelId)) {
            log.info("无需切换播放器");
            return;
        }

        Asserts.checkEmpty(this.playerQueue, "还在忙啦，别急。");

        if (playerChannelId != null) {
            log.info("重启播放器");
            this.disconnect();
        }
        if (playerProcess != null) {
            playerProcess.destroy();
        }
        if (musicProcess != null) {
            musicProcess.destroy();
        }
        if (musicFuture != null) {
            musicFuture.cancel(true);
        }

        this.playerChannelId = channelId;
        this.stop = false;
        String rtmpUrl = this.connect(token, channelId, () -> {
            log.info("播放器关闭");
            playerChannelId = null;
            return null;
        }).get();

        int port = (int) (5000 + bot.getId());
        String playerCommand = String.format("ffmpeg -re -loglevel level+info -nostats -stream_loop -1 -i zmq:tcp://127.0.0.1:%s -map 0:a:0 -acodec libopus -ab 128k -filter:a volume=0.2 -ac 2 -ar 48000 -f tee [select=a:f=rtp:ssrc=1357:payload_type=100]%s", port, rtmpUrl);
        log.info("ffmpeg开启推流命令：" + playerCommand);
        playerProcess = Runtime.getRuntime().exec(playerCommand);

        musicFuture = scheduled.scheduleAtFixedRate(() -> {
            if (stop) {
                log.info("bot{}暂停", bot.getId());
                return;
            }
            if (thePlayerMusic == null || !thePlayerMusic.isRollPlayer()) {
                thePlayerMusic = playerQueue.poll();
            }
            if (thePlayerMusic == null) {
                log.info("bot{}没有下一首", bot.getId());
                return;
            }
            log.info("bot{}播放{}", bot.getId(), thePlayerMusic.getName());
            try {
                String command = String.format("ffmpeg -re -nostats -i %s -acodec libopus -vn -ab 128k -f mpegts zmq:tcp://127.0.0.1:%s", thePlayerMusic.getFile().getPath(), port);
                log.info("ffmpeg推流命令：" + command);

                // 运行cmd命令，获取其进程
                musicProcess = Runtime.getRuntime().exec(command);
                // 输出ffmpeg推流日志
                BufferedReader br = new BufferedReader(new InputStreamReader(musicProcess.getErrorStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    log.info("视频推流信息[" + line + "]");
                }
                log.info("视频推流结果" + musicProcess.waitFor());
            } catch (Exception e) {
                log.warn("播放列表播放异常", e);
            } finally {
                if (!thePlayerMusic.isRollPlayer()) {
                    FileUtil.deleteIfExists(thePlayerMusic.getFile());
                }
                log.info("结束播放{}", thePlayerMusic.getName());
            }
        }, 1, 1, TimeUnit.SECONDS);
    }














    /**
     * Call this to create connection with the channel that specified by this instance.
     *
     * @param onDead Just a callback, called when the connection is dead for some reason (e.g. another connection created)
     * @return The Future representation, it contains the RTP link for you to pushing stream using ffmpeg or other program.
     * @throws IllegalStateException Thrown if something unexpected happened, is the Bot not in your guild?
     */
    private Future<String> connect(String token, Long channelId, Callable<Void> onDead) throws IllegalStateException {
        Asserts.notNull(token, "啊嘞，不对劲");

        disconnect(); // make sure the connection actually dead, or something unexpected will happen?
        webSocket = null; // help GC

        OkHttpClient client = new OkHttpClient.Builder().pingInterval(30, TimeUnit.SECONDS).build();

        // region Get Gateway
        String gatewayWs;
        String fullGatewayUrl = "https://www.kookapp.cn/api/v3/gateway/voice?channel_id=" + channelId;
        try (Response response = client.newCall(
                new Request.Builder()
                        .get()
                        .url(fullGatewayUrl)
                        .addHeader("Authorization", String.format("Bot %s", token))
                        .build()
        ).execute()) {
            if (response.code() != 200) {
                throw new IllegalStateException();
            }
            assert response.body() != null;
            JsonObject element = JsonParser.parseString(response.body().string()).getAsJsonObject();
            if (element.get("code").getAsInt() != 0) {
                throw new IllegalStateException();
            }
            gatewayWs = element.getAsJsonObject("data").get("gateway_url").getAsString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        CompletableFuture<String> future = new CompletableFuture<>();

        // endregion
        webSocket = client.newWebSocket(
                new Request.Builder()
                        .url(gatewayWs)
                        .build(),
                new SimpleWebSocketListener(future, onDead)
        );

        webSocket.send(randomId(Constants.STAGE_1));
        webSocket.send(randomId(Constants.STAGE_2));
        webSocket.send(randomId(Constants.STAGE_3));
        return future;
    }

    /**
     * Disconnect with the specified channel.
     */
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "User Closed Service");
        }
    }

    private static String randomId(String constant) {
        JsonObject object = JsonParser.parseString(constant).getAsJsonObject();
        object.remove("id");
        object.addProperty("id", new SecureRandom().nextInt(8999999) + 1000000);
        return new Gson().toJson(object);
    }
}
