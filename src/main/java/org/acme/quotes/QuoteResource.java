package org.acme.quotes;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Path("/quotes")
public class QuoteResource {

    private final Logger log = LoggerFactory.getLogger(QuoteResource.class);
    private final Random random = new Random();

    // we want individual streams to show the same prices
    private final static Map<String, Double> bidPrices = new HashMap<>();
    private final static Map<String, Double> askPrices = new HashMap<>();

    // FANGMR's
    private final Map<String, String> symbols = Map.of(
            "FB", "Facebook",
            "AMZN", "Amazon",
            "NFLX", "Netflix",
            "GOOGL", "Google",
            "MSFT", "Microsoft",
            "RHT", "Red Hat");

    @Inject
    EventBus bus;

    @Inject
    Template quotes;

    @GET
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public TemplateInstance listQuotes() {
        return quotes.data("quotes", quote().await().atMost(Duration.ofMillis(10)).getQuotes());
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
        // this is our single source of quotes
        return bus.<Quotes>request("AddQuotes", getQuotes()).onItem().transform(Message::body);
    }

    private Quotes getQuotes() {
        Quotes quotes = new Quotes();
        symbols.forEach(
                (k, v) -> quotes.add(getQuote(k, v))
        );
        return quotes;
    }

    private Quote getQuote(String symbol, String name) {
        Quote quote = new Quote();
        quote.setExchange("vert.x stock exchange");
        quote.setSymbol(symbol);
        quote.setName(name);
        quote.setBid(getPrice(symbol, "bid"));
        quote.setAsk(getPrice(symbol, "ask"));
        quote.setVolume(10000);
        quote.setPrice(100.0);
        quote.setShare(10000 / 2);
        return quote;
    }

    @Scheduled(every = "3s")
    protected void incrementPrices() {
        symbols.forEach(
                (k, v) -> incrementPrice(k)
        );
    }

    // 50% of the time..
    static int rand50() {
        return (int) (10 * Math.random()) & 1;
    }

    // 75% of the time..
    static int rand75() {
        return rand50() | rand50();
    }

    private void incrementPrice(String symbol) {
        int askPrice = Integer.valueOf(100 + random.nextInt(100 / 2));
        int bidPrice;
        // 75% of the time we get a negative bid-ask spread
        if (rand75() > 0) {
            bidPrice = random.ints(askPrice - 20, askPrice )
                    .findFirst()
                    .getAsInt();
        } else {
            bidPrice = random.ints(askPrice, askPrice + 20)
                    .findFirst()
                    .getAsInt();
        }

        bidPrices.put(symbol, Double.valueOf(bidPrice));
        askPrices.put(symbol, Double.valueOf(askPrice));
    }

    private double getPrice(String symbol, String bidask) {
        if (bidask.equalsIgnoreCase("bid"))
            return bidPrices.getOrDefault(symbol, 100.0).doubleValue();
        else
            return askPrices.getOrDefault(symbol, 100.0).doubleValue();
    }

}
