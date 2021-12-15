package com.tilitili.bot;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.tilitili.common.entity.gocqhttp.GoCqhttpWsMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StartApplicationTest {

    @Test
    void main() {
        String json = "[{\"interval\":5000,\"meta_event_type\":\"heartbeat\",\"post_type\":\"meta_event\",\"self_id\":536657454,\"status\":{\"app_enabled\":true,\"app_good\":true,\"app_initialized\":true,\"good\":true,\"online\":true,\"plugins_good\":null,\"stat\":{\"packet_received\":1154,\"packet_sent\":226,\"packet_lost\":0,\"message_received\":237,\"message_sent\":0,\"disconnect_times\":0,\"lost_times\":0,\"last_message_time\":1639540194}},\"time\":1639540208}\n" +
                "]";

        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        List<GoCqhttpWsMessage> messageList = gson.fromJson(json, new TypeToken<List<GoCqhttpWsMessage>>(){}.getType());
        System.out.println("?");
    }
}