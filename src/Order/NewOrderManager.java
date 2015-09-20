package Order;

import Error.StockControllerException;
import Stock.NewStockConsumer;
import Util.Util;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Created by adrian on 16/09/15.
 */
public class NewOrderManager {

    public static void main(String[] argv) {
        try {
            receiveNewOrders();
        } catch (StockControllerException e) {
            e.printStackTrace();
        }
    }

    private static void receiveNewOrders() throws StockControllerException {
        String[] propertiesName = {"queueHost","newOrderQueueName"};
        Map<String, String> queueNames = Util.getProperties(propertiesName);
        String queueHost = queueNames.get("queueHost");
        String queueName = queueNames.get("newOrderQueueName");

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(queueHost);
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(queueName, false, false, false, null);
            NewOroderConsumer consumer = new NewOroderConsumer(channel);
            boolean autoAck = false;
            channel.basicConsume(queueName, autoAck, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}
