package Log;

import Order.Order;
import com.rabbitmq.client.*;
import org.apache.commons.lang3.SerializationUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by adrian on 17/09/15.
 */
public class OrderAuditoryConsumer extends DefaultConsumer {

    private BufferedWriter bufferWriter;
    private DateFormat dateFormatter;

    public OrderAuditoryConsumer(Channel channel) {
        super(channel);
    }

    public void setFileName(String name) throws IOException {
        File logFile = new File(name);
        FileWriter fileWriter = new FileWriter(logFile.getName(), true);
        bufferWriter = new BufferedWriter(fileWriter);

        dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    }

    public void handleDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {

        Order order = SerializationUtils.deserialize(bytes);

        String orderMessage = "Order Id: " + order.getOrderId() + " - Prodcuct Id: " + order.getProductId() + " Quantity: " + order.getProductQty();
        String stringDate = dateFormatter.format(new Date());
        String logEntrance = stringDate + " " + orderMessage;

        System.out.println("Logging: "+logEntrance);
        bufferWriter.write(logEntrance);
        bufferWriter.newLine();

        bufferWriter.flush();

        long deliveryTag = envelope.getDeliveryTag();
        getChannel().basicAck(deliveryTag, true);

    }

    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
        try {
            bufferWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
