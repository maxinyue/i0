package com.heren.i0.jetty.servlet;

import com.heren.i0.config.Configuration;
import com.heren.i0.config.util.LogLevel;
import com.heren.i0.core.Application;
import com.heren.i0.core.ApplicationModule;
import com.heren.i0.core.Servlet3;
import com.heren.i0.jetty.EmbeddedJetty;

import static com.heren.i0.config.Configuration.config;
import static com.heren.i0.jetty.EmbeddedJetty.Asset;

@Application("embedded")
@EmbeddedJetty(assets = @Asset(uri = "/static", resource = "./webapp"),
        mimeExtensions = {@EmbeddedJetty.MimeExtension(extension = "eot", mime = "application/vnd.ms-fontobject"),
                @EmbeddedJetty.MimeExtension(extension = "svg", mime = "image/svg+xml")})
@Servlet3
public class EmbeddedContainer extends ApplicationModule<Configuration> {
    @Override
    protected Configuration createDefaultConfiguration(Configuration.ConfigurationBuilder config) {
        return config().http().port(8051).end().logging().level(LogLevel.INFO).console().end().end().build();
    }
}
