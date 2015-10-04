package Order;

import Error.StockControllerException;
import Util.PropertiesManager;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Map;

/**
 * Created by adrian on 20/09/15.
 */
public class OrderUpdateConsumer extends DefaultConsumer {

    private Integer numberOrderFiles;

    public OrderUpdateConsumer(Channel channel) {
        super(channel);
    }

    public void init () throws StockControllerException {
        String[] propertiesNames = {"orderFiles"};
        Map<String, String> propertiesValues = PropertiesManager.getProperties(propertiesNames);

        numberOrderFiles = Integer.parseInt(propertiesValues.get("orderFiles"));
    }

    public void handleDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) {
        try {
            Order order = SerializationUtils.deserialize(bytes);
            System.out.println("Updating order: "+order.getOrderId());
            this.updateOrder(order);

            long deliveryTag = envelope.getDeliveryTag();
            getChannel().basicAck(deliveryTag, true);

        } catch (StockControllerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateOrder (Order order) throws StockControllerException {
        BufferedReader br = null;
        FileChannel channel = null;
        try {
            // Get the file in which the order is logged
            Integer orderFileId = Integer.parseInt(order.getOrderId()) % numberOrderFiles;
            String orderFileName = "Order" + orderFileId;

            File file = new File(orderFileName);
            channel = new RandomAccessFile(file, "rw").getChannel();
            FileLock lock = channel.lock();

            String line;
            String totalStr = "";

            br = new BufferedReader(new FileReader(file));

            String replaceString = "";

            Boolean found = false;
            String currentStatus = Order.OrderStatus.REJECTED.toString();

            while ((line = br.readLine()) != null) {
                String[] orderInfo = line.split(":");
                String orderName = orderInfo[0];

                if (orderName.equals(order.getOrderId())) {
                    currentStatus = orderInfo[1];
                    replaceString = line;
                    found = true;
                }

                totalStr += line + "\n";
            }

            if (found) {
                if (Order.OrderStatus.ACCEPTED.toString().equals(currentStatus)) {
                    // We just need to updates the Accepted orders.
                    String orderMessage = order.getOrderId() + ":" + order.getOrderStatus();
                    totalStr = totalStr.replaceAll(replaceString, orderMessage);
                }
                FileWriter fw = new FileWriter(file);
                fw.write(totalStr);
                fw.close();
            }

            lock.release();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (br != null) {
                br.close();
            }
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
}
