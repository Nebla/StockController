package Order;

import java.io.Serializable;

/**
 * Created by adrian on 21/09/15.
 */
public class OrderStatusMessage implements Serializable {
    private String orderId;
    private Order.OrderStatus orderStatus;

    public OrderStatusMessage(String orderId, Order.OrderStatus status) {
        this.orderId = orderId;
        this.orderStatus = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public Order.OrderStatus getOrderStatus() {
        return orderStatus;
    }
}
