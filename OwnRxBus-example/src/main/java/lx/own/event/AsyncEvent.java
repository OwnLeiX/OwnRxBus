package lx.own.event;

/**
 * <p> </p><br/>
 *
 * @author Lx
 * @date 2017/2/10
 */

public class AsyncEvent {
    public String type = "AsyncEvent:";
    public String message;

    public AsyncEvent(int num) {
        message = "第 " + num + " 个";
    }
}
