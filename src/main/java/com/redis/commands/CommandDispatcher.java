package com.redis.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.redis.core.RedisStore;
import com.redis.protocol.RespValue;

public class CommandDispatcher {
    private final RedisStore store;
    private final Map<String, CommandHandler> commands = new HashMap<>();

    public CommandDispatcher(RedisStore store) {
        this.store = store;
        commands.put("PING", new PingCommand());
        commands.put("SET", new SetCommand());
        commands.put("GET", new GetCommand());
    }

    public RespValue dispatch(RespValue command) {
        List<RespValue> parts = command.getArrayValue();
        String name = parts.get(0).getStrValue().toUpperCase();
        List<String> args = parts.stream()
                                 .skip(1)
                                 .map(RespValue::getStrValue)
                                 .collect(Collectors.toList());
        
        CommandHandler handler = commands.get(name);
        if (handler == null) {
            return RespValue.error("ERR unknow command '" + name + "'");
        }
        return handler.execute(args, store);
    }
}
