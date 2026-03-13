package com.redis.commands;

import java.util.List;

import com.redis.core.RedisStore;
import com.redis.protocol.RespValue;

public interface CommandHandler {
    RespValue execute(List<String> args, RedisStore store);
}
