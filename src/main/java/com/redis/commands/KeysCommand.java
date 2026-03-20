package com.redis.commands;

import java.util.ArrayList;
import java.util.List;

import com.redis.core.RedisStore;
import com.redis.protocol.RespValue;

public class KeysCommand implements CommandHandler{
    @Override
    public RespValue execute(List<String> args, RedisStore store) {
        if (args.isEmpty()) return RespValue.error("ERR wrong number of argument for 'keys' command");
        
        List<RespValue> result = new ArrayList<>();
        for (String key : store.keys(args.get(0))) {
            result.add(RespValue.bulkString(key));
        }

        return RespValue.array(result);
    }
}
