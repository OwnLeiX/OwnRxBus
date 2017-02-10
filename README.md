# OwnRxBus
一款用RxJava实现的Bus

内含example的module和library的module

将library的module导入自己的工程即可

## 如果出现不可预测的异常，Station会自己修复订阅状态，你可以使用OwnAccident来接收相关异常

## 如果在短时间内post大量Event，而，OwnBusStation.onBusStop()内的工作比较耗时，Bus会为已经订阅的BusStation缓存事件，但是有造成OOM的可能。
