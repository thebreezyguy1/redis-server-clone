package com.redis.commands;

import java.util.List;

import com.redis.core.RedisStore;
import com.redis.protocol.RespValue;

public class FlushAllCommand implements CommandHandler {
    @Override
    public RespValue execute(List<String> args, RedisStore store) {
        store.flushAll();
        return RespValue.simpleString("OK");
    }
}
