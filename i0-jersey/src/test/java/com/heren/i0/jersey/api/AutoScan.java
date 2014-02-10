package com.heren.i0.jersey.api;

import com.heren.i0.config.Configuration;
import com.heren.i0.config.util.LogLevel;
import com.heren.i0.container.grizzly.EmbeddedGrizzly;
import com.heren.i0.core.Application;
import com.heren.i0.core.ApplicationModule;
import com.heren.i0.jersey.RestApi;

import static com.heren.i0.config.Configuration.config;

@Application("autoscan") @EmbeddedGrizzly
@RestApi
public class AutoScan extends ApplicationModule<Configuration> {
    @Override
    protected Configuration createDefaultConfiguration(Configuration.ConfigurationBuilder config) {
        return config().http().port(8051).end().logging().level(LogLevel.DEBUG).console().end().end().build();
    }
}
