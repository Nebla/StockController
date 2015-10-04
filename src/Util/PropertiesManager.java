package Util;

import Error.StockControllerException;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by adrian on 19/09/15.
 */
public class PropertiesManager {

    public static Map<String, String> getProperties(String filename, String[] keys) throws StockControllerException {
        FileLock lock = null;
        FileChannel channel = null;
        try {
            File file = new File(filename);
            channel = new RandomAccessFile(file, "r").getChannel();
            lock = channel.lock(0L, Long.MAX_VALUE, true);
            HashMap<String, String> returnValue = new HashMap<String, String>();
            Properties prop = new Properties();
            InputStream input = null;

            if (!file.exists()) {
                System.err.println("Config file " + filename + " missing");
		        lock.release();
            	channel.close();
		        throw new StockControllerException("Missing configuration file");
            } else {
                input = new FileInputStream(filename);
                prop.load(input);
            }

            for (String key : keys) {
                returnValue.put(key, prop.getProperty(key));
            }

            lock.release();
            channel.close();
            return returnValue;

        } catch (FileNotFoundException e) {
            throw new StockControllerException("Missing configuration file");
        } catch (IOException e) {
            throw new StockControllerException("I/O Exception");
        }
    }

    public static Map<String, String> getProperties(String[] keys) throws StockControllerException {
        return getProperties("Config/Config.properties", keys);
    }

    public static Map<String, String> getSimulationProperties(String[] keys) throws StockControllerException {
        return getProperties("Config/Simulation.properties", keys);
    }
}
