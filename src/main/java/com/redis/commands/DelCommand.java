package com.redis.commands;

import java.util.List;

import com.redis.core.RedisStore;
import com.redis.protocol.RespValue;

public class DelCommand implements CommandHandler{

    @Override
    public RespValue execute(List<String> args, RedisStore store) {
        if (args.isEmpty()) {
            return RespValue.error("ERR wrong number of arguments for 'del' command");
        }
        int count = store.del(args.toArray(new String[0]));
        return RespValue.integer(count);
    }
}
