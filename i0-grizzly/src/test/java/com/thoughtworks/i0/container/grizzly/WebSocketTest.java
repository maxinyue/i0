package com.thoughtworks.i0.container.grizzly;

import com.thoughtworks.i0.container.grizzly.websockets.WebSocektApplication;
import com.thoughtworks.i0.core.Launcher;
import com.thoughtworks.i0.core.ServletContainer;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class WebSocketTest {
    private ServletContainer server;
    private WebSocketClient client;
    private TestSocket socket;

    @Before
    public void before() throws Exception {
        server = Launcher.launch(new WebSocektApplication(), false);
        client = new WebSocketClient();
        socket = new TestSocket();
        client.start();
    }

    @After
    public void after() throws Exception {
        server.stop();
        client.stop();
    }

    @Test
    public void should_launch_embedded_grizzly_with_websocket_support() throws Exception {
        client.connect(socket, new URI("ws://localhost:8051/websocket/echo1"));
        assertThat(socket.connected(), is(true));
        socket.send("message");
        assertThat(socket.received(), is("message"));
    }

    @Test
    public void should_launch_embedded_grizzly_with_injected_websocket() throws Exception {
        client.connect(socket, new URI("ws://localhost:8051/websocket/echo2"));
        assertThat(socket.connected(), is(true));
        socket.send("message");
        assertThat(socket.received(), is("injected message"));
    }

    @WebSocket
    public class TestSocket {
        private String received;
        //        private WebSocketClientConnection connection;
        public CountDownLatch open = new CountDownLatch(1);
        public CountDownLatch message = new CountDownLatch(1);
        private Session session;

        @OnWebSocketConnect
        public void onWebSocketConnect(Session session) {
            this.session = session;
            open.countDown();
        }

        public boolean connected() throws Exception {
            return open.await(500, TimeUnit.MILLISECONDS);
        }

        public String received() throws Exception {
            message.await(500, TimeUnit.MILLISECONDS);
            return received;
        }

        public void send(String message) throws IOException {
            session.getRemote().sendString(message);
        }

        @OnWebSocketMessage
        public void onMessage(String message) {
            this.received = message;
            this.message.countDown();
        }

    }
}
