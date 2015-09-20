package Stock;

import java.io.Serializable;

/**
 * Created by adrian on 19/09/15.
 */
public class Stock implements Serializable {

    private String name;
    private Integer qty;

    public Stock(String name, Integer qty) {
        this.name = name;
        this.qty = qty;
    }

    public String getProductId() {
        return name;
    }

    public Integer getProductQty() {
        return qty;
    }
}
