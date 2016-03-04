package com.eeontheway.android.applocker.locate;

import android.content.Context;
import android.location.Location;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;

/**
 * 后台定位服务
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-8
 */
public class LocationService {
	private LocationClient client;
	private LocationClientOption mOption;
	private LocationClientOption diyOption;
    private MyLocationListener bdListener;
    private PositionChangeListener positionListener;

	private static LocationService instance;
	private static Object objLock = new Object();

    private Position lastPosition;
    private int maxDistance = 10;

    /**
     * 位置状态监听器
     */
    public interface PositionChangeListener {
        void onPositionChanged (Position oldPosition, Position newPosition);
    }

    /**
     * 获取位置监听器
     * @return 监听器
     */
    public PositionChangeListener getPositionListener() {
        return positionListener;
    }

    /**
     * 设置位置监听器
     * @param positionListener 位置监听器
     */
    public void setPositionListener(PositionChangeListener positionListener) {
        this.positionListener = positionListener;
    }

	/**
	 * 获取上一次定位的地址
	 * @return 上一次位置，如果之前没有定位，返回null
     */
	public Position getLastPosition() {
		return lastPosition;
	}

	/***
	 * 构造函数
	 */
	public LocationService() {
	}


	public static LocationService getInstance (Context context) {
		synchronized (objLock) {
			if(instance == null){
				instance = new LocationService();
				instance.client = new LocationClient(context);
				instance.bdListener = instance.new MyLocationListener();
				instance.client.setLocOption(instance.getDefaultLocationClientOption());
			}
		}

		return instance;
	}

	/***
	 * 设置定位选项
	 * @param option 选项
	 * @return isSuccessSetOption 是否设置成功
	 */
	public boolean setLocationOption(LocationClientOption option){
		boolean isSuccess = false;

        if(option != null){
			if(client.isStarted()) {
                client.stop();
            }

			diyOption = option;
			client.setLocOption(option);
		}
		return isSuccess;
	}

    /**
     * 返回当前的定位设置
     * @return 当前的定位设置
     */
	public LocationClientOption getOption(){
		return diyOption;
	}

	/***
	 * 获取缺省的定位设置
	 * @return DefaultLocationClientOption
	 */
	public LocationClientOption getDefaultLocationClientOption(){
		if(mOption == null){
			mOption = new LocationClientOption();

            //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
			mOption.setLocationMode(LocationMode.Hight_Accuracy);

            //可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
			mOption.setCoorType("bd09ll");

            //可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
			mOption.setScanSpan(3000);

            //可选，设置是否需要地址信息，默认不需要
		    mOption.setIsNeedAddress(true);

            // 需要周边PIO列表
            mOption.setIsNeedLocationPoiList(false);

            //可选，设置是否需要设备方向结果
		    mOption.setNeedDeviceDirect(false);

            //可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
		    mOption.setLocationNotify(false);

            //可选，默认true，设置是否在stop的时候杀死这个进程，默认不杀死
		    mOption.setIgnoreKillProcess(true);

            //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到
		    mOption.setIsNeedLocationDescribe(true);

            //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
		    mOption.setIsNeedLocationPoiList(true);

            //可选，默认false，设置是否收集CRASH信息，默认收集
		    mOption.SetIgnoreCacheException(false);
		}
		return mOption;
	}

    /**
     * 启动定位
     */
	public void start(){
		synchronized (objLock) {
			if(client != null && !client.isStarted()){
                client.registerLocationListener(bdListener);
				client.start();
			}
		}
	}

    /**
     * 结束定位
     */
    public void stop(){
		synchronized (objLock) {
			if(client != null && client.isStarted()){
                client.unRegisterLocationListener(bdListener);
				client.stop();
			}
		}
	}

    /**
     * 定位SDK监听器
     */
    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null || location.getLocType() == BDLocation.TypeServerError
                    || location.getLocType() == BDLocation.TypeNetWorkException
                    || location.getLocType() == BDLocation.TypeCriteriaException) {
                return;
            }

            if(lastPosition == null){
                // 保存当前位置
				lastPosition = new Position();
				lastPosition.setRadius(location.getRadius());
				lastPosition.setLatitude(location.getLatitude());
				lastPosition.setLongitude(location.getLongitude());
				lastPosition.setAddress(location.getAddrStr());
            }else{
                // 计算之前定位的位置与当前定位间的偏差
                float[] distance = new float[1];

                Location.distanceBetween(lastPosition.getLatitude(), lastPosition.getLongitude(),
                                        location.getLatitude(), location.getLongitude(), distance);

                // 如果偏差较大，则说明切换了地点(考虑到定位本身存在偏差)
                if(distance[0] >= maxDistance){
					Position newPosition = new Position();
					newPosition.setRadius(location.getRadius());
					newPosition.setLatitude(location.getLatitude());
					newPosition.setLongitude(location.getLongitude());
					newPosition.setAddress(location.getAddrStr());

                    // 通知外界，地点发生了变化
					if (positionListener != null) {
						positionListener.onPositionChanged(lastPosition, newPosition);
					}

					lastPosition.update(newPosition);
				}
            }
        }
    }
}
