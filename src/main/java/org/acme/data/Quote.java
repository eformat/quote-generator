package org.acme.data;

public class Quote {

    private String exchange;
    private String name;
    private long period;
    private String symbol;
    private int stocks;
    private double price;
    private double bid;
    private double ask;
    private int share;
    private double value;
    private int volume;

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getStocks() {
        return stocks;
    }

    public void setStocks(int stocks) {
        this.stocks = stocks;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getBid() {
        return bid;
    }

    public void setBid(double bid) {
        this.bid = bid;
    }

    public double getAsk() {
        return ask;
    }

    public void setAsk(double ask) {
        this.ask = ask;
    }

    public int getShare() {
        return share;
    }

    public void setShare(int share) {
        this.share = share;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    @Override
    public String toString() {
        return "Quote{" +
                "exchange='" + exchange + '\'' +
                ", name='" + name + '\'' +
                ", period=" + period +
                ", symbol='" + symbol + '\'' +
                ", stocks=" + stocks +
                ", price=" + price +
                ", bid=" + bid +
                ", ask=" + ask +
                ", share=" + share +
                ", value=" + value +
                ", volume=" + volume +
                '}';
    }
}
