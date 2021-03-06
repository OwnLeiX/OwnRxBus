package lx.own;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import lx.own.event.MainThreadEvent;
import lx.own.event.NewThreadEvent;
import lx.own.rxbus.OwnAccident;
import lx.own.rxbus.OwnBus;
import lx.own.rxbus.OwnBusStation;

/**
 * 请使用Log查看结果
 * 注意在子线程请尽量不要爆发性的发送事件，如果如此做，会导致除了OwnScheduler.usual之外的Station崩溃，在爆发结束后重新绑定
 * 在UIThread爆发性的发送事件也会导致OwnScheduler.main的Station崩溃。
 *
 * */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "Bus";

    private static final String TAG_USUAL = "usual";
    private static final String TAG_ASYNC = "async";
    private static final String TAG_IO = "io";
    private static final String TAG_MAIN = "main";

    private Button btn_mainThread, btn_mainThread50, btn_newThread, btn_newThread50;
    private TextView tv_usualResult, tv_ioResult, tv_mainResult, tv_asyncResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onContentChanged() {
        bindView();
        initListener();
        initBus();
    }

    /**
     * 在不需要的时候使用此方法解绑
     * */
    @Override
    protected void onDestroy() {
        OwnBus.$().abandonStations(TAG);
        super.onDestroy();
    }

    /**
     * 在需要的时候订阅
     * 具体订阅方法请查看分支方法
     * */
    private void initBus() {
        //在发送Event的线程回调
        initUsualBus();

        //在新线程回调
        initAsyncStation();

        //在io线程回调
        initIOStation();

        //在主线程回调
        initMainStation();

    }

    private void initListener() {
        btn_mainThread.setOnClickListener(this);
        btn_mainThread50.setOnClickListener(this);
        btn_newThread.setOnClickListener(this);
        btn_newThread50.setOnClickListener(this);
    }

    private void bindView() {
        btn_mainThread = (Button) findViewById(R.id.btn_mainThread);
        btn_mainThread50 = (Button) findViewById(R.id.btn_mainThread50);
        btn_newThread = (Button) findViewById(R.id.btn_newThread);
        btn_newThread50 = (Button) findViewById(R.id.btn_newThread50);
        tv_usualResult = (TextView) findViewById(R.id.tv_usualResult);
        tv_ioResult = (TextView) findViewById(R.id.tv_ioResult);
        tv_mainResult = (TextView) findViewById(R.id.tv_mainResult);
        tv_asyncResult = (TextView) findViewById(R.id.tv_asyncResult);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_mainThread:
                sendEventAtMain();
                break;
            case R.id.btn_newThread:
                sendEventAtNew();
                break;
            case R.id.btn_mainThread50:
                sendEventAtMain50();
                break;
            case R.id.btn_newThread50:
                sendEventAtNew50();
                break;
        }
    }

    private void sendEventAtNew() {
        new Thread() {
            @Override
            public void run() {
//                OwnBus.$().take(new AsyncEvent(-1));
                OwnBus.$().take(new NewThreadEvent(-1));
            }
        }.start();
    }

    /**
     * 发送事件的方法
     * */
    private void sendEventAtMain() {
//        OwnBus.$().take(new AsyncEvent(-1));
        OwnBus.$().take(new MainThreadEvent(-1));
    }

    private void sendEventAtMain50() {
        for (int i = 1; i <= 50; i++) {
            OwnBus.$().take(new MainThreadEvent(i));
//            OwnBus.$().take(new AsyncEvent(i));
        }
    }

    private void sendEventAtNew50() {
        new Thread() {
            @Override
            public void run() {
                final long id = Thread.currentThread().getId();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"ThreadId: "+id,Toast.LENGTH_SHORT).show();
                    }
                });
                for (int i = 1; i <= 50; i++) {
                    OwnBus.$().take(new NewThreadEvent(i));
//                    OwnBus.$().take(new AsyncEvent(i));
                }
            }
        }.start();
    }

    private void initMainStation() {
        OwnBus.$().newStation(TAG, MainThreadEvent.class, new OwnBusStation<MainThreadEvent>() {
            @Override
            public void onBusStop(MainThreadEvent event) {
                Log.wtf(TAG_MAIN, "Received:ThreadId:" + Thread.currentThread().getId() + "(" + event.type + ")" + event.message);
                tv_mainResult.setText("Thread id:" + Thread.currentThread().getId() + "\n"
                        + event.type + event.message);
            }
        }, new OwnAccident() {
            @Override
            public void onAccident(Throwable error) {
                Toast.makeText(MainActivity.this,TAG_MAIN + ": break down!",Toast.LENGTH_SHORT).show();
            }
        }, OwnBus.BusRoute.main);

        OwnBus.$().newStation(TAG, NewThreadEvent.class, new OwnBusStation<NewThreadEvent>() {
            @Override
            public void onBusStop(NewThreadEvent event) {
                Log.wtf(TAG_MAIN, "Received:ThreadId:" + Thread.currentThread().getId() + "(" + event.type + ")" + event.message);
                tv_mainResult.setText("Thread id:" + Thread.currentThread().getId() + "\n"
                        + event.type + event.message);

            }
        }, OwnBus.BusRoute.main);
    }

    private void initIOStation() {
        OwnBus.$().newStation(TAG, MainThreadEvent.class, new OwnBusStation<MainThreadEvent>() {
            @Override
            public void onBusStop(MainThreadEvent event) {
                Log.wtf(TAG_IO, "Received:ThreadId:" + Thread.currentThread().getId() + "(" + event.type + ")" + event.message);
                SystemClock.sleep(500);
            }
        }, OwnBus.BusRoute.io);

        OwnBus.$().newStation(TAG, NewThreadEvent.class, new OwnBusStation<NewThreadEvent>() {
            @Override
            public void onBusStop(NewThreadEvent event) {
                Log.wtf(TAG_IO, "Received:ThreadId:" + Thread.currentThread().getId() + "(" + event.type + ")" + event.message);
                SystemClock.sleep(500);
            }
        }, OwnBus.BusRoute.io);
    }

    private void initAsyncStation() {
        OwnBus.$().newStation(TAG, MainThreadEvent.class, new OwnBusStation<MainThreadEvent>() {
            @Override
            public void onBusStop(MainThreadEvent event) {
                Log.wtf(TAG_ASYNC, "Received:ThreadId:" + Thread.currentThread().getId() + "(" + event.type + ")" + event.message);
                SystemClock.sleep(1000);

            }
        }, OwnBus.BusRoute.async);

        OwnBus.$().newStation(TAG, NewThreadEvent.class, new OwnBusStation<NewThreadEvent>() {
            @Override
            public void onBusStop(NewThreadEvent event) {
                Log.wtf(TAG_ASYNC, "Received:ThreadId:" + Thread.currentThread().getId() + "(" + event.type + ")" + event.message);
                SystemClock.sleep(1000);
            }
        }, OwnBus.BusRoute.async);

    }

    private void initUsualBus() {
        OwnBus.$().newStation(TAG, MainThreadEvent.class, new OwnBusStation<MainThreadEvent>() {
            @Override
            public void onBusStop(MainThreadEvent event) {
                Log.wtf(TAG_USUAL, "Received:ThreadId:" + Thread.currentThread().getId() + "(" + event.type + ")" + event.message);
                SystemClock.sleep(50);
            }
        });

        OwnBus.$().newStation(TAG, NewThreadEvent.class, new OwnBusStation<NewThreadEvent>() {
            @Override
            public void onBusStop(NewThreadEvent event) {
                Log.wtf(TAG_USUAL, "Received:ThreadId:" + Thread.currentThread().getId() + "(" + event.type + ")" + event.message);
                SystemClock.sleep(50);
            }
        });

    }
}
