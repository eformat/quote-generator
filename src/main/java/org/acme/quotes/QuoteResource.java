package org.acme.quotes;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import org.acme.data.Quote;
import org.jboss.resteasy.annotations.SseElementType;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Path("/quotes")
public class QuoteResource {

    private final Logger log = LoggerFactory.getLogger(QuoteResource.class);
    private final Random random = new Random();

    @Inject
    EventBus bus;

    @Inject
    Template quotes;

    @GET
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance listQuotes() {
        return quotes.data("quotes", getQuotes());
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType(MediaType.APPLICATION_JSON)
    public Publisher<Quote> stream() {
        Multi<Long> ticks = Multi.createFrom().ticks().every(Duration.ofSeconds(2)).onOverflow().drop();
        return ticks.on().subscribed(subscription -> log.info("We are subscribed!"))
                .on().cancellation(() -> log.info("Downstream has cancelled the interaction"))
                .onFailure().invoke(failure -> log.warn("Failed with " + failure.getMessage()))
                .onCompletion().invoke(() -> log.info("Completed"))
                .onItem().produceUni(
                        x -> quote()
                ).merge();
    }

    @ConsumeEvent(value = "AddQuote")
    public Uni<Quote> addQuote(Quote quote) {
        log.info("addQuote {}", quote);
        return Uni.createFrom().item(quote);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Quote> quote() {
        return bus.<Quote>request("AddQuote", getQuote("RHT", "Red Hat")).onItem().apply(Message::body);
    }

    private List<Quote> getQuotes() {
        List quotes = new ArrayList();
        quotes.add(getQuote("RHT", "Red Hat"));
        quotes.add(getQuote("FB", "Facebook"));
        quotes.add(getQuote("AMZN", "Amazon"));
        quotes.add(getQuote("NFLX", "Netflix"));
        quotes.add(getQuote("GOOGL", "Google"));
        quotes.add(getQuote("MSFT", "Microsoft"));
        return quotes;
    }


    private Quote getQuote(String symbol, String name) {
        Quote quote = new Quote();
        quote.setExchange("vert.x stock exchange");
        quote.setSymbol(symbol);
        quote.setName(name);
        quote.setBid(100 + random.nextInt(100 / 2));
        quote.setAsk(100 + random.nextInt(100 / 2));
        quote.setVolume(10000);
        quote.setPrice(100.0);
        quote.setShare(10000 / 2);
        return quote;
    }
}
