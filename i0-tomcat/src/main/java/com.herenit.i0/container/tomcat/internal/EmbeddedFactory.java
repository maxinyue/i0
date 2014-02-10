package com.herenit.i0.container.tomcat.internal;

import com.google.inject.Binder;
import com.google.inject.servlet.ServletModule;
import com.herenit.i0.container.tomcat.EmbeddedTomcat;
import com.heren.i0.config.Configuration;
import com.heren.i0.core.ApplicationModule;
import com.heren.i0.core.BindingProvider;
import com.heren.i0.core.ContainerCreator;
import com.heren.i0.core.ServletContainer;
import com.heren.i0.core.internal.servlet.AssetServlet;

import java.util.HashMap;
import java.util.Map;


public class EmbeddedFactory implements ContainerCreator<EmbeddedTomcat, Configuration>, BindingProvider<EmbeddedTomcat, Configuration> {

    @Override
    public ServletContainer create(EmbeddedTomcat annotation, Configuration configuration) {
        return new Embedded(configuration.getHttp());
    }

    @Override
    public void configure(Binder binder, final EmbeddedTomcat annotation, ApplicationModule<?> module, Configuration configuration) {
        if (annotation.assets().length == 0) return;
        binder.install(new ServletModule() {
            @Override
            protected void configureServlets() {
                for (EmbeddedTomcat.Asset asset : annotation.assets()) {
                    serve(asset.uri() + "/*").with(new AssetServlet(asset.resource()).setMimeExtensions(toMap(annotation.mimeExtensions())));
                }
            }
        });
    }

    private Map<String, String> toMap(EmbeddedTomcat.MimeExtension[] mimeExtensions) {
        Map<String, String> map = new HashMap<>();
        for (EmbeddedTomcat.MimeExtension mimeExtension : mimeExtensions) {
            map.put(mimeExtension.extension(), mimeExtension.mime());
        }
        return map;
    }
}
