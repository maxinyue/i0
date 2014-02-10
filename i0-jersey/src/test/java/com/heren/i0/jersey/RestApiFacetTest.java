package com.heren.i0.jersey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heren.i0.core.Launcher;
import com.heren.i0.core.ServletContainer;
import com.heren.i0.jersey.api.AutoScan;
import com.heren.i0.jersey.api.Specified;
import com.heren.i0.jersey.api.V2;
import com.heren.i0.jersey.api.p2.Data;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.After;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import java.io.IOException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class RestApiFacetTest {
    private ServletContainer server;

    @Test
    public void should_auto_scan_all_packages() throws Exception {
        server = Launcher.launch(new AutoScan(), false);
        assertThat(get("http://localhost:8051/autoscan/api/p1"), is("resource1"));
        assertThat(get("http://localhost:8051/autoscan/api/p2"), is("resource2"));
    }

    @Test
    public void should_register_all_packages_under_different_prefix() throws Exception {
        server = Launcher.launch(new V2(), false);
        assertThat(get("http://localhost:8051/autoscan/api/v2/p1"), is("resource1"));
        assertThat(get("http://localhost:8051/autoscan/api/v2/p2"), is("resource2"));
    }

//    @Test(expected = UniformInterfaceException.class)
//    public void should_auto_scan_specified_packages() throws Exception {
//        server = Launcher.launch(new Specified(), false);
//        assertThat(get("http://localhost:8051/autoscan/api/p2"), is("resource2"));
//        get("http://localhost:8051/autoscan/api/p1");
//    }

    @Test
    public void should_support_json_response() throws Exception {
        server = Launcher.launch(new Specified(), false);
        assertThat(json("http://localhost:8051/autoscan/api/p2/data", Data.class), is(new Data("data")));
    }

    @Test
    public void should_support_json_request() throws Exception {
        server = Launcher.launch(new Specified(), false);
        //    System.out.println(create().target("http://localhost:8051/autoscan/api/p2/echo").request(APPLICATION_JSON_TYPE).post(Entity.text(new Data("value"))).getStatus());
        assertThat(create().target("http://localhost:8051/autoscan/api/p2/echo").request(APPLICATION_JSON_TYPE).post(Entity.entity(new Data("value"),APPLICATION_JSON_TYPE)).readEntity(Data.class), is(new Data("value")));
    }

    private String get(String uri) {
        return create().target(uri).request().get(String.class);
    }

    private Client create() {
        return ClientBuilder.newBuilder().register((JacksonFeature.class)).build();
    }

    private <T> T json(String uri, Class<T> type) throws IOException {
        String json = create().target(uri).request(APPLICATION_JSON_TYPE).get(String.class);
        return new ObjectMapper().reader(type).readValue(json);
    }

    @After
    public void after() throws Exception {
        if (server != null) server.stop();
    }
}
