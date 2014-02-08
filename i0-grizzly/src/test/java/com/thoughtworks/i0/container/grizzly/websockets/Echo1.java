package com.thoughtworks.i0.container.grizzly.websockets;

import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import java.io.IOException;

@ServerEndpoint("/echo1")
public class Echo1 {

    @OnMessage
    public void echo(String message, Session session) throws IOException {
        session.getBasicRemote().sendText(message);
    }

}