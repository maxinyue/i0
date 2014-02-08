package com.herenit.i0.container.tomcat.internal;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceFilter;
import com.thoughtworks.i0.config.HttpConfiguration;
import com.thoughtworks.i0.core.ServletContainer;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import javax.servlet.ServletException;


/**
 * Created by Obama on 14-2-6.
 */
public class Embedded implements ServletContainer {

    private final Tomcat tomcat;
    private final ImmutableMap.Builder<String, Injector> injectors = ImmutableMap.builder();
    private Injector injector;

    public Embedded(HttpConfiguration configuration) {
        tomcat = new Tomcat();
        try {
            tomcat.setPort(configuration.getPort());
            tomcat.init();
        } catch (LifecycleException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addServletContext(String name, boolean shareNothing, Module... modules) {
        injector = Guice.createInjector(modules);
//        tomcat.addContext("/*",".");
//        tomcat.addServlet(name, "default", new DefaultServlet());
        StandardServer server = (StandardServer) tomcat.getServer();

        AprLifecycleListener listener = new AprLifecycleListener();
        server.addLifecycleListener(listener);

        Context context;
        try {
            context = tomcat.addWebapp(name, ".");
            context.addFilterDef(createFilterDef("guice", GuiceFilter.class.getName()));
            context.addFilterMap(createFilterMap("guice", "/*"));

//            context.addContainerListener();
        } catch (ServletException e) {
            e.printStackTrace();
        }


    }

    @Override
    public Injector injector() {
        return injector;
    }

    @Override
    public void start(boolean standalone) throws Exception {
        tomcat.start();
        if (standalone) System.in.read();
    }

    @Override
    public void stop() throws Exception {
        tomcat.stop();
    }

    private FilterDef createFilterDef(String filterName, String filterClass) {
        FilterDef filterDef = new FilterDef();
        filterDef.setFilterName(filterName);
        filterDef.setFilterClass(filterClass);
        return filterDef;
    }

    private FilterMap createFilterMap(String filterName, String urlPattern) {
        FilterMap filterMap = new FilterMap();
        filterMap.setFilterName(filterName);
        filterMap.addURLPattern(urlPattern);
        return filterMap;
    }
}
