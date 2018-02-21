package com.cheddar.robinhood.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class Order {
    private String id;
    private String state;
    private String created_at;
    private String average_price;
    private String price;
    private String url;
    private String side;
    private String instrument;
    private String quantity;
    private List<Execution> executions = new ArrayList<>();

    public Order() {

    }
}
