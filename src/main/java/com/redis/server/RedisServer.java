package com.redis.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.redis.commands.CommandDispatcher;

public class RedisServer {
    private final int port;
    private final CommandDispatcher dispatcher;
    private final ThreadPoolExecutor threadPool;

    public RedisServer(CommandDispatcher dispatcher) {
        this.port = 6379;
        this.dispatcher = dispatcher;
        this.threadPool = new ThreadPoolExecutor(
            0,                        // corePoolSize: no idle threads kept alive
            Integer.MAX_VALUE,        // maximumPoolSize: grow unboundedly under load
            60L, TimeUnit.SECONDS,    // keepAliveTime: idle threads die after 60s
            new SynchronousQueue<>()  // no task queue — hand off directly to a thread
        );
    }

    public void start() throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            threadPool.shutdown();
            try {
                threadPool.awaitTermination(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }));
        
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(port));
            while (true) {
                Socket client = serverSocket.accept();
                // System.out.println("Client connected: " + client.getInetAddress());
                threadPool.submit(() -> new ClientHandler().handleClient(client, dispatcher));
            }
        }
    }
}
