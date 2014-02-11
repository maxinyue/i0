package com.heren.i0.jersey.internal;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

/**
 * Created by Obama on 14-2-11.
 */
public class I0ResourceConfig extends ResourceConfig{

    public I0ResourceConfig(String packages) {
        property(ServerProperties.BV_DISABLE_VALIDATE_ON_EXECUTABLE_OVERRIDE_CHECK, true);
        packages(packages);
    }
}
