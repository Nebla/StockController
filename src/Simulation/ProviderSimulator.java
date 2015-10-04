package Simulation;

import Error.StockControllerException;
import Stock.Stock;
import Util.PropertiesManager;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Created by adrian on 16/09/15.
 */
public class ProviderSimulator {

    public static void main(String[] args) throws IOException, TimeoutException, StockControllerException {
        String[] propertiesName = {"queueHost","stockQueueName"};
        Map<String, String> queueNames = PropertiesManager.getProperties(propertiesName);
        String queueHost = queueNames.get("queueHost");
        String queueName = queueNames.get("stockQueueName");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(queueHost);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(queueName, false, false, false, null);

        String[] simulationPropertiesName = {"stockProviderInterval","stockProviderIterations"};
        Map<String, String> simulationValues = PropertiesManager.getSimulationProperties(simulationPropertiesName);
        Integer providerInterval = Integer.parseInt(simulationValues.get("stockProviderInterval"));
        Integer providerIterations = Integer.parseInt(simulationValues.get("stockProviderIterations"));

        for (int i=0; i < providerIterations; ++i) {
            String productId = RandomStringUtils.randomAlphabetic(1).toUpperCase();

            System.out.println("Adding stock for product " + productId);

            Stock stockMessage = new Stock(productId, 10);
            channel.basicPublish("", queueName, null, SerializationUtils.serialize(stockMessage));

            try {
                Thread.sleep(providerInterval*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        channel.close();
        connection.close();
        System.out.println("Finishing provider simulator");
        System.exit(0);
    }
}
