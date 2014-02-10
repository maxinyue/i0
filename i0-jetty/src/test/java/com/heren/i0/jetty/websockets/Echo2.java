package com.heren.i0.jetty.websockets;

import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;

import javax.inject.Inject;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/echo2")
public class Echo2 {
    @Inject
    Service service;

    @OnWebSocketMessage
    public String echo(String message) {
        return service.message() + " " + message;
    }

}