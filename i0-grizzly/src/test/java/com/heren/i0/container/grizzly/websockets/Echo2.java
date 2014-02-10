package com.heren.i0.container.grizzly.websockets;

import javax.inject.Inject;
import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;


@ServerEndpoint("/echo2")
public class Echo2 {
    @Inject
    Service service;

    @OnMessage
    public String echo(String message) {
        return service.message() + " " + message;
    }

}