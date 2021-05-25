package org.acme;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.impl.ClientPhase;
import io.vertx.ext.web.client.impl.WebClientInternal;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.jboss.resteasy.core.ResteasyContext;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/hello-resteasy")
public class GreetingResource {
    @Inject
    Vertx vertx;

    private WebClient client;

    @PostConstruct
    void initialize() {
        WebClientInternal client = (WebClientInternal) io.vertx.ext.web.client.WebClient.create(
                vertx,
                new WebClientOptions()
                        .setDefaultHost("localhost")
                        .setDefaultPort(9999)
        );
        client.addInterceptor(event -> {
            final ClientPhase phase = event.phase();
            if (phase == ClientPhase.PREPARE_REQUEST) {
                final Ctx data = ResteasyContext.getContextData(Ctx.class);
                if (data == null) {
                    event.fail(new RuntimeException("no data in context"));
                }
            }
            event.next();
        });

        this.client = WebClient.newInstance(client);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/test")
    public String test() {
        return "test";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> hello() {
        ResteasyContext.pushContext(Ctx.class, new Ctx("test"));
        return client.get("/hello-resteasy/test")
                .send()
                .map(HttpResponse::bodyAsString)
                .map(it -> "Hello RESTEasy")
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())//if comment this line test passes
                ;
    }

    public static class Ctx {
        final String value;

        public Ctx(String value) {
            this.value = value;
        }
    }
}


