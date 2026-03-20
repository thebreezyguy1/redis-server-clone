package com.redis.commands;

import java.util.List;

import com.redis.core.RedisStore;
import com.redis.protocol.RespValue;

public class TypeCommand implements CommandHandler{
    @Override
    public RespValue execute(List<String> args, RedisStore store) {
        if (args.isEmpty()) return RespValue.error("ERR wrong number of argument for 'type' command");
        return RespValue.simpleString(store.type(args.get(0)).name().toLowerCase());
    }
}
