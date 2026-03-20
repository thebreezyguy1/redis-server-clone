package com.redis.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.redis.core.RedisStore;
import com.redis.persistence.RdbSnapshot;
import com.redis.protocol.RespValue;

public class CommandDispatcher {
    private final RedisStore store;
    private final Map<String, CommandHandler> commands = new HashMap<>();
    private final RdbSnapshot rdb;

    public CommandDispatcher(RedisStore store, RdbSnapshot rdb) {
        this.store = store;
        this.rdb = rdb;
        commands.put("CONFIG", (args, s) -> RespValue.array(new ArrayList<>()));
        commands.put("COMMAND", new Command());
        commands.put("PING", new PingCommand());
        commands.put("SET", new SetCommand());
        commands.put("GET", new GetCommand());
        commands.put("DEL", new DelCommand());
        commands.put("EXPIRE", new ExpireCommand());
        commands.put("EXISTS", new ExistsCommand());
        commands.put("TTL", new TtlCommand());
        commands.put("KEYS", new KeysCommand());
        commands.put("TYPE", new TypeCommand());
        commands.put("ECHO", new EchoCommand());
        commands.put("FLUSHALL", new FlushAllCommand());
        commands.put("DBSIZE", new DbSizeCommand());
    }

    private static final Set<String> WRITE_COMMANDS = Set.of(
        "SET", "DEL", "EXPIRE", "FLUSHALL"
    );

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

        RespValue result = handler.execute(args, store);

        if (WRITE_COMMANDS.contains(name) && rdb != null) {
            rdb.incrementWriteCount();
        }

        return result;
    }
}
