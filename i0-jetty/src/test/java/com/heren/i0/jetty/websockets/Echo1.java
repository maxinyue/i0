package com.heren.i0.jetty.websockets;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;


import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@ServerEndpoint("/echo1")
public class Echo1 {

    @OnWebSocketMessage
    public void echo(String message, Session session) throws IOException {
        session.getRemote().sendString(message);
    }

}