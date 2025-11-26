package com.example.samsunghealthtofirebase;

import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class WearableMessageListenerService extends WearableListenerService {

    private static final String TAG="WearableListener";
    private static final String HR_PATH="/heart_rate_data";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents){
        super.onDataChanged(dataEvents);
        Log.d(TAG,"메세지수신함");
        for(DataEvent event: dataEvents){
            if(event.getType() == DataEvent.TYPE_CHANGED){
                DataItem item = event.getDataItem();
                if(item.getUri().getPath().equals(HR_PATH)){
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    String heartDataStr = dataMap.getString("heart_date");
                    Float heartRate = dataMap.getFloat("heart_rate");
                    Log.d("디버그", "파싱성공");
                }
            }
        }
    }
}
