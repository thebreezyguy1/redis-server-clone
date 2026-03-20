package com.redis.commands;

import java.util.List;
import java.util.ArrayList;

import com.redis.core.RedisStore;
import com.redis.protocol.RespValue;

public class Command implements CommandHandler {
    @Override
    public RespValue execute(List<String> args, RedisStore store) {
        // COMMAND COUNT
        if (!args.isEmpty() && args.get(0).equalsIgnoreCase("COUNT")) {
            return RespValue.integer(getCommandInfo().size());
        }

        // COMMAND (no args) - return info for all commands
        List<RespValue> result = new ArrayList<>();
        for (CommandInfo info : getCommandInfo()) {
            result.add(info.toRespValue());
        }
        return RespValue.array(result);
    }

    private record CommandInfo(String name, int arity, String[] flags, int firstKey, int lastKey, int step) {
        RespValue toRespValue() {
            List<RespValue> entry = new ArrayList<>();
            entry.add(RespValue.bulkString(name));
            entry.add(RespValue.integer(arity));
            List<RespValue> flagArray = new ArrayList<>();
            for (String flag : flags) {
                flagArray.add(RespValue.simpleString(flag));
            }
            entry.add(RespValue.array(flagArray));
            entry.add(RespValue.integer(firstKey));
            entry.add(RespValue.integer(lastKey));
            entry.add(RespValue.integer(step));
            return RespValue.array(entry);
        }
    }

    private List<CommandInfo> getCommandInfo() {
        return List.of(
            new CommandInfo("get",      2,  new String[]{"readonly", "fast"},   1, 1, 1),
            new CommandInfo("set",     -3,  new String[]{"write", "denyoom"},   1, 1, 1),
            new CommandInfo("del",     -2,  new String[]{"write"},             1, -1, 1),
            new CommandInfo("exists",  -2,  new String[]{"readonly", "fast"},  1, -1, 1),
            new CommandInfo("ping",    -1,  new String[]{"fast"},              0, 0, 0),
            new CommandInfo("echo",     2,  new String[]{"fast"},              0, 0, 0),
            new CommandInfo("ttl",      2,  new String[]{"readonly", "fast"},  1, 1, 1),
            new CommandInfo("keys",     2,  new String[]{"readonly"},          0, 0, 0),
            new CommandInfo("type",     2,  new String[]{"readonly", "fast"},  1, 1, 1),
            new CommandInfo("flushall",-1,  new String[]{"write"},             0, 0, 0),
            new CommandInfo("dbsize",   1,  new String[]{"readonly", "fast"},  0, 0, 0),
            new CommandInfo("command",  -1, new String[]{"loading"},           0, 0, 0)
        );
    }
}