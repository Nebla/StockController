package Order;

import java.io.Serializable;

/**
 * Created by adrian on 18/09/15.
 */
public class NewOrder implements Serializable {

    private String orderId;
    private String productId;
    private Integer productQty;

    public NewOrder(String orderId, String productId, Integer productQty) {
        this.orderId = orderId;
        this.productId = productId;
        this.productQty = productQty;
    }

    public String getProductId() {
        return productId;
    }

    public Integer getProductQty() {
        return productQty;
    }

    public String getOrderId() {
        return orderId;
    }
}
