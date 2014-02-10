package com.heren.i0.core.guicemodule;

import com.heren.i0.config.Configuration;
import com.heren.i0.core.ApplicationModule;
import com.heren.i0.core.GuiceModule;

@GuiceModule(packages = "com.heren.i0.core.guicemodule.p2")
public class Specified extends ApplicationModule<Configuration> {
    @Override
    protected Configuration createDefaultConfiguration(Configuration.ConfigurationBuilder config) {
        return Configuration.config().http().end().build();
    }
}
