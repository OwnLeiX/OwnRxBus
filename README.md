# OwnRxBus
一款用RxJava实现的Bus

内含example的module和library的module

将library的module导入自己的工程即可
## 注意在子线程请尽量不要爆发性的发送事件，如果如此做，会导致除了OwnScheduler.usual之外的Station崩溃
## 在UIThread爆发性的发送事件也会导致OwnScheduler.main的Station崩溃。
## 如果进行了以上操作，Station会自动尝试重新订阅。
## 你可以使用OwnBusAccident的onBusBreakDown(Throwable error)来获取关于Station崩溃的信息
