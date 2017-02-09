package lx.own.rxbus;

/**
 * <p> </p><br/>
 *
 * @author Lx
 * @date 2017/2/9
 */

public interface OwnBusStation<T> {
    void onBusStop(T event);
}
