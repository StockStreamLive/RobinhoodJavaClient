package com.cheddar.robinhood.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {
    private double excess_maintenance;
    private double excess_margin;
    private double market_value;

    private double adjusted_equity_previous_close;
    private double equity_previous_close;
}
