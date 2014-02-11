package com.heren.i0.jersey.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.heren.i0.config.Configuration;
import com.heren.i0.core.ApplicationModule;
import com.heren.i0.core.BindingProvider;
import com.heren.i0.core.internal.servlet.AutoScanningServletModule;
import com.heren.i0.jersey.RestApi;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import static com.google.common.base.Joiner.on;

public class JerseyEnabler implements BindingProvider<RestApi, Configuration> {

    @Override
    public void configure(Binder binder, final RestApi annotation, ApplicationModule<?> module, Configuration configuration) {
        final String[] autoScanPackages = new String[]{module.getClass().getPackage().getName()};
        binder.install(new AutoScanningServletModule() {

            @Override
            protected void configureServlets() {
                System.out.println("------------" + GuiceContainer.class.getName());
//                bind(JacksonJaxbJsonProvider.class).in(Singleton.class);

                ImmutableSet<String> packageSet = ImmutableSet.<String>builder()
                        .add(annotation.packages().length == 0 ? autoScanPackages : annotation.packages()).build();
//                bind(ResourceConfig.class).toInstance(new MyApplication(on(",").skipNulls().join(packageSet), org.glassfish.jersey.jackson.JacksonFeature.class));
                bind(ResourceConfig.class).toInstance(new I0ResourceConfig(on(",").skipNulls().join(packageSet)));
                serve(annotation.prefix()).with(GuiceContainer.class, new ImmutableMap.Builder<String, String>()
                        .put(ServerProperties.PROVIDER_PACKAGES, on(",").skipNulls().join(packageSet)).build());
//                serve(annotation.prefix()).with(Application.class);
            }
        });
    }
}
