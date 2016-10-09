package huadou.bleserverlib.listener;

/**
 * Created by jinliang on 16-9-12.
 *
 *  BLE Server Manager 之间的接口对应的方法函数
 *
 *  调用的先后顺序
 *
 *  start: 开启蓝牙开关
 *  addCharacteritic : 添加特征, 添加服务
 *  startAdvertic():  进行广播
 *
 *  停止链接的状态
 *
 */
public interface BLEServerListener {

    void openBluetooth() ; //  BLEServerManager 生命周期的开始

    void addCharacteristics(String characteristicUUID) ; // 添加特征

    void startAdvertic(); //
}
