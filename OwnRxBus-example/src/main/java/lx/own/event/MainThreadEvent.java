package lx.own.event;

/**
 * <p> </p><br/>
 *
 * @author Lx
 * @date 2017/2/10
 */

public class MainThreadEvent {
    public String type = "MainThreadEvent:";
    public String message;

    public MainThreadEvent(int num) {
        message = "第 " + num + " 个";
    }
}
