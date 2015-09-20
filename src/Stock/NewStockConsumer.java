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

        try {
            // Get a file channel for the file
            File file = new File("StockFile");
            FileChannel channel = new RandomAccessFile(file, "rw").getChannel();

            // Use the file channel to create a lock on the file.
            // This method blocks until it can retrieve the lock.
            FileLock lock = channel.lock();

            FileReader fr = new FileReader(file);

            String line;
            String totalStr = "";

            BufferedReader br = new BufferedReader(fr);

            String replaceString = "";
            NewStock message = SerializationUtils.deserialize(bytes);

            Boolean found = false;
            Integer currentQty = 0;
            while ((line = br.readLine()) != null) {
                String[] productInfo = line.split(":");
                if ((productInfo.length == 2) && !found) {
                    String productName = productInfo[0];
                    if (productName.equals(message.getProductName())) {
                        replaceString = line;
                        currentQty = Integer.parseInt(productInfo[1]);
                        found = true;
                    }
                }
                totalStr += line + "\n";
            }

            if (!found) {
                // Add the new product
                totalStr += (message.getProductName() + ":" + message.getProductQty() + "\n");
            } else {
                // Get the amount of the product and add the new ones
                String newValue = message.getProductName() + ":" + (currentQty + message.getProductQty());
                totalStr = totalStr.replaceAll(replaceString, newValue);
            }

            FileWriter fw = new FileWriter(file);
            fw.write(totalStr);
            fw.close();

            // Release the lock - if it is not null!
            if (lock != null) {
                lock.release();
            }

            // Close the file
            channel.close();

            long deliveryTag = envelope.getDeliveryTag();
            getChannel().basicAck(deliveryTag, true);
        }  catch (IOException e) {
            e.printStackTrace();
        }
    }
}