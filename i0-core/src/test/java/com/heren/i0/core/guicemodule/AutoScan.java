package com.heren.i0.core.guicemodule;

import com.heren.i0.config.Configuration;
import com.heren.i0.core.Application;
import com.heren.i0.core.ApplicationModule;
import com.heren.i0.core.GuiceModule;

@GuiceModule
@Application("autoscan")
public class AutoScan extends ApplicationModule<Configuration> {
    @Override
    protected Configuration createDefaultConfiguration(Configuration.ConfigurationBuilder config) {
        return Configuration.config().http().end().build();
    }
}
