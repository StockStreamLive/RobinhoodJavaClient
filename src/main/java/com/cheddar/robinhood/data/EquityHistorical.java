package com.cheddar.robinhood.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquityHistorical {
    private String begins_at;

    private double adjusted_open_equity;
    private double adjusted_close_equity;

    private double net_return;

    private double open_equity;
    private double close_equity;

    private double open_market_value;
    private double close_market_value;
}
