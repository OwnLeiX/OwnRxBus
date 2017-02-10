package lx.own.event;

/**
 * <p> </p><br/>
 *
 * @author Lx
 * @date 2017/2/10
 */

public class NewThreadEvent {
    public String type = "NewThreadEvent";
    public String message;
    public NewThreadEvent(int num) {
        message = "第 " + num + " 个";
    }
}
