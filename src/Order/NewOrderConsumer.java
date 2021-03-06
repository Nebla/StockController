package Order;

import Error.StockControllerException;
import Util.PropertiesManager;
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
public class NewOrderConsumer extends DefaultConsumer {

    private Channel auditoryChannel;
    private String auditoryQueueName;
    private Integer numberOrderFiles;

    public NewOrderConsumer(Channel channel) {
        super(channel);
    }

    public void init() throws StockControllerException {
        try {
            String[] propertiesNames = {"queueHost", "auditoryQueueName", "orderFiles"};
            Map<String, String> propertiesValues = PropertiesManager.getProperties(propertiesNames);

            numberOrderFiles = Integer.parseInt(propertiesValues.get("orderFiles"));

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
        auditoryChannel.basicPublish("", auditoryQueueName, null, SerializationUtils.serialize(order));
    }

    public Boolean checkStock(Order newOrder) {
        // Check in the stock file if there is enough stock
        FileChannel channel = null;
        Boolean shouldUpdate = false;
        try {
            File file = new File("StockFile");
            channel = new RandomAccessFile(file, "rw").getChannel();
            FileLock lock = channel.lock();
            FileReader fr = new FileReader(file);

            BufferedReader br = new BufferedReader(fr);
            String totalStr = "";
            String replaceString = "";
            String newString = "";

            Boolean enoughStock = true;
            String line;
            while (((line = br.readLine()) != null) && enoughStock) {
                String[] productInfo = line.split(":");
                String productName = productInfo[0];
                if (productName.equals(newOrder.getProductId())) {
                    // Check available stock
                    Integer currentQty = Integer.parseInt(productInfo[1]);
                    if (currentQty >= newOrder.getProductQty()) {
                        shouldUpdate = true;
                        replaceString = line;
                        newString = newOrder.getProductId() + ":" + (currentQty - newOrder.getProductQty());
                    } else {
                        // There isn't enough stock, we leave stock file as it is and break the cycle
                        enoughStock = false;
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

            lock.release();

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (channel != null) {
                channel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return shouldUpdate;
    }

    public void saveOrderStatus(Order newOrder) {
        FileChannel channel = null;
        try {
            // Get the file the order is logged
            Integer orderFileId = Integer.parseInt(newOrder.getOrderId()) % numberOrderFiles;
            String orderFileName = "Order" + orderFileId;

            File file = new File(orderFileName);
            channel = new RandomAccessFile(file, "rw").getChannel();
            FileLock lock = channel.lock();

            // We just need to append the new order status, as it's a new one
            BufferedWriter bf = new BufferedWriter(new FileWriter(file, true));
            String orderMessage = newOrder.getOrderId() + ":" + newOrder.getOrderStatus() + System.lineSeparator();
            bf.write(orderMessage);
            bf.close();

            lock.release();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (channel != null) {
                channel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) {
        try {
            Order newOrder = SerializationUtils.deserialize(bytes);

            System.out.println("Received order: "+newOrder.getOrderId());
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
