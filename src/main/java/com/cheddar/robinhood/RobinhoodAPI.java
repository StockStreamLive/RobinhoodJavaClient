package com.cheddar.robinhood;

import com.cheddar.robinhood.data.*;
import com.cheddar.robinhood.exception.RobinhoodException;
import org.joda.time.DateTime;

import java.util.*;

public interface RobinhoodAPI {

    Portfolio getPortfolio() throws RobinhoodException;

    Set<Instrument> getAllInstruments();

    Optional<Instrument> getInstrumentFromURL(final String instrumentURL);

    Optional<Instrument> getInstrumentForSymbol(final String symbol);

    public MarginBalances getMarginBalances() throws RobinhoodException;

    List<Position> getPositions() throws RobinhoodException;

    Collection<Order> getOrdersAfterDate(final Date date) throws RobinhoodException;

    List<EquityHistorical> getHistoricalValues(final String span, final String interval, final String bounds) throws RobinhoodException;

    List<Quote> getQuotes(final Collection<String> symbols) throws RobinhoodException;

    Quote getQuote(final String symbol) throws RobinhoodException;

    Order buyShares(final String symbol, final int shares, final double limit) throws RobinhoodException;

    Order sellShares(final String symbol, final int shares, final double limit) throws RobinhoodException;

    Optional<Order> getOrderFromURL(final String orderURL) throws RobinhoodException;

    MarketState getMarketStateForDate(final DateTime dateTime) throws RobinhoodException;

}
