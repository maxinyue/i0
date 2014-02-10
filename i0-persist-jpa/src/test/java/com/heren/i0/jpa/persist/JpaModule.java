package com.heren.i0.jpa.persist;

import com.heren.i0.config.Configuration;
import com.heren.i0.container.grizzly.EmbeddedGrizzly;
import com.heren.i0.core.Application;
import com.heren.i0.core.ApplicationModule;
import com.heren.i0.core.Servlet3;
import com.heren.i0.jpa.JpaConfiguration;
import com.heren.i0.jpa.JpaPersist;
import com.heren.i0.jpa.config.H2;
import com.heren.i0.jpa.config.Hibernate;

import static com.heren.i0.config.Configuration.config;
import static com.heren.i0.jpa.DatabaseConfiguration.database;


@JpaPersist(unit = "domain")
@EmbeddedGrizzly
@Servlet3
@Application("jpa")
public class JpaModule extends ApplicationModule<JpaConfiguration> {
    @Override
    protected JpaConfiguration createDefaultConfiguration(Configuration.ConfigurationBuilder config) {
        return new JpaConfiguration(config().logging().console().end().end().build(),
                database().with(H2.driver, H2.tempFileDB, H2.compatible("ORACLE"),
                        Hibernate.dialect("Oracle10g"), Hibernate.showSql)
                        .user("sa").password("").build());
    }
}
