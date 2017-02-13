package lx.own.rxbus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * <p> </p><br/>
 *
 * @author Lx
 * @date 2017/2/6
 */

public class OwnBus {
    public static class BusRoute {
        public static final int error = -1;
        public static final int usual = 1;
        public static final int main = 1 << 1;
        public static final int async = 1 << 2;
        public static final int io = 1 << 3;
    }

    private Map<String, List<Subscription>> mSubscriptions;
    private static OwnBus mInstance;

    /**
     * @return 获取OwnBusManager实例
     */
    public static OwnBus $() {
        if (mInstance == null) {
            synchronized (OwnBus.class) {
                if (mInstance == null)
                    mInstance = new OwnBus();
            }
        }
        return mInstance;
    }

    private OwnBus() {
        mSubscriptions = new ConcurrentHashMap<>(4);
    }


    private <T> Observable<T> setScheduler(Observable<T> observable, int scheduler) {
        switch (scheduler) {
            case BusRoute.usual:
                return observable;
            case BusRoute.main:
                return observable.observeOn(AndroidSchedulers.mainThread());
            case BusRoute.async:
                return observable.observeOn(Schedulers.newThread());
            case BusRoute.io:
                return observable.observeOn(Schedulers.io());
            default:
                throw new IllegalArgumentException("If you want use the scheduler, Please use the arguments in BusRoute.class . ");
        }
    }

    private Subscription add(Subscription subscription, String tag) {
        if (subscription.isUnsubscribed())
            return subscription;
        List<Subscription> subList = mSubscriptions.get(tag);
        if (subList == null) {
            subList = new ArrayList<>();
            mSubscriptions.put(tag, subList);
        }
        subList.add(subscription);
        return subscription;
    }

    public synchronized void take(Object event) {
        OwnRxCore.$().post(event);
    }

    /**
     * <p>上车的方法</p><br/>
     *
     * @param tag              作为Station的标记
     * @param station 回调
     * @return 取消单个站台时所需的Subscription
     */
    public Subscription newStation(String tag, OwnBusStation<Object> station) {
        return newStation(tag, Object.class, station);
    }

    /**
     * <p>上车的方法</p><br/>
     *
     * @param tag              作为Station的标记
     * @param station   回调
     * @param scheduler 想要在什么线程接收回调
     * @return 取消单个站台时所需的Subscription
     */
    public Subscription newStation(String tag, OwnBusStation<Object> station, int scheduler) {
        return newStation(tag, Object.class, station, scheduler);
    }

    /**
     * <p>上车的方法</p><br/>
     *
     * @param tag              作为Station的标记
     * @param eventType 关心的事件类型
     * @param station   回调
     * @return 取消单个站台时所需的Subscription
     */
    public <T> Subscription newStation(String tag, Class<T> eventType, OwnBusStation<T> station) {
        return newStation(tag, eventType, station, BusRoute.usual);
    }

    /**
     * <p>上车的方法</p><br/>
     *
     * @param tag              作为Station的标记
     * @param eventType 关心的事件类型
     * @param station   回调
     * @param scheduler 想要在什么线程接收回调
     * @return 取消单个站台时所需的Subscription
     */
    public <T> Subscription newStation(String tag, Class<T> eventType, OwnBusStation<T> station, int scheduler) {
        return newStation(tag, eventType, station, null, scheduler);
    }

    /**
     * <p>上车的方法</p><br/>
     *
     * @param tag              作为Station的标记
     * @param station          回调
     * @param accidentReceiver 不可预估的错误信息的回调
     * @return 取消单个站台时所需的Subscription
     */
    public <T> Subscription newStation(String tag, OwnBusStation<Object> station, OwnAccident accidentReceiver) {
        return newStation(tag, Object.class, station, accidentReceiver);
    }

    /**
     * <p>上车的方法</p><br/>
     *
     * @param tag              作为Station的标记
     * @param station          回调
     * @param accidentReceiver 不可预估的错误信息的回调
     * @param scheduler        想要在什么线程接收回调
     * @return 取消单个站台时所需的Subscription
     */
    public <T> Subscription newStation(String tag, OwnBusStation<Object> station, OwnAccident accidentReceiver, int scheduler) {
        return newStation(tag, Object.class, station, accidentReceiver, scheduler);
    }

    /**
     * <p>上车的方法</p><br/>
     *
     * @param tag              作为Station的标记
     * @param eventType        关心的事件类型
     * @param station          回调
     * @param accidentReceiver 不可预估的错误信息的回调
     * @return 取消单个站台时所需的Subscription
     */
    public <T> Subscription newStation(String tag, Class<T> eventType, OwnBusStation<T> station, OwnAccident accidentReceiver) {
        return newStation(tag, eventType, station, accidentReceiver, BusRoute.usual);
    }

    /**
     * <p>上车的方法</p><br/>
     *
     * @param tag              作为Station的标记
     * @param eventType        关心的事件类型
     * @param station          回调
     * @param accidentReceiver 不可预估的错误信息的回调
     * @param scheduler        想要在什么线程接收回调
     * @return 取消单个站台时所需的Subscription
     */
    public <T> Subscription newStation(String tag, Class<T> eventType, OwnBusStation<T> station, OwnAccident accidentReceiver, int scheduler) {
        checkNull(tag, eventType, station);
        return add(setScheduler(OwnRxCore.$()
                .toObservable(eventType)
                .onBackpressureBuffer(), scheduler)
                .subscribe(getSubscriber(tag, eventType, station, accidentReceiver, scheduler)), tag);
    }

    private <T> void checkNull(Object tag, Class<T> eventType, OwnBusStation<T> station) {
        if (tag == null)
            throw new IllegalArgumentException("Tag can not be null !");
        if (eventType == null)
            throw new IllegalArgumentException("EventType can not be null !");
        if (station == null)
            throw new IllegalArgumentException("Station can not be null !");
    }

    private <T> Subscription restoreStation(CatchObserver<T> observer) {
        return newStation(observer.mTag, observer.mEventType, observer.mStation, observer.mAccidentReceiver, observer.mScheduler);
    }

    private <T> CatchObserver<T> getSubscriber(String tag, Class<T> eventType, OwnBusStation<T> station, OwnAccident accidentReceiver, int scheduler) {
        return new CatchObserver.Builder<T>()
                .station(station)
                .receiver(accidentReceiver)
                .type(eventType)
                .tag(tag)
                .scheduler(scheduler)
                .create();
    }

    /**
     * <p>单人下车的方法</p><br/>
     *
     * @param tag          建立Station时传入的tag
     * @param subscription 建立时候返回的Subscription
     */
    public OwnBus abandonStation(String tag, Subscription subscription) {
        if (tag == null)
            return this;
        synchronized (OwnBus.class) {
            List<Subscription> subList = mSubscriptions.get(tag);
            if (subList == null)
                return this;
            if (subList.remove(subscription)) {
                if (!subscription.isUnsubscribed())
                    subscription.unsubscribe();
            }
        }
        return this;
    }

    /**
     * <p>一起下车的方法</p><br/>
     *
     * @param tag 建立Station时传入的tag
     */
    public OwnBus abandonStations(String tag) {
        if (tag == null)
            return this;
        synchronized (OwnBus.class) {
            List<Subscription> subList = mSubscriptions.get(tag);
            if (subList == null)
                return this;
            for (Subscription subscription : subList) {
                if (!subscription.isUnsubscribed())
                    subscription.unsubscribe();
            }
            subList.clear();
            mSubscriptions.remove(tag);

            return this;
        }
    }

    public void reset() {
        for (List<Subscription> subscriptions : mSubscriptions.values()) {
            for (Subscription subscription : subscriptions) {
                if (!subscription.isUnsubscribed())
                    subscription.unsubscribe();
            }
            subscriptions.clear();
        }
        mSubscriptions.clear();
    }

    public static class CatchObserver<T> extends Subscriber<T> {

        private OwnBusStation<T> mStation;
        private OwnAccident mAccidentReceiver;
        private String mTag;
        private Class<T> mEventType;
        private int mScheduler;

        public CatchObserver(OwnBusStation<T> station, OwnAccident receiver, String tag, Class<T> eventType, int scheduler) {
            this.mStation = station;
            this.mAccidentReceiver = receiver;
            this.mTag = tag;
            this.mEventType = eventType;
            this.mScheduler = scheduler;
        }

        @Override
        public void onStart() {
            request(1);
        }

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
//            e.printStackTrace();
            if (mAccidentReceiver != null)
                mAccidentReceiver.onAccident(e);
            OwnBus.$().restoreStation(this);
        }

        @Override
        public void onNext(T t) {
            try {
                mStation.onBusStop(t);
            } catch (Exception e) {
                e.printStackTrace();
            }
            request(1);
        }

        public static class Builder<T> {
            private OwnBusStation<T> mStation;
            private OwnAccident mAccidentReceiver;
            private String mTag;
            private Class<T> mClass;
            private int mScheduler = -1;

            public Builder() {

            }

            public Builder station(OwnBusStation station) {
                this.mStation = station;
                return this;
            }

            public Builder receiver(OwnAccident receiver) {
                this.mAccidentReceiver = receiver;
                return this;
            }

            public Builder tag(String tag) {
                this.mTag = tag;
                return this;
            }

            public Builder type(Class<T> eventType) {
                this.mClass = eventType;
                return this;
            }

            public Builder scheduler(int scheduler) {
                this.mScheduler = scheduler;
                return this;
            }

            public CatchObserver<T> create() {
                if (mTag == null)
                    throw new IllegalArgumentException("you must call the Builder.tag() before Builder.create() !");
                if (mStation == null)
                    throw new IllegalArgumentException("you must call the Builder.station() before Builder.create() !");
                if (mClass == null)
                    throw new IllegalArgumentException("you must call the Builder.type() before Builder.create() !");
                if (mScheduler == BusRoute.error)
                    throw new IllegalArgumentException("you must call the Builder.scheduler() before Builder.create() !");
                return new CatchObserver<>(mStation, mAccidentReceiver, mTag, mClass, mScheduler);
            }


        }
    }

}
