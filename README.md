# OwnRxBus
一款用RxJava实现的Bus

内含example的module和library的module

将library的module导入自己的工程即可

## 如果在某一时段内爆发性的发送事件，比如for循环发送事件
## 在子线程，可能会导致除了OwnScheduler.usual之外的Station丢失事件(其他Sation还未处理完成，下一次事件已经发送完成)
## 在UIThread，会导致OwnScheduler.main的Station丢失事件，如果存在耗时的OwnScheduler.usual Station，可能会导致ANR
## 如果进行了以上操作，Station会自动尝试重新订阅。
## 你可以使用OwnBusAccident的onBusBreakDown(Throwable error)来获取关于Station崩溃的信息
