package com.cheddar.robinhood.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarginBalances {
    private double unallocated_margin_cash;
    private double margin_limit;
    private double start_of_day_dtbp;
    private double overnight_buying_power;
}
