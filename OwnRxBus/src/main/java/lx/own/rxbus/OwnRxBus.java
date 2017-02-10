package lx.own.rxbus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

/**
 * <p> </p><br/>
 *
 * @author Lx
 * @date 2017/2/6
 */

public class OwnRxBus {

    private static OwnRxBus mInstance;

    private final rx.subjects.Subject<Object, Object> mBus;
    private final Map<Class<?>, Object> mStickyEventMap;

    private OwnRxBus() {
        mBus = new SerializedSubject<>(PublishSubject.create());
        mStickyEventMap = new ConcurrentHashMap<>();
    }

    protected static OwnRxBus $() {
        if (mInstance == null) {
            synchronized (OwnRxBus.class) {
                if (mInstance == null)
                    mInstance = new OwnRxBus();
            }
        }
        return mInstance;
    }

    protected static void destroy() {
        if (mInstance == null)
            return;
        mInstance.mBus.onCompleted();
        mInstance.removeAllStickyEvents();
        mInstance = null;
    }

    protected static void reset() {
        if (mInstance == null)
            return;
        mInstance.mBus.onCompleted();
        mInstance.removeAllStickyEvents();
    }

    /**
     * 发送事件
     */
    protected void post(Object event) {
        mBus.onNext(event);
    }

    /**
     * 发送Sticky事件
     */
    public synchronized void postSticky(Object event) {
        mStickyEventMap.put(event.getClass(), event);
        post(event);
    }

    /**
     * 根据传递的 eventType 类型返回特定类型(eventType)的 Observable
     */
    protected <T> Observable<T> toObservable(final Class<T> eventType) {
        return mBus.ofType(eventType);
    }

    /**
     * 根据传递的 eventType 类型返回特定类型(eventType)的 Observable
     */
    public synchronized <T> Observable<T> toObservableSticky(final Class<T> eventType) {
        Observable<T> observable = mBus.ofType(eventType);
        final Object event = mStickyEventMap.get(eventType);

        if (event != null) {
            return observable.mergeWith(Observable.create(new Observable.OnSubscribe<T>() {
                @Override
                public void call(Subscriber<? super T> subscriber) {
                    subscriber.onNext(eventType.cast(event));
                }
            }));
        } else {
            return observable;
        }
    }

    /**
     * 返回全类型的Observable
     */
    protected Observable<Object> toObservable() {
        return mBus;
    }

    /**
     * 判断是否有订阅者
     */
    public boolean hasObservers() {
        return mBus.hasObservers();
    }

    /**
     * 根据eventType获取Sticky事件
     */
    public synchronized <T> T getStickyEvent(Class<T> eventType) {
        return eventType.cast(mStickyEventMap.get(eventType));
    }

    /**
     * 移除指定eventType的Sticky事件
     */
    public synchronized <T> T removeStickyEvent(Class<T> eventType) {
        return eventType.cast(mStickyEventMap.remove(eventType));
    }

    /**
     * 移除所有的Sticky事件
     */
    public synchronized void removeAllStickyEvents() {
        mStickyEventMap.clear();
    }

}
