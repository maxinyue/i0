package com.thoughtworks.i0.container.grizzly.internal;

import com.google.common.base.Function;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.thoughtworks.i0.config.Configuration;
import com.thoughtworks.i0.container.grizzly.WebSocket;
import com.thoughtworks.i0.container.grizzly.internal.tryus.TyrusWebsocketServerContainer;
import com.thoughtworks.i0.core.ApplicationModule;
import com.thoughtworks.i0.core.ContainerConfigurator;
import com.thoughtworks.i0.core.internal.util.ClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Set;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Iterables.transform;
import static com.thoughtworks.i0.core.internal.util.TypePredicates.isPresent;

public class WebSocketEnabler implements ContainerConfigurator<WebSocket, Configuration, Embedded> {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationModule.class);

    @Override
    public void configure(Embedded container, WebSocket annotation, ApplicationModule<Configuration> module) throws DeploymentException, IOException {
        final String[] autoScanPackages = new String[]{module.getClass().getPackage().getName()};
        ClassScanner scanner = new ClassScanner(annotation.packages().length == 0 ? autoScanPackages : annotation.packages());
        final Set<Class<?>> endpoints = scanner.findBy(isPresent(ServerEndpoint.class));
        if (logger.isInfoEnabled())
            logger.info(endpoints.isEmpty() ? "No WebSocket endpoint classes found." : "WebSocket endpoint classes found:\n  {}",
                    on("\n  ").join(transform(endpoints, LOG_FORMATTER)));
        if (!endpoints.isEmpty()) {
            Injector injector = container.injector().createChildInjector(new AbstractModule() {

                @Override
                protected void configure() {
                    for (Class<?> endpointClass : endpoints)
                        bind(endpointClass);
                }
            });
            TyrusWebsocketServerContainer tyrusWebsocketServerContainer = new TyrusWebsocketServerContainer(container, module.path(), endpoints, injector, null, module.configuration().getHttp().getPort());
            tyrusWebsocketServerContainer.start();
        }
    }

    public static final Function<Class<?>, String> LOG_FORMATTER = new Function<Class<?>, String>() {
        @Nullable
        @Override
        public String apply(@Nullable Class<?> input) {
            return String.format("%s --> %s", input, input.getAnnotation(ServerEndpoint.class).value());
        }
    };
}
