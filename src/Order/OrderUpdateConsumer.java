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
public class OrderUpdateConsumer extends DefaultConsumer {

    private Integer numberOrderFiles;

    public OrderUpdateConsumer(Channel channel) {
        super(channel);
    }

    public void init () throws StockControllerException {
        String[] propertiesNames = {"orderFiles"};
        Map<String, String> propertiesValues = Util.getProperties(propertiesNames);

        numberOrderFiles = Integer.parseInt(propertiesValues.get("orderFiles"));
    }

    public void handleDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) {
        try {
            Order order = SerializationUtils.deserialize(bytes);
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
        try {
            // Get the file in which the order is logged
            Integer orderFileId = Integer.parseInt(order.getOrderId()) % numberOrderFiles;
            String orderFileName = "Order" + orderFileId;

            File file = new File(orderFileName);
            FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
            FileLock lock = channel.lock();

            FileReader fr = new FileReader(file);

            String line;
            String totalStr = "";

            BufferedReader br = new BufferedReader(fr);

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

            if (!found) {
                // We are trying to update a nonexistent order
                throw new StockControllerException("The order being updated does not exists in the system");
            } else {
                if (Order.OrderStatus.ACCEPTED.toString().equals(currentStatus)) {
                    // We just neec to updates the Accepted orders.
                    String orderMessage = order.getOrderId() + ":" + order.getOrderStatus();
                    totalStr = totalStr.replaceAll(replaceString, orderMessage);
                }
            }

            FileWriter fw = new FileWriter(file);
            fw.write(totalStr);
            fw.close();

            if (lock != null) lock.release();
            channel.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
