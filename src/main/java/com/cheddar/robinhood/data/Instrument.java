package com.cheddar.robinhood.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Instrument {
    private String url;
    private String symbol;
    private String name;
    private float day_trade_ratio;
    private boolean tradeable;
    private float min_tick_size;
}
