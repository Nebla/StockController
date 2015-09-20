package Order;

import java.io.Serializable;

/**
 * Created by adrian on 18/09/15.
 */
public class Order implements Serializable {

    private String orderId;
    private String productId;
    private Integer productQty;
    private OrderStatus orderStatus;

    public enum OrderStatus {
        RECEIVED,
        REJECTED,
        ACCEPTED,
        DELIVERED
    }

    public Order(String orderId, String productId, Integer productQty) {
        this.orderId = orderId;
        this.productId = productId;
        this.productQty = productQty;
        this.orderStatus = OrderStatus.RECEIVED;
    }

    public void updateStatus (Order.OrderStatus newStatus) {
        this.orderStatus = newStatus;
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

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }
}
