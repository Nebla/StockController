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
public class Util {

    public static Map<String, String> getProperties(String[] keys) throws StockControllerException {
        FileLock lock = null;
        FileChannel channel = null;

        try {
            String configFile = "Config/Config.properties";
            File file = new File(configFile);

            channel = new RandomAccessFile(file, "r").getChannel();

            // Use the file channel to create a lock on the file.
            // This method blocks until it can retrieve the lock.
            lock = channel.lock(0L, Long.MAX_VALUE, true);

            HashMap<String, String> returnValue = new HashMap<String, String>();

            Properties prop = new Properties();
            InputStream input = null;

            if (!file.exists()) {
                System.err.println("Config file " + configFile + " missing");
                throw new StockControllerException("Missing configuration file");
            } else {
                input = new FileInputStream(configFile);
                prop.load(input);
            }

            for (String key : keys) {
                returnValue.put(key, prop.getProperty(key));
            }

            // Release the lock - if it is not null!
            if (lock != null) {
                lock.release();
            }

            // Close the file
            channel.close();

            return returnValue;

        } catch (FileNotFoundException e) {
            throw new StockControllerException("Missing configuration file");
        } catch (IOException e) {
            throw new StockControllerException("I/O Exception");
        }
    }
}
