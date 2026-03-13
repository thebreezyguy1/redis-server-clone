package com.redis.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.redis.commands.CommandDispatcher;

public class RedisServer {
    private final int port;
    private final CommandDispatcher dispatcher;
    private ClientHandler handler;

    public RedisServer(CommandDispatcher dispatcher) {
        this.port = 6379;
        this.dispatcher = dispatcher;
        this.handler = new ClientHandler();
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(port));
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("Client connected: " + client.getInetAddress());
                handler.handleClient(client, dispatcher);
            }
        }
    }
}
