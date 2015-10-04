package Log;

import Error.StockControllerException;
import Util.PropertiesManager;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Created by adrian on 16/09/15.
 */
public class OrderLogger {

    public static void main(String[] argv) {
        try {
            startLogging();
        } catch (StockControllerException e) {
            e.printStackTrace();
        }
    }

    private static void startLogging() throws StockControllerException {
        String[] propertiesName = {"queueHost","auditoryQueueName","orderLogFile","flushInterval"};
        Map<String, String> properties = PropertiesManager.getProperties(propertiesName);
        String queueHost = properties.get("queueHost");
        String queueName = properties.get("auditoryQueueName");
        String logFileName = properties.get("orderLogFile");
        if (logFileName.length() == 0) {
            System.err.println("Log file name key missing, using default value \"orders.log\"");
            logFileName = "order.log";
        }

        File logFile = new File(logFileName);

        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }

            System.out.println("Starting auditory logger");
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(queueHost);
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.queueDeclare(queueName, false, false, false, null);

            OrderAuditoryConsumer consumer = new OrderAuditoryConsumer(channel);
            consumer.setFileName(logFileName);

            channel.basicConsume(queueName, false, consumer);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}