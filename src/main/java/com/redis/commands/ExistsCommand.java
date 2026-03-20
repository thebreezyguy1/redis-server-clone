package com.redis.commands;

import java.util.List;

import com.redis.core.RedisStore;
import com.redis.protocol.RespValue;

public class ExistsCommand implements CommandHandler{
    @Override
    public RespValue execute(List<String> args, RedisStore store) {
        if (args.isEmpty()) {
            return RespValue.error("ERR wrong number of arguments for 'exists' command");
        }

        int count = 0;

        for (String string : args) {
            if (store.exists(string)) {
                count++;
            }
        }

        return RespValue.integer(count);
    }
}
