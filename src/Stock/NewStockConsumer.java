package Stock;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * Created by adrian on 19/09/15.
 */
public class NewStockConsumer extends DefaultConsumer {

    public NewStockConsumer(Channel channel) {
        super(channel);
    }

    public void handleDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) {
        FileWriter fw = null;
        BufferedReader br = null;
        FileChannel channel = null;

        try {
            Stock message = SerializationUtils.deserialize(bytes);
            System.out.println("Updating stock for product: "+message.getProductId()+" in " +message.getProductQty());
            File file = new File("StockFile");
            channel = new RandomAccessFile(file, "rw").getChannel();

            FileLock lock = channel.lock();

            String line;
            String totalStr = "";

            br = new BufferedReader(new FileReader(file));

            String replaceString = "";

            Boolean found = false;
            Integer currentQty = 0;
            while ((line = br.readLine()) != null) {
                String[] productInfo = line.split(":");
                if ((productInfo.length == 2) && !found) {
                    String productName = productInfo[0];
                    if (productName.equals(message.getProductId())) {
                        replaceString = line;
                        currentQty = Integer.parseInt(productInfo[1]);
                        found = true;
                    }
                }
                totalStr += line + "\n";
            }
            br.close();

            if (!found) {
                // Add the new product
                totalStr += (message.getProductId() + ":" + message.getProductQty() + "\n");
            } else {
                // Get the amount of the product and add the new ones
                String newValue = message.getProductId() + ":" + (currentQty + message.getProductQty());
                totalStr = totalStr.replaceAll(replaceString, newValue);
            }

            fw = new FileWriter(file);
            fw.write(totalStr);
            lock.release();

            long deliveryTag = envelope.getDeliveryTag();
            getChannel().basicAck(deliveryTag, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (fw != null) {
                fw.close();
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
