package org.acme.quotes;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import org.acme.data.Quote;
import org.acme.data.Quotes;
import org.jboss.resteasy.reactive.RestStreamElementType;
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
        return quotes.data("quotes", getQuotes().getQuotes());
    }

    @GET
    @Path("/stream")
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Publisher<Quotes> stream() {
        Multi<Long> ticks = Multi.createFrom().ticks().every(Duration.ofSeconds(2)).onOverflow().drop();
        return ticks.onItem().transformToUniAndMerge(
                x -> quote()
        );
    }

    @ConsumeEvent(value = "AddQuotes")
    public Uni<Quotes> addQuote(Quotes quotes) {
        return Uni.createFrom().item(quotes);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Quotes> quote() {
        return bus.<Quotes>request("AddQuotes", getQuotes()).onItem().transform(Message::body);
    }

    private Quotes getQuotes() {
        Quotes quotes = new Quotes();
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
