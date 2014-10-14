package com.heren.i0.container.grizzly.internal.tryus;

import com.google.inject.Injector;
import com.heren.i0.container.grizzly.internal.Embedded;
import org.glassfish.tyrus.core.*;
import org.glassfish.tyrus.server.TyrusServerContainer;
import org.glassfish.tyrus.spi.WebSocketEngine;

import javax.websocket.DeploymentException;
import javax.websocket.EndpointConfig;
import javax.websocket.server.ServerEndpointConfig;
import java.io.IOException;
import java.util.Set;

/**
 * Created by Obama on 14-2-2.
 */
public class TyrusWebsocketServerContainer extends TyrusServerContainer {

    private final Injector injector;
    private final Embedded server;
    private final String contextPath;
    private final TyrusWebSocketEngine engine;
    private final int port;
    private final ErrorCollector collector;
    private final ComponentProviderService componentProvider;
    private final Set<Class<?>> classes;
    public final static int DEFAULT_INCOMING_BUFFER_SIZE=4194315;

    public TyrusWebsocketServerContainer(final Embedded server, final String contextPath,
                                         final Set<Class<?>> classes, Injector injector, TyrusWebSocketEngine engine, int port) {
        super(classes);
        this.classes = classes;
        this.injector = injector;
        this.server = server;
        this.contextPath = contextPath;
        this.engine = engine == null ? TyrusWebSocketEngine.builder(this).build() : engine;
        this.port = port;
        this.collector = new ErrorCollector();
        componentProvider = ComponentProviderService.create();
    }

    public void start() throws IOException, DeploymentException {
        for (Class<?> endpointClass : classes) {
            AnnotatedEndpoint endpoint = AnnotatedEndpoint.fromInstance(injector.getInstance(endpointClass), componentProvider, true,DEFAULT_INCOMING_BUFFER_SIZE, collector);
            register(endpoint);
        }
        server.getServer().getListener("grizzly").getKeepAlive().setIdleTimeoutInSeconds(-1);
        server.getServer().getListener("grizzly").registerAddOn(new WebSocketAddOn(this));
    }

    public void register(AnnotatedEndpoint endpoint) throws DeploymentException {
        engine.register(endpoint, contextPath);
    }

    @Override
    public void register(Class<?> endpointClass) throws DeploymentException {
        engine.register(endpointClass, contextPath);
    }

    @Override
    public void register(ServerEndpointConfig serverEndpointConfig) throws DeploymentException {
        engine.register(serverEndpointConfig, contextPath);
    }

    @Override
    public WebSocketEngine getWebSocketEngine() {
        return engine;
    }
}
