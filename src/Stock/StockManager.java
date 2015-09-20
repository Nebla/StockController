package Stock;

import Log.OrderAuditoryConsumer;

import Error.StockControllerException;

import Util.Util;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

/**
 * Created by adrian on 16/09/15.
 */
public class StockManager {

    public static void main(String[] argv) {
        try {
            startUpdating();
        } catch (StockControllerException e) {
            e.printStackTrace();
        }
    }

    private static void startUpdating() throws StockControllerException {
        String[] propertiesName = {"queueHost","stockQueueName"};
        Map<String, String> queueNames = Util.getProperties(propertiesName);
        String queueHost = queueNames.get("queueHost");
        String queueName = queueNames.get("stockQueueName");

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(queueHost);
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(queueName, false, false, false, null);
            NewStockConsumer consumer = new NewStockConsumer(channel);
            boolean autoAck = false;
            channel.basicConsume(queueName, autoAck, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}