package lx.own.rxbus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * <p> </p><br/>
 *
 * @author Lx
 * @date 2017/2/6
 */

public class OwnBusManager {
    public static class OwnScheduler {
        public static final int error = -1;
        public static final int usual = 1;
        public static final int main = 1 << 1;
        public static final int async = 1 << 2;
        public static final int io = 1 << 3;
    }

    private Map<String, List<Subscription>> mSubscriptions;
    private static OwnBusManager mInstance;

    public static OwnBusManager $() {
        if (mInstance == null) {
            synchronized (OwnBusManager.class) {
                if (mInstance == null)
                    mInstance = new OwnBusManager();
            }
        }
        return mInstance;
    }

    private OwnBusManager() {
        mSubscriptions = new ConcurrentHashMap<>(4);
    }


    private <T> Observable<T> setScheduler(Observable<T> observable, int scheduler) {
        switch (scheduler) {
            case OwnScheduler.usual:
                return observable;
            case OwnScheduler.main:
                return observable.observeOn(AndroidSchedulers.mainThread());
            case OwnScheduler.async:
                return observable.observeOn(Schedulers.newThread());
            case OwnScheduler.io:
                return observable.observeOn(Schedulers.io());
            default:
                throw new IllegalArgumentException("If you want use the scheduler, Please use the arguments in OwnScheduler.class . ");
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

    public synchronized void post(Object event) {
        OwnRxBus.$().post(event);
    }

    public Subscription subscribe(String tag, OwnBusStation<Object> station) {
        return subscribe(tag, Object.class, station);
    }

    public Subscription subscribe(String tag, OwnBusStation<Object> station, int scheduler) {
        return subscribe(tag, Object.class, station, scheduler);
    }

    public <T> Subscription subscribe(String tag, Class<T> eventType, OwnBusStation<T> station) {
        return subscribe(tag, eventType, station, OwnScheduler.usual);
    }

    public <T> Subscription subscribe(String tag, Class<T> eventType, OwnBusStation<T> station, int scheduler) {
        return subscribe(tag,eventType,station,null,scheduler);
    }


    public <T> Subscription subscribe(String tag, OwnBusStation<Object> station, OwnBusAccident accidentReceiver) {
        return subscribe(tag, Object.class, station, accidentReceiver);
    }

    public <T> Subscription subscribe(String tag, OwnBusStation<Object> station, OwnBusAccident accidentReceiver, int scheduler) {
        return subscribe(tag, Object.class, station, accidentReceiver, scheduler);
    }

    public <T> Subscription subscribe(String tag, Class<T> eventType, OwnBusStation<T> station, OwnBusAccident accidentReceiver) {
        return subscribe(tag, eventType, station, accidentReceiver, OwnScheduler.usual);
    }

    public <T> Subscription subscribe(String tag, Class<T> eventType, OwnBusStation<T> station, OwnBusAccident accidentReceiver, int scheduler) {
        checkNull(tag,eventType,station);
        return add(setScheduler(OwnRxBus.$().toObservable(eventType), scheduler)
                .subscribe(getObserver(tag,eventType,station,accidentReceiver,scheduler)), tag);
    }

    private <T> void checkNull(String tag, Class<T> eventType, OwnBusStation<T> station) {
        if (tag == null)
            throw new IllegalArgumentException("Tag can not be null !");
        if (eventType == null)
            throw new IllegalArgumentException("EventType can not be null !");
        if (station == null)
            throw new IllegalArgumentException("Station can not be null !");
    }

    public <T> Subscription subscribe(CatchObserver<T> observer) {
        return subscribe(observer.mTag,observer.mEventType,observer.mStation,observer.mAccidentReceiver,observer.mScheduler);
    }

    private <T>CatchObserver<T> getObserver(String tag, Class<T> eventType, OwnBusStation<T> station, OwnBusAccident accidentReceiver, int scheduler) {
        return new CatchObserver.Builder<T>()
                .station(station)
                .receiver(accidentReceiver)
                .type(eventType)
                .tag(tag)
                .scheduler(scheduler)
                .create();
    }

    public void unsubscribeSingle(String tag, Subscription key) {
        if (tag == null)
            return;
        List<Subscription> subList = mSubscriptions.get(tag);
        if (subList == null)
            return;
        subList.remove(key);
        if (!key.isUnsubscribed())
            key.unsubscribe();
    }

    public OwnBusManager unsubscribe(String tag) {
        if (tag == null)
            return this;
        synchronized (OwnBusManager.class) {
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

    /**
     * 因为RxJava在onComplete()或者onError()后会自动unsubscribe()，所以强行try-catch异常，防止事件订阅被取消。
     * 在爆发性事件发生时候try catch异常，并且重新订阅自己，进行修复。
     */
    public static class CatchObserver<T> implements Observer<T> {

        private OwnBusStation<T> mStation;
        private OwnBusAccident mAccidentReceiver;
        private String mTag;
        private Class<T> mEventType;
        private int mScheduler;

        public CatchObserver(OwnBusStation<T> station, OwnBusAccident receiver, String tag, Class<T> eventType, int scheduler) {
            this.mStation = station;
            this.mAccidentReceiver = receiver;
            this.mTag = tag;
            this.mEventType = eventType;
            this.mScheduler = scheduler;
        }

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
//            e.printStackTrace();
            if (mAccidentReceiver != null)
                mAccidentReceiver.onBusBreakDown(e);
            OwnBusManager.$().subscribe(this);
        }

        @Override
        public void onNext(T t) {
            try {
                mStation.onBusStop(t);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static class Builder<T>{
            private OwnBusStation<T> mStation;
            private OwnBusAccident mAccidentReceiver;
            private String mTag;
            private Class<T> mClass;
            private int mScheduler = -1;

            public Builder(){

            }

            public Builder station(OwnBusStation station) {
                this.mStation = station;
                return this;
            }

            public Builder receiver(OwnBusAccident receiver) {
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

            public CatchObserver<T> create(){
                if (mTag == null)
                    throw new IllegalArgumentException("you must call the Builder.tag() before Builder.create() !");
                if (mStation == null)
                    throw new IllegalArgumentException("you must call the Builder.station() before Builder.create() !");
                if (mClass == null)
                    throw new IllegalArgumentException("you must call the Builder.type() before Builder.create() !");
                if (mScheduler == OwnScheduler.error)
                    throw new IllegalArgumentException("you must call the Builder.scheduler() before Builder.create() !");
                return new CatchObserver<T>(mStation,mAccidentReceiver,mTag,mClass,mScheduler);
            }



        }
    }

}
