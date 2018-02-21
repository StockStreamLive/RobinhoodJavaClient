package com.cheddar.robinhood.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Quote {

    private String symbol;

    private double ask_price;
    private double bid_price;

    private int ask_size;
    private int bid_size;

    private double last_trade_price;
    private double last_extended_hours_trade_price;

    private double previous_close;
    private double adjusted_previous_close;

    private String instrument;
}
