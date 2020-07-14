package org.acme.data;

import java.util.ArrayList;
import java.util.List;

public class Quotes {

    private List<Quote> quotes;

    public List<Quote> getQuotes() {
        return quotes;
    }

    public void setQuotes(List<Quote> quotes) {
        this.quotes = quotes;
    }

    public void add(Quote quote) {
        if (null == quotes) {
            quotes = new ArrayList<>();
        }
        this.quotes.add(quote);
    }
}
