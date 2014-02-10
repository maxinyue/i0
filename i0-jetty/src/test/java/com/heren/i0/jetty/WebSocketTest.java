package com.heren.i0.jetty;

import com.heren.i0.core.Launcher;
import com.heren.i0.core.ServletContainer;
import com.heren.i0.jetty.websockets.WebSocektApplication;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WebSocketTest {
    private ServletContainer server;

    @Before
    public void before() throws Exception {
        server = Launcher.launch(new WebSocektApplication(), false);
    }

    @After
    public void after() throws Exception {
        server.stop();
    }

    @Test
    public void should_launch_embedded_jetty_with_websocket_support() throws Exception {
//        TestSocket socket = new TestSocket();
//        WebSocketClient client = new WebSocketClient();
//
//                factory.newWebSocketClient(socket);
//        client.connect(new URI("ws://localhost:8051/websocket/echo1"));
//
//        assertThat(socket.connected(), is(true));
//
//        socket.send("message");
//
//        assertThat(socket.received(), is("message"));
    }

    @Test
    public void should_launch_embedded_jetty_with_injected_websocket() throws Exception {
//        TestSocket socket = new TestSocket();
//        WebSocketClient client = factory.newWebSocketClient(socket);
//        client.connect(new URI("ws://localhost:8051/websocket/echo2"));
//
//        assertThat(socket.connected(), is(true));
//
//        socket.send("message");
//
//        assertThat(socket.received(), is("injected message"));
    }

    class TestSocket extends WebSocketAdapter {
//        private String received;
//        private WebSocketConnection connection;
//        public CountDownLatch open = new CountDownLatch(1);
//        public CountDownLatch message = new CountDownLatch(1);

//        @Override
//        public void onWebSocketConnect(WebSocketConnection connection) {
//            this.connection = connection;
//            open.countDown();
//        }
//
//        public boolean connected() throws Exception {
//            return open.await(500, TimeUnit.MILLISECONDS);
//        }
//
//        public String received() throws Exception {
//            message.await(500, TimeUnit.MILLISECONDS);
//            return received;
//        }
//
//        public void send(String message) throws IOException {
//            connection.write(message);
//        }
//
//        @Override
//        public void onWebSocketText(String message) {
//            this.received = message;
//            this.message.countDown();
//        }

    }
}
