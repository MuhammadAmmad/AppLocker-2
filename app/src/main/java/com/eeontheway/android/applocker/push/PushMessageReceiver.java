package com.eeontheway.android.applocker.push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.igexin.sdk.PushConsts;

/**
 * 个推的推送服务接受器
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class PushMessageReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();

        switch (bundle.getInt(PushConsts.CMD_ACTION)) {
            case PushConsts.GET_MSG_DATA:
                byte[] payload = bundle.getByteArray("payload");
                if (payload != null) {
                    // 调用消息处理器处理消息
                    PushMessageProcessor processor = new PushMessageProcessor(context);
                    processor.processMessage(payload);
                }
                break;
            case PushConsts.GET_CLIENTID:
                String cid = bundle.getString("clientid");
                GetTuiPush.setClientId(cid);
                break;

            default:
                break;
        }
    }
}