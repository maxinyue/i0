package com.thoughtworks.i0.jersey.internal;

import com.google.inject.Injector;
import com.google.inject.Scope;
import com.google.inject.servlet.ServletScopes;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.WebComponent;
import org.glassfish.jersey.servlet.WebConfig;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Created by Obama on 14-2-3.
 */
@Singleton
public class GuiceContainer extends ServletContainer {

    private static final long serialVersionUID = 1931878850157940335L;

    private final Injector injector;

    @Inject
    public GuiceContainer(ResourceConfig resourceConfig, Injector injector) {
        super();
        this.injector = injector;
   //     getConfiguration().register(JacksonFeature.class);
    }

    @Override
    protected void init(WebConfig webConfig) throws ServletException {
        super.init(webConfig);
        ServiceLocator locator;
        try {
            Field webComponentField = getClass().getSuperclass()
                    .getDeclaredField("webComponent");
            webComponentField.setAccessible(true);
            WebComponent webComponent = (WebComponent) webComponentField.get(this);
            Field appHandlerField = webComponent.getClass().getDeclaredField("appHandler");
            appHandlerField.setAccessible(true);
            ApplicationHandler appHandler = (ApplicationHandler) appHandlerField.get(webComponent);
            locator = appHandler.getServiceLocator();
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        GuiceBridge.getGuiceBridge().initializeGuiceBridge(
                locator);
        GuiceIntoHK2Bridge guiceBridge = locator
                .getService(GuiceIntoHK2Bridge.class);
        guiceBridge.bridgeGuiceInjector(injector);
    }

    public Injector getInjector() {
        return injector;
    }


}
