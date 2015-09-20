package Order;

import Util.Util;
import com.rabbitmq.client.*;

import Error.StockControllerException;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Created by adrian on 20/09/15.
 */
public class OrderRequestConsumer extends DefaultConsumer {

    private Channel responseChannel;
    private String responseQueueName;

    public OrderRequestConsumer(Channel channel) {
        super(channel);
    }

    public void init() throws StockControllerException {
        try {
            String[] propertiesNames = {"queueHost", "orderResponseQueueName"};
            Map<String, String> propertiesValues = Util.getProperties(propertiesNames);

            String queueHost = propertiesValues.get("queueHost");
            responseQueueName = propertiesValues.get("orderResponseQueueName");

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(queueHost);
            Connection connection = factory.newConnection();
            responseChannel = connection.createChannel();
            responseChannel.queueDeclare(responseQueueName, false, false, false, null);

        } catch (TimeoutException e) {
            throw new StockControllerException("There was a timeout while trying to connect to queue server");
        } catch (IOException e) {
            throw new StockControllerException("There was a problem while trying to connect to the I/O device");
        }
    }

    public void handleDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) {
        try {
            Order order = SerializationUtils.deserialize(bytes);
            this.getOrderStatus(order);

            long deliveryTag = envelope.getDeliveryTag();
            getChannel().basicAck(deliveryTag, true);
        } catch (StockControllerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getOrderStatus (Order order) throws StockControllerException {
        

    }
}
