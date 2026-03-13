package com.redis.commands;

import java.util.List;

import com.redis.core.RedisStore;
import com.redis.protocol.RespValue;

public class GetCommand implements CommandHandler {
    @Override
    public RespValue execute(List<String> args, RedisStore store) {
        if (args.size() != 1) {
            return RespValue.error("ERR wrong number of arguments for 'get' command");
        }

        String value = store.get(args.get(0));

        if (value == null) return RespValue.nullValue();
        return RespValue.bulkString(value);
    }
}
