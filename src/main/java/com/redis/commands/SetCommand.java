package com.redis.commands;

import java.util.List;

import com.redis.core.RedisStore;
import com.redis.protocol.RespValue;

public class SetCommand implements CommandHandler {
    @Override
    public RespValue execute(List<String> args, RedisStore store) {
        if (args.size() < 2) {
            return RespValue.error("ERR wrong number of arguments for 'set' command");
        }

        String key = args.get(0);
        String value = args.get(1);

        if (args.size() == 4) {
            String option = args.get(2).toUpperCase();
            long ttl = Long.parseLong(args.get(3));

            if (option.equals("EX")) {
                store.set(key, value, ttl * 1000);
                return RespValue.simpleString("OK");
            } else if (option.equals("PX")) {
                store.set(key, value, ttl);
                return RespValue.simpleString("OK");
            } else {
                return RespValue.error("ERR invalid option '" + option + "'");
            }
        }

        store.set(key, value);
        return RespValue.simpleString("OK");
    }
}
