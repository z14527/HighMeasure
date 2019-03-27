package com.gyq.highmeasure;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

import static java.lang.Math.abs;

public class MainActivity extends AppCompatActivity {
    private SensorManager sm = null;
    TextView textView = null;
    private float[] r = new float[9];
    //记录通过getOrientation()计算出来的方位横滚俯仰值
    private float[] values = new float[3];
    private float[] gravity = null;
    private float[] geomagnetic = null;
    SensorEventListener sensorEventListener = null;
    float dx = 0, dy = 0, dz = 0;
    float ty1=0,ty2=0,ty3=0;
    int tap = 0;
    ArrayList<Float> ylist = new ArrayList<Float>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView)findViewById(R.id.text1);
        textView.setTextSize(30);
        textView.setOnTouchListener(new OnDoubleClickListener(new OnDoubleClickListener.DoubleClickCallback() {
            @Override
            public void onDoubleClick() {
                if(tap<3)
                    tap++;
                else
                    tap=0;
             }
        }));
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorEventListener = new SensorEventListener() {
            public void onAccuracyChanged(Sensor sensor,int i){

            }
            public void onSensorChanged(SensorEvent event) {
                switch (event.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER: //加速度传感器
                        gravity = event.values;
                        break;
                    case Sensor.TYPE_MAGNETIC_FIELD://磁场传感器
                        geomagnetic = event.values;
                        break;
                }
                if(gravity!=null && geomagnetic!=null) {
                    if(SensorManager.getRotationMatrix(r, null, gravity, geomagnetic)) {
                        SensorManager.getOrientation(r, values);
                        float dx1 = values[0];
                        if(abs(dx - dx1)>0.01) dx = dx1;
                        float dy1 = values[1];
                        if(abs(dy - dy1)>0.01) dy = dy1;
                        float dz1 = values[2];
                        if(abs(dz - dz1)>0.01) dz = dz1;
                        String info = "x方位角＝" + dx + "<br/>y方位角=" + dy + "<br/>z方位角=" + dz;
                        if(tap==1){
                            ty1= dy;
                            if(ylist.size()==0) {
                                ylist.add(abs(ty1));
                                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                Ringtone rt = RingtoneManager.getRingtone(getApplicationContext(), uri);
                                rt.play();
                            }
                            info = info + "<br/>第1次点击：y方位角=" + ylist.get(0);
                        }
                        if(tap==2){
                            ty2= dy;
                            if(ylist.size()==1) {
                                ylist.add(abs(ty2));
                                info = info + "<br/>第2次点击：y方位角=" + ty2;
                                float h = (float) (3.0 * abs(Math.tan(ylist.get(0))) / abs(Math.tan(ylist.get(0)) - Math.tan(ylist.get(1))));
                                float d = (float) (h / abs(Math.tan(ylist.get(0))));
                                info = info + "<br/>方位角1="+ylist.get(0)+"<br/>方位角2="+ylist.get(1)+"<br/>高度=" + h + "<br/>距离=" + d;
                                textView.setText(Html.fromHtml(info));
                                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                Ringtone rt = RingtoneManager.getRingtone(getApplicationContext(), uri);
                                rt.play();
                            }
                         }
                        if(tap==3){
                            ty3= dy;
                            info = info + "<br/>重新开始" + ty3;
                            ylist.clear();
                        }
                        if(ylist.size()<2)
                            textView.setText(Html.fromHtml(info));
                    }
                }
//                if (Sensor.TYPE_ACCELEROMETER != event.sensor.getType()) {
//                    return;
//                }
//                float[] values = event.values;
//                float ax = values[0];
//                float ay = values[1];
//                float az = values[2];
//                double g = Math.sqrt(ax * ax + ay * ay);
//                double cos = ay / g;
//                if (cos > 1) {
//                    cos = 1;
//                } else if (cos < -1) {
//                    cos = -1;
//                }
//                double rad = Math.acos(cos);
//                if (ax < 0) {
//                    rad = 2 * Math.PI - rad;
//                }
//                textView.setText(""+ax+","+ay+","+az+"\n"+rad+"\n"+180*rad/Math.PI);
            }
        };
        sm.registerListener(sensorEventListener,sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(sensorEventListener,sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);

    }
    @Override
    protected void onDestroy() {
        sm.unregisterListener(sensorEventListener);
        super.onDestroy();
    }
}
class OnDoubleClickListener implements View.OnTouchListener{

    private int count = 0;//点击次数
    private long firstClick = 0;//第一次点击时间
    private long secondClick = 0;//第二次点击时间
    /**
     * 两次点击时间间隔，单位毫秒
     */
    private final int totalTime = 1000;
    /**
     * 自定义回调接口
     */
    private DoubleClickCallback mCallback;

    public interface DoubleClickCallback {
        void onDoubleClick();
    }
    public OnDoubleClickListener(DoubleClickCallback callback) {
        super();
        this.mCallback = callback;
    }

    /**
     * 触摸事件处理
     * @param v
     * @param event
     * @return
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (MotionEvent.ACTION_DOWN == event.getAction()) {//按下
            count++;
            if (1 == count) {
                firstClick = System.currentTimeMillis();//记录第一次点击时间
            } else if (2 == count) {
                secondClick = System.currentTimeMillis();//记录第二次点击时间
                if (secondClick - firstClick < totalTime) {//判断二次点击时间间隔是否在设定的间隔时间之内
                    if (mCallback != null) {
                        mCallback.onDoubleClick();
                    }
                    count = 0;
                    firstClick = 0;
                } else {
                    firstClick = secondClick;
                    count = 1;
                }
                secondClick = 0;
            }
        }
        return true;
    }
}

