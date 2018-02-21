package com.cheddar.robinhood.client;

import com.cheddar.http.HTTPClient;
import com.cheddar.http.HTTPQuery;
import com.cheddar.http.HTTPResult;
import com.cheddar.robinhood.RobinhoodAPI;
import com.cheddar.robinhood.data.*;
import com.cheddar.robinhood.exception.RobinhoodException;
import com.cheddar.util.JSONUtil;
import com.cheddar.util.TimeUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

@Data
@Slf4j
public class RobinhoodClient implements RobinhoodAPI {

    private final HashMap<String, String> headers = new HashMap<String, String>() {{
        put("Accept", "*/*");
        put("Accept-Encoding", "gzip, deflate");
        put("Accept-Language", "en;q=1, fr;q=0.9, de;q=0.8, ja;q=0.7, nl;q=0.6, it;q=0.5");
        put("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        put("X-Robinhood-API-Version", "1.70.0");
        put("Connection", "keep-alive");
        put("User-Agent", "Robinhood/823 (iPhone; iOS 7.1.2; Scale/2.00)");
    }};

    private final HashMap<String, String> endpoints = new HashMap<String, String>() {{
        put("login", "https://api.robinhood.com/api-token-auth/");
        put("investment_profile", "https://api.robinhood.com/user/investment_profile/");
        put("accounts", "https://api.robinhood.com/accounts/");
        put("ach_iav_auth", "https://api.robinhood.com/ach/iav/auth/");
        put("ach_relationships", "https://api.robinhood.com/ach/relationships/");
        put("ach_transfers", "https://api.robinhood.com/ach/transfers/");
        put("applications", "https://api.robinhood.com/applications/");
        put("dividends", "https://api.robinhood.com/dividends/");
        put("edocuments", "https://api.robinhood.com/documents/");
        put("instruments", "https://api.robinhood.com/instruments/");
        put("margin_upgrades", "https://api.robinhood.com/margin/upgrades/");
        put("markets", "https://api.robinhood.com/markets/");
        put("notifications", "https://api.robinhood.com/notifications/");
        put("orders", "https://api.robinhood.com/orders/");
        put("password_reset", "https://api.robinhood.com/password_reset/request/");
        put("quotes", "https://api.robinhood.com/quotes/");
        put("document_requests", "https://api.robinhood.com/upload/document_requests/");
        put("user", "https://api.robinhood.com/user/");
        put("user/additional_info", "https://api.robinhood.com/user/additional_info/");
        put("user/basic_info", "https://api.robinhood.com/user/basic_info/");
        put("user/employment", "https://api.robinhood.com/user/employment/");
        put("user/investment_profile", "https://api.robinhood.com/user/investment_profile/");
        put("watchlists", "https://api.robinhood.com/watchlists/");
    }};

    private static final List<String> REQUIRED_ENDPOINT_KEYS = ImmutableList.of("positions", "portfolio", "account");

    private final HTTPClient httpClient = new HTTPClient();

    private String username;
    private String password;

    public RobinhoodClient(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    private boolean isLoggedIn() {
        if (!headers.containsKey("Authorization")) {
            return false;
        }

        for (final String key : REQUIRED_ENDPOINT_KEYS) {
            if (!endpoints.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    private void doLoginRoutine() {
        try {
            this.doLogin();
            this.acquireAccountInfo();
            log.info("Robinhood logged in.");
        } catch (final Exception e) {
            log.warn("Unable to log into Robinhood because " + e.getMessage(), e);
        }
    }

    private void verifyLoginStatus() throws RobinhoodException {
        if (!isLoggedIn()) {
            doLoginRoutine();
        }
        if (!isLoggedIn()) {
            throw new RobinhoodException("Login failed!");
        }
    }

    private void doLogin() throws RobinhoodException {
        log.info("Logging in to Robinhood.");

        Optional<HTTPResult> httpResult =
                this.httpClient.executeHTTPPostRequest(new HTTPQuery(endpoints.get("login"), ImmutableMap.of("username", username, "password", password), headers));

        if (!httpResult.isPresent()) {
            throw new RobinhoodException("Bad response from Robinhood.");
        }

        final String jsonData = httpResult.get().getBody();

        Optional<Map<String, String>> responseMap =
                JSONUtil.deserializeString(jsonData, new TypeReference<Map<String, String>>() {});

        if (!responseMap.isPresent()) {
            throw new RobinhoodException(String.format("Unable to deserialize response of [%s]", httpResult));
        }

        headers.put("Authorization", "Token " + responseMap.get().get("token"));
    }

    private void acquireAccountInfo() throws RobinhoodException {
        log.info("Acquiring account info.");

        final Optional<HTTPResult> getResponse = this.httpClient.executeHTTPGetRequest(new HTTPQuery(endpoints.get("accounts"), ImmutableMap.of(), headers));

        if (!getResponse.isPresent()) {
            throw new RobinhoodException("Bad response from Robinhood.");
        }

        Optional<Map<String, List<Map<String, Object>>>> callResults =
                JSONUtil.deserializeString(getResponse.get().getBody(), new TypeReference<Map<String, List<Map<String, Object>>>>() {});

        if (!callResults.isPresent()) {
            throw new RobinhoodException(String.format("Unable to deserialize response of [%s]", getResponse));
        }

        Map<String, List<Map<String, Object>>> accountsMap = callResults.get();

        final String positionsUrlStr = (String) accountsMap.get("results").get(0).get("positions");
        final String portfolioUrlStr = (String) accountsMap.get("results").get(0).get("portfolio");
        final String accountUrlStr = (String) accountsMap.get("results").get(0).get("url");

        final String accountNumber = (String) accountsMap.get("results").get(0).get("account_number");
        final String portfolioHistoricals = "https://api.robinhood.com/portfolios/historicals/" + accountNumber;

        endpoints.put("positions", positionsUrlStr);
        endpoints.put("portfolio", portfolioUrlStr);
        endpoints.put("portfolios_historicals", portfolioHistoricals);
        endpoints.put("account", accountUrlStr);
    }

    private Order placeLimitOrder(final String symbol, final int shares, final String side, final double limit) throws RobinhoodException {
        final Optional<Instrument> instrumentOptional = getInstrumentForSymbol(symbol);

        if (!instrumentOptional.isPresent()) {
            throw new RobinhoodException(String.format("Unable to determine instrument for symbol %s", symbol));
        }

        final Instrument instrument = instrumentOptional.get();

        final String maxPriceStr = String.format("%.2f", limit);

        log.info("Placing limit order of {} with a limit of {}", side, maxPriceStr);

        final Map<String, String> params =
                ImmutableMap.<String, String>builder().put("account", endpoints.get("account"))
                                                      .put("extended_hours", "true")
                                                      .put("override_dtbp_checks", "false")
                                                      .put("instrument", instrument.getUrl())
                                                      .put("side", side)
                                                      .put("quantity", String.valueOf(shares))
                                                      .put("symbol", symbol)
                                                      .put("time_in_force", "gfd")
                                                      .put("trigger", "immediate")
                                                      .put("price", maxPriceStr)
                                                      .put("type", "limit")
                                                      .build();

        log.info("Placing order {} {} {}", endpoints.get("orders"), params, headers);

        final Optional<HTTPResult> httpResult =
                this.httpClient.executeHTTPPostRequest(new HTTPQuery(endpoints.get("orders"), params, headers));

        if (!httpResult.isPresent()) {
            throw new RobinhoodException("Bad response from Robinhood.");
        }

        final String jsonData = httpResult.get().getBody();

        log.info("From order got response {}", jsonData);

        JSONObject jsonObj = new JSONObject(jsonData);

        if (jsonObj.has("non_field_errors")) {
            log.warn("Got error when executing tradeCommand {} : {}",
                     jsonObj.get("non_field_errors"), side + " -> " + symbol);
            throw new RobinhoodException("Bad response from Robinhood.");
        }

        if (jsonObj.has("detail")) {
            throw new RobinhoodException(String.format("Got unexpected response from Robinhood [%s]", jsonObj));
        }

        if (!jsonObj.isNull("reject_reason")) {
            throw new RobinhoodException(String.format("Got unexpected response from Robinhood [%s]", jsonObj));
        }

        final Optional<Order> placedOrder = JSONUtil.deserializeObject(jsonData, Order.class);

        if (!placedOrder.isPresent()) {
            log.warn("Could not create Order object from JSON response! {}", jsonData);
            throw new RobinhoodException(String.format("Got unexpected response from Robinhood [%s]", jsonObj));
        }

        final Order order = placedOrder.get();

        if (StringUtils.isEmpty(order.getId())) {
            log.warn("No order ID found! {}", jsonData);
            throw new RobinhoodException(String.format("Got unexpected response from Robinhood [%s]", jsonObj));
        }

        return order;
    }

    @Override
    public Order buyShares(final String symbol, final int shares, final double limit) throws RobinhoodException {
        verifyLoginStatus();
        return placeLimitOrder(symbol, shares, "buy", limit);
    }

    @Override
    public Order sellShares(final String symbol, final int shares, final double limit) throws RobinhoodException {
        verifyLoginStatus();
        return placeLimitOrder(symbol, shares, "sell", limit);
    }

    @Override
    public Optional<Order> getOrderFromURL(final String orderURL) throws RobinhoodException {
        final Optional<HTTPResult> httpResult = this.httpClient.executeHTTPGetRequest(new HTTPQuery(orderURL, ImmutableMap.of(), headers));


        if (!httpResult.isPresent()) {
            return Optional.empty();
        }

        Optional<Order> orderOptional = JSONUtil.deserializeObject(httpResult.get().getBody(), Order.class);

        return orderOptional;
    }

    @Override
    public List<EquityHistorical> getHistoricalValues(final String span, final String interval, final String bounds) throws RobinhoodException {
        verifyLoginStatus();

        final List<EquityHistorical> historicalValues = new ArrayList<>();

        Optional<HTTPResult> httpResult =
                this.httpClient.executeHTTPGetRequest(new HTTPQuery(endpoints.get("portfolios_historicals"),
                                                                    ImmutableMap.of("span", span,
                                                                                    "interval", interval,
                                                                                    "bounds", bounds), headers));

        final JSONObject jsonObj = new JSONObject(httpResult.get().getBody());

        final JSONArray equitiesListObj = jsonObj.getJSONArray("equity_historicals");

        for (final Object historicalObj : equitiesListObj) {
            if (!(historicalObj instanceof JSONObject)) {
                continue;
            }

            final Optional<EquityHistorical> equityOptional = JSONUtil.deserializeObject(historicalObj.toString(), EquityHistorical.class);

            if (!equityOptional.isPresent()) {
                continue;
            }

            historicalValues.add(equityOptional.get());
        }

        return historicalValues;
    }

    @Override
    public List<Quote> getQuotes(final Collection<String> symbols) throws RobinhoodException {
        if (CollectionUtils.isEmpty(symbols)) {
            return Collections.emptyList();
        }

        Map<String, String> params = ImmutableMap.of("symbols", Joiner.on(',').join(symbols));
        Optional<HTTPResult> httpResult = this.httpClient.executeHTTPGetRequest(new HTTPQuery(endpoints.get("quotes"), params, headers));

        if (!httpResult.isPresent()) {
            throw new RobinhoodException("Bad response from Robinhood.");
        }

        final String jsonData = httpResult.get().getBody();

        final Optional<Map<String, List<Quote>>> mapResult =
                JSONUtil.deserializeString(jsonData, new TypeReference<Map<String, List<Quote>>>() {});

        if (!mapResult.isPresent()) {
            throw new RobinhoodException(String.format("Unable to deserialize response of [%s]", jsonData));
        }

        final List<Quote> quotes = mapResult.get().get("results");

        quotes.removeIf(Objects::isNull);

        return quotes;
    }

    @Override
    public Quote getQuote(final String symbol) throws RobinhoodException {
        final Optional<HTTPResult> httpResult =
                this.httpClient.executeHTTPGetRequest(new HTTPQuery(endpoints.get("quotes"), ImmutableMap.of("symbols", symbol), headers));

        if (!httpResult.isPresent()) {
            throw new RobinhoodException("Bad response when getting quotes!");
        }

        final String jsonData = httpResult.get().getBody();

        final Optional<Map<String, List<Quote>>> mapResult =
                JSONUtil.deserializeString(jsonData, new TypeReference<Map<String, List<Quote>>>() {});

        if (!mapResult.isPresent()) {
            throw new RobinhoodException(String.format("Unable to deserialize response of [%s]", jsonData));
        }

        return mapResult.get().get("results").get(0);
    }

    @Override
    public Optional<Instrument> getInstrumentFromURL(final String instrumentURL) {
        final Optional<HTTPResult> httpResult =
                this.httpClient.executeHTTPGetRequest(new HTTPQuery(instrumentURL, ImmutableMap.of(), headers));

        if (!httpResult.isPresent()) {
            return Optional.empty();
        }

        Optional<Instrument> instrumentOptional = JSONUtil.deserializeObject(httpResult.get().getBody(), Instrument.class);

        return instrumentOptional;
    }

    @Override
    public Portfolio getPortfolio() throws RobinhoodException {
        verifyLoginStatus();

        final Optional<HTTPResult> httpResult =
                this.httpClient.executeHTTPGetRequest(new HTTPQuery(endpoints.get("portfolio"), ImmutableMap.of(), headers));

        if (!httpResult.isPresent()) {
            throw new RobinhoodException("Bad response from Robinhood.");
        }

        final Optional<Portfolio> portfolioOptional = JSONUtil.deserializeObject(httpResult.get().getBody(), Portfolio.class);

        if (!portfolioOptional.isPresent()) {
            throw new RobinhoodException("Bad response from Robinhood.");
        }

        return portfolioOptional.get();
    }

    @Override
    public Optional<Instrument> getInstrumentForSymbol(final String symbol) {
        final Optional<HTTPResult> httpResult =
                this.httpClient.executeHTTPGetRequest(new HTTPQuery(endpoints.get("instruments"), ImmutableMap.of("symbol", symbol), headers));

        if (!httpResult.isPresent()) {
            return Optional.empty();
        }

        final JSONObject jsonObj = new JSONObject(httpResult.get().getBody());

        final JSONArray jsonArray = jsonObj.getJSONArray("results");

        if (jsonArray.length() <= 0) {
            return Optional.empty();
        }

        final JSONObject jsonInstrumentObj = jsonArray.getJSONObject(0);

        final Optional<Instrument> instrumentOptional = JSONUtil.deserializeObject(jsonInstrumentObj.toString(), Instrument.class);

        return instrumentOptional;
    }

    @Override
    public MarginBalances getMarginBalances() throws RobinhoodException {
        verifyLoginStatus();

        final Optional<HTTPResult> httpResult =
                this.httpClient.executeHTTPGetRequest(new HTTPQuery(endpoints.get("account"), ImmutableMap.of(), headers));

        if (!httpResult.isPresent()) {
            throw new RobinhoodException("Bad response from Robinhood.");
        }

        final JSONObject jsonObj = new JSONObject(httpResult.get().getBody());

        Optional<MarginBalances> marginBalancesOptional = Optional.empty();

        if (!jsonObj.isNull("margin_balances")) {
            final JSONObject marginBalancesObj = jsonObj.getJSONObject("margin_balances");
            marginBalancesOptional = JSONUtil.deserializeObject(marginBalancesObj.toString(), MarginBalances.class);
        }

        if (!marginBalancesOptional.isPresent()) {
            throw new RobinhoodException("Bad response from Robinhood.");
        }

        log.info("Got buying power of {}", marginBalancesOptional.get());

        return marginBalancesOptional.get();
    }

    @Override
    public Set<Instrument> getAllInstruments() {

        String instrumentsURL = endpoints.get("instruments");

        final Set<Instrument> allInstruments = new HashSet<>();

        final Collection<HTTPResult> httpResults = scrapePaginatedAPI(instrumentsURL, ImmutableMap.of(), headers);

        for (final HTTPResult httpResult : httpResults) {
            final JSONObject callResult = new JSONObject(httpResult.getBody());

            final JSONArray instrumentsListObj = callResult.getJSONArray("results");

            for (final Object instrumentObj : instrumentsListObj) {
                if (!(instrumentObj instanceof JSONObject)) {
                    continue;
                }

                Optional<Instrument> instrumentOptional = JSONUtil.deserializeObject(instrumentObj.toString(), Instrument.class);

                if (!instrumentOptional.isPresent()) {
                    log.warn("Could not deserialize instrument {}", instrumentObj);
                    continue;
                }

                allInstruments.add(instrumentOptional.get());
            }
        }

        return allInstruments;
    }

    private Collection<HTTPResult> scrapePaginatedAPI(final String url, final Map<String, String> parameters, final Map<String, String> headers) {
        final Set<HTTPResult> httpResults = new HashSet<>();

        String urlToQuery = url;

        while (urlToQuery != null && !"null".equalsIgnoreCase(urlToQuery)) {
            log.info("Scraping paginated URL {} all instruments, have {} results so far.", urlToQuery, httpResults.size());

            final Optional<HTTPResult> getResult = this.httpClient.executeHTTPGetRequest(new HTTPQuery(urlToQuery, parameters, headers));

            if (!getResult.isPresent()) {
                break;
            }

            final JSONObject callResult = new JSONObject(getResult.get().getBody());

            final int sleepSeconds = Integer.valueOf(getResult.get().getHeaders().getOrDefault("Retry-After", "0"));

            if (sleepSeconds > 0) {
                try {
                    Thread.sleep(sleepSeconds * 1000);
                } catch (final InterruptedException e) {
                    log.warn(e.getMessage(), e);
                }
                continue;
            }

            httpResults.add(getResult.get());

            urlToQuery = String.valueOf(callResult.get("next"));
        }

        return httpResults;
    }

    @Override
    public List<Position> getPositions() throws RobinhoodException {
        verifyLoginStatus();

        log.info("Getting owned assets.");

        final List<Position> positions = new ArrayList<>();

        final Collection<HTTPResult> httpResults = scrapePaginatedAPI(endpoints.get("positions"), ImmutableMap.of(), headers);

        for (final HTTPResult httpResult : httpResults) {
            final JSONObject jsonResult = new JSONObject(httpResult.getBody());

            final JSONArray instrumentsListObj = jsonResult.getJSONArray("results");


            for (final Object positionObj : instrumentsListObj) {
                if (!(positionObj instanceof JSONObject)) {
                    continue;
                }

                Optional<Position> positionOptional = JSONUtil.deserializeObject(positionObj.toString(), Position.class);

                if (!positionOptional.isPresent()) {
                    log.warn("Could not deserialize position {}", positionOptional);
                    continue;
                }

                final Position position = positionOptional.get();

                if (Double.valueOf(position.getQuantity()).equals(0d)) {
                    continue;
                }

                positions.add(position);
            }
        }

        return positions;
    }

    @Override
    public Collection<Order> getOrdersAfterDate(final Date date) throws RobinhoodException {
        verifyLoginStatus();

        final Collection<Order> allOrders = new HashSet<>();

        final String dateStr = TimeUtil.createStrFromDate(date, "yyyy-MM-dd'T'HH:mm:ss.000000'Z'");

        final Collection<HTTPResult> httpResults = scrapePaginatedAPI(endpoints.get("orders"), ImmutableMap.of("updated_at[gte]", dateStr), headers);

        for (final HTTPResult httpResult : httpResults) {
            log.info("Getting all orders, have {} so far.", allOrders.size());

            final JSONObject callResult = new JSONObject(httpResult.getBody());

            final JSONArray ordersListObj = callResult.getJSONArray("results");

            for (final Object ordersObj : ordersListObj) {
                if (!(ordersObj instanceof JSONObject)) {
                    continue;
                }

                Optional<Order> order = JSONUtil.deserializeObject(ordersObj.toString(), Order.class);

                if (!order.isPresent()) {
                    log.warn("Could not deserialize order {}", ordersObj);
                    continue;
                }

                allOrders.add(order.get());
            }
        }

        return allOrders;
    }

    @Override
    public MarketState getMarketStateForDate(final DateTime dateTime) throws RobinhoodException {

        final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        final String dateString = formatter.print(dateTime);
        final String url = String.format("%s%s%s", endpoints.get("markets"), "XNAS/hours/", dateString);

        final Optional<HTTPResult> getResult = this.httpClient.executeHTTPGetRequest(new HTTPQuery(url, ImmutableMap.of(), headers));

        if (!getResult.isPresent()) {
            throw new RobinhoodException("Bad response from Robinhood.");
        }

        final Optional<Map<String, String>> responseObject =
                JSONUtil.deserializeString(getResult.get().getBody(), new TypeReference<Map<String, String>>() {});

        if (!responseObject.isPresent()) {
            throw new RobinhoodException(String.format("Unable to deserialize response of [%s]", getResult));
        }

        return new MarketState(responseObject.get());
    }

}
