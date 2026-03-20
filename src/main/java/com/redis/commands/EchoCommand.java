package com.redis.commands;

import java.util.List;

import com.redis.core.RedisStore;
import com.redis.protocol.RespValue;

public class EchoCommand implements CommandHandler{
    @Override
    public RespValue execute(List<String> args, RedisStore store) {
        if (args.isEmpty()) return RespValue.error("ERR wrong number of arguments for 'echo' command");
        return RespValue.bulkString(args.get(0));
    }
}
