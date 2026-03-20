package com.redis.commands;

import java.util.List;

import com.redis.core.RedisStore;
import com.redis.protocol.RespValue;

public class ExpireCommand implements CommandHandler {
    @Override
    public RespValue execute(List<String> args, RedisStore store) {
        if (args.size() < 2) {
            return RespValue.error("ERR wrong number of arguments for 'expire' command");
        }
        try {
            long seconds = Long.parseLong(args.get(1));
            boolean result = store.expire(args.get(0), seconds);
            return RespValue.integer(result ? 1 : 0);
        } catch (NumberFormatException e) {
            return RespValue.error("ERR value is not an integer or out of range");
        }
    }
}