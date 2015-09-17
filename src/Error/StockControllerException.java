package Error;

/**
 * Created by adrian on 16/09/15.
 */
public class StockControllerException extends Exception
{
    public StockControllerException() {}

    public StockControllerException(String message) {
        super(message);
    }
}
