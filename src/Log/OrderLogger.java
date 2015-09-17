package Log;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import Error.StockControllerException;
import com.rabbitmq.client.*;

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

        Properties prop = new Properties();
        InputStream input = null;

        String configFile = "Config/Config.properties";
        File f = new File(configFile);

        if (!f.exists()) {
            System.err.println("Config file " + configFile + " missing");
            throw new StockControllerException("Missing configuration file");
        } else {
            try {
                input = new FileInputStream(configFile);
                prop.load(input);
            } catch (FileNotFoundException e) {
                // The code should never reach this statement as we are evaluating file existance.
                throw new StockControllerException("Missing configuration file");
            } catch (IOException e) {
                e.printStackTrace();
                throw new StockControllerException("I/O Exception");
            }
        }

        String logFileName = prop.getProperty("orderLogFile");
        if (logFileName.length() == 0) {
            System.err.println("Log file name key missing, using default value \"orders.log\"");
            logFileName = "order.log";
        }

        Integer flushInteval = Integer.parseInt(prop.getProperty("flushInterval"));
        if (flushInteval == 0) {
            flushInteval = 5;
        }

        String queueHost = prop.getProperty("queueHost");
        String queueName = prop.getProperty("auditoryQueueName");
        if (queueHost.length() == 0 || queueName.length() == 0) {
            System.err.println("Auditory params missing from Configuration file");
            throw new StockControllerException("Communication queues can not be created");
        }

        File logFile = new File(logFileName);

        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }

            FileWriter fileWriter = new FileWriter(logFile.getName(), true);
            BufferedWriter bufferWriter = new BufferedWriter(fileWriter);

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(queueHost);
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.queueDeclare(queueName, false, false, false, null);

            OrderAuditoryConsumer consumer = new OrderAuditoryConsumer(channel);
            consumer.setFileParams(logFileName, flushInteval);

            boolean autoAck = false;
            channel.basicConsume(queueName, autoAck, consumer);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}

