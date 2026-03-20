package com.redis.commands;

import java.util.List;

import com.redis.core.RedisStore;
import com.redis.protocol.RespValue;

public class TtlCommand implements CommandHandler{
    @Override
    public RespValue execute(List<String> args, RedisStore store) {
        if (args.isEmpty()) return RespValue.error("ERR wrong number of arguments for 'ttl' command");
        return RespValue.integer(store.ttl(args.get(0)));
    }
}
