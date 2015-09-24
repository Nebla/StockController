package Order;

import Error.StockControllerException;
import Util.Util;
import com.rabbitmq.client.*;
import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Created by adrian on 20/09/15.
 */
public class OrderRequestConsumer extends DefaultConsumer {

    private Channel responseChannel;
    private String responseQueueName;
    private Integer numberOrderFiles;

    public OrderRequestConsumer(Channel channel) {
        super(channel);
    }

    public void init() throws StockControllerException {
        try {
            String[] propertiesNames = {"queueHost", "orderResponseQueueName","orderFiles"};
            Map<String, String> propertiesValues = Util.getProperties(propertiesNames);

            numberOrderFiles = Integer.parseInt(propertiesValues.get("orderFiles"));

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
            String orderId = new String(bytes);
            System.out.println("Receiver request for order: "+orderId);
            Order.OrderStatus status = this.getOrderStatus(orderId);
            this.sendStatus(orderId, status);
            long deliveryTag = envelope.getDeliveryTag();
            getChannel().basicAck(deliveryTag, true);
        } catch (StockControllerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendStatus(String orderId, Order.OrderStatus status) throws StockControllerException {
        try {
            OrderStatusMessage message = new OrderStatusMessage(orderId, status);
            responseChannel.basicPublish("", responseQueueName, null, SerializationUtils.serialize(message));
        } catch (IOException e) {
            throw new StockControllerException("I/O Exception");
        }
    }

    public Order.OrderStatus getOrderStatus (String orderId) throws StockControllerException {
        try {
            // Get the file the order is logged
            Integer orderFileId = Integer.parseInt(orderId) % numberOrderFiles;
            String orderFileName = "Order" + orderFileId;

            File file = new File(orderFileName);
            FileChannel channel = new RandomAccessFile(file, "r").getChannel();
            FileLock lock = channel.lock(0L, Long.MAX_VALUE, true);

            String line;
            BufferedReader br = new BufferedReader(new FileReader(file));

            Boolean found = false;
            Order.OrderStatus status = Order.OrderStatus.NONEXISTENT;
            while (((line = br.readLine()) != null) && !found) {
                String[] orderInfo = line.split(":");
                String orderName = orderInfo[0];
                if (orderName.equals(orderId)) {
                    status = Order.OrderStatus.valueOf(orderInfo[1]);
                    found = true;
                }
            }
            if (lock != null) lock.release();
            channel.close();

            return status;
        } catch (FileNotFoundException e) {
            throw new StockControllerException("The file where the order is logger doesn't exists");
        } catch (IOException e) {
            throw new StockControllerException("I/O Exception");
        }

    }
}
