package com.heren.i0.jetty.websockets;

import com.heren.i0.config.Configuration;
import com.heren.i0.config.util.LogLevel;
import com.heren.i0.core.Application;
import com.heren.i0.core.ApplicationModule;
import com.heren.i0.jetty.EmbeddedJetty;
import com.heren.i0.jetty.WebSocket;

import static com.heren.i0.config.Configuration.config;

@EmbeddedJetty
@WebSocket
@Application("websocket")
public class WebSocektApplication extends ApplicationModule<Configuration> {
    @Override
    protected Configuration createDefaultConfiguration(Configuration.ConfigurationBuilder config) {
        return config().http().port(8051).end().logging().level(LogLevel.INFO).console().end().end().build();
    }
}
