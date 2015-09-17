package Log;

import com.rabbitmq.client.*;

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
    private Integer flushInterval;
    private Date lastFlush;
    private DateFormat dateFormatter;

    public OrderAuditoryConsumer(Channel channel) {
        super(channel);
    }

    public void setFileParams(String name, Integer interval) throws IOException {

        flushInterval = interval;

        File logFile = new File(name);
        FileWriter fileWriter = new FileWriter(logFile.getName(), true);
        bufferWriter = new BufferedWriter(fileWriter);

        dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        lastFlush = new Date();
    }

    public void handleDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
        String orderId = new String(bytes, "UTF-8");
        String stringDate = dateFormatter.format(new Date());
        String logEntrance = stringDate + " - " + orderId;
        bufferWriter.write(logEntrance);
        bufferWriter.newLine();

        if (((new Date().getTime() - lastFlush.getTime()) / 1000) > flushInterval) {
            bufferWriter.flush();
            lastFlush = new Date();
        }

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
