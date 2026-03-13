package com.redis.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.redis.commands.CommandDispatcher;
import com.redis.protocol.RespEncoder;
import com.redis.protocol.RespParser;
import com.redis.protocol.RespValue;

public class ClientHandler {
    private final RespParser parser = new RespParser();
    private final RespEncoder encoder = new RespEncoder();

    public void handleClient(Socket client, CommandDispatcher dispatcher) {
        try (client) {
            InputStream in = new BufferedInputStream(client.getInputStream());
            OutputStream out = client.getOutputStream();

            while (true) {
                RespValue command = parser.parse(in);
                if (command == null) break;
                RespValue response = dispatcher.dispatch(command);
                out.write(encoder.encodeToBytes(response));
                out.flush();
            }

            System.out.println("Client disconnected: " + client.getInetAddress());
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
        
    }
}
