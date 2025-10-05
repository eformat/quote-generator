package org.acme.quotes;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Flow.Publisher;
import org.acme.data.Quote;
import org.acme.data.Quotes;
import org.jboss.resteasy.reactive.RestStreamElementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/quotes")
public class QuoteResource {

    private final Logger log = LoggerFactory.getLogger(QuoteResource.class);
    private final Random random = new Random();

    // we want individual streams to show the same prices
    private final static Map<String, Double> bidPrices = new HashMap<>();
    private final static Map<String, Double> askPrices = new HashMap<>();
    // maintain a per-symbol phase for sinusoidal price movements
    private final static Map<String, Double> pricePhase = new HashMap<>();
    // ensure half of symbols drift up and the other half drift down
    private final static Map<String, Integer> driftDirections = new HashMap<>();

    // FANGMR's
    private final Map<String, String> symbols = Map.of(
            "FB", "Facebook",
            "AMZN", "Amazon",
            "NFLX", "Netflix",
            "GOOGL", "Google",
            "MSFT", "Microsoft",
            "RHT", "Red Hat");

    private void initializeDriftDirections() {
        if (!driftDirections.isEmpty()) return;
        ArrayList<String> keys = new ArrayList<>(symbols.keySet());
        Collections.shuffle(keys, random);
        int half = keys.size() / 2;
        for (int i = 0; i < keys.size(); i++) {
            String sym = keys.get(i);
            driftDirections.put(sym, i < half ? 1 : -1);
        }
        // Ensure RHT never drifts down
        driftDirections.put("RHT", 1);
    }

    @Inject
    EventBus bus;

    @Inject
    Template quotes;

    @Inject
    EntityManager em;

    @GET
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public TemplateInstance listQuotes() {
        return quotes.data("quotes", quote().await().atMost(Duration.ofSeconds(2)).getQuotes());
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Publisher<Quotes> stream() {
        Multi<Long> ticks = Multi.createFrom().ticks().every(Duration.ofSeconds(2)).onOverflow().drop();
        return ticks.onItem().transformToUniAndMerge(
                x -> quote()
        );
    }

    @ConsumeEvent(value = "AddQuotes")
    @Blocking
    @Transactional
    public Uni<Quotes> addQuote(Quotes quotes) {
        if (quotes != null && quotes.getQuotes() != null) {
            quotes.getQuotes().forEach(em::persist);
        }
        return Uni.createFrom().item(quotes);
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    @Transactional
    public String countQuotes() {
        Long count = em.createQuery("select count(q) from Quote q", Long.class).getSingleResult();
        return String.valueOf(count);
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
        double bid = getPrice(symbol, "bid");
        double ask = getPrice(symbol, "ask");
        double mid = (bid + ask) / 2.0;
        double spread = ask - bid;
        quote.setBid(bid);
        quote.setAsk(ask);
        quote.setVolume(10000);
        quote.setPrice(mid);
        quote.setSpread(spread);
        quote.setShare(10000 / 2);
        return quote;
    }

    @Scheduled(every = "3s")
    protected void incrementPrices() {
        initializeDriftDirections();
        symbols.forEach(
                (k, v) -> incrementPrice(k)
        );
    }

    @Scheduled(every = "2m")
    protected void reshuffleDriftDirections() {
        ArrayList<String> keys = new ArrayList<>(symbols.keySet());
        Collections.shuffle(keys, random);
        driftDirections.clear();
        int half = keys.size() / 2;
        for (int i = 0; i < keys.size(); i++) {
            String sym = keys.get(i);
            driftDirections.put(sym, i < half ? 1 : -1);
        }
        // Ensure RHT never drifts down even after reshuffle
        driftDirections.put("RHT", 1);
    }

    // 50% of the time..
    static int rand50() {
        return (int) (10 * Math.random()) & 1;
    }

    // 75% of the time..
    static int rand75() {
        return rand50() | rand50();
    }

    private int getDriftDirection(String symbol) {
        // Enforce upward drift for RHT
        if ("RHT".equals(symbol)) {
            return 1;
        }
        Integer dir = driftDirections.get(symbol);
        if (dir == null) {
            initializeDriftDirections();
            dir = driftDirections.getOrDefault(symbol, 1);
        }
        return dir;
    }

    private void incrementPrice(String symbol) {
        // Use previous prices to introduce a positive drift over time
        double previousAsk = askPrices.getOrDefault(symbol, 100.0);
        double previousBid = bidPrices.getOrDefault(symbol, 99.5);
        double previousMid = (previousAsk + previousBid) / 2.0;

        // Positive drift plus sinusoidal oscillation and a touch of noise
        double phase = pricePhase.getOrDefault(symbol, 0.0);
        phase += 0.2; // advance phase each tick (~31 ticks per full cycle)
        pricePhase.put(symbol, phase);

        int direction = getDriftDirection(symbol); // +1 for up-trend, -1 for down-trend
        double drift = 0.001 * direction; // ±0.1% baseline drift per tick
        double amplitude = 0.006; // ±0.6% oscillation
        double noise = (random.nextDouble() - 0.5) * 0.001; // ±0.05% noise
        double percentMove = drift + (amplitude * Math.sin(phase)) + noise;

        double newMid = previousMid * (1.0 + percentMove);

        // Small realistic spread between bid and ask
        double spread = 0.05 + (random.nextDouble() * 0.55); // $0.05 to $0.60
        double baseAsk = Math.max(0.01, newMid + (spread / 2.0));
        double baseBid = Math.max(0.01, newMid - (spread / 2.0));

        // 10% of the time, invert spread to allow arbitrage (bid > ask)
        boolean allowArbitrage = random.nextDouble() < 0.10;
        double newAsk = allowArbitrage ? baseBid : baseAsk;
        double newBid = allowArbitrage ? baseAsk : baseBid;

        // Round to cents
        newAsk = Math.round(newAsk * 100.0) / 100.0;
        newBid = Math.round(newBid * 100.0) / 100.0;

        askPrices.put(symbol, newAsk);
        bidPrices.put(symbol, newBid);
    }

    private double getPrice(String symbol, String bidask) {
        if (bidask.equalsIgnoreCase("bid"))
            return bidPrices.getOrDefault(symbol, 100.0).doubleValue();
        else
            return askPrices.getOrDefault(symbol, 100.0).doubleValue();
    }

}
