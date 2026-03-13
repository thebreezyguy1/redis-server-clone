package com.redis.commands;

import java.util.List;

import com.redis.core.RedisStore;
import com.redis.protocol.RespValue;

public class PingCommand implements CommandHandler {
    @Override
    public RespValue execute(List<String> args, RedisStore store) {
        if (args.isEmpty()) return RespValue.simpleString("PONG");
        return RespValue.simpleString(args.get(0));
    }
}
