package Order;

import Stock.NewStock;
import Util.Util;
import Error.StockControllerException;

import com.rabbitmq.client.*;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Created by adrian on 20/09/15.
 */
public class NewOrderConsumer extends DefaultConsumer {

    private Channel auditoryChannel;
    private String auditoryQueueName;

    public NewOrderConsumer(Channel channel) {
        super(channel);
    }

    public void init() throws StockControllerException {
        try {
            String[] propertiesNames = {"queueHost", "auditoryQueueName"};
            Map<String, String> propertiesValues = Util.getProperties(propertiesNames);

            String queueHost = propertiesValues.get("queueHost");
            auditoryQueueName = propertiesValues.get("auditoryQueueName");

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(queueHost);
            Connection connection = factory.newConnection();
            auditoryChannel = connection.createChannel();
            auditoryChannel.queueDeclare(auditoryQueueName, false, false, false, null);

        } catch (TimeoutException e) {
            throw new StockControllerException("There was a timeout while trying to connect to queue server");
        } catch (IOException e) {
            throw new StockControllerException("There was a problem while trying to connect to the I/O device");
        }
    }

    public void sendAuditory(Order order) throws IOException {
        // Here we send a formatted message so the auditory log can register
        String message = order.getOrderId() + " - " + order.getProductId() + ":" + order.getProductQty();
        auditoryChannel.basicPublish("", auditoryQueueName, null, message.getBytes());
    }

    public Boolean checkStock(Order newOrder) throws IOException {
        // Check in the stock file if there is enough stock
        File file = new File("StockFile");
        FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
        FileLock lock = channel.lock();
        FileReader fr = new FileReader(file);

        BufferedReader br = new BufferedReader(fr);
        String totalStr = "";
        String replaceString = "";
        String newString = "";

        Boolean shouldUpdate = false;
        String line;
        while ((line = br.readLine()) != null) {
            String[] productInfo = line.split(":");
            String productName = productInfo[0];
            if (productName.equals(newOrder.getProductId())) {
                // Check available stock
                Integer currentQty = Integer.parseInt(productInfo[1]);
                if (currentQty >= newOrder.getProductQty()) {
                    shouldUpdate = true;
                    replaceString = line;
                    newString = newOrder.getProductId() + ":" + (currentQty - newOrder.getProductQty());
                }
                else {
                    // There isn't enough stock, we leave stock file as it is and break the cycle
                    break;
                }
            }
            totalStr += line + "\n";
        }

        if (shouldUpdate) {
            totalStr = totalStr.replaceAll(replaceString, newString);
            FileWriter fw = new FileWriter(file);
            fw.write(totalStr);
            fw.close();
        }

        if (lock != null) lock.release();
        channel.close();

        return shouldUpdate;
    }

    public void saveOrderStatus(Order newOrder) throws IOException {
        Integer orderFileId = Integer.parseInt(newOrder.getOrderId())%10;
        String orderFileName = "Order" + orderFileId;

        File file = new File(orderFileName);
        FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
        FileLock lock = channel.lock();

        // We just need to append the new order status, as it's a new one
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
        String orderMessage = newOrder.getOrderId() + ":" + newOrder.getOrderStatus();
        out.println(orderMessage);
        out.close();

        if (lock != null) lock.release();
        channel.close();
    }

    public void handleDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) {
        try {
            Order newOrder = SerializationUtils.deserialize(bytes);

            // Send the order to auditory
            this.sendAuditory(newOrder);

            // Check stock availability
            Boolean availableStock = this.checkStock(newOrder);
            Order.OrderStatus status = (availableStock) ? Order.OrderStatus.ACCEPTED : Order.OrderStatus.REJECTED;
            newOrder.updateStatus(status);

            // Save the order status
            this.saveOrderStatus(newOrder);

            long deliveryTag = envelope.getDeliveryTag();
            getChannel().basicAck(deliveryTag, true);
        }  catch (IOException e) {
            e.printStackTrace();
        }
    }
}
