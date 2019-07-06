package com.gyq.highmeasure;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rorbin.q.radarview.RadarData;
import rorbin.q.radarview.RadarView;

import static java.lang.Math.abs;

public class MainActivity extends AppCompatActivity {
    private SensorManager sm = null;
    TextView textView = null;
    RadarView mRadarView = null;
    private float[] r = new float[9];
    //记录通过getOrientation()计算出来的方位横滚俯仰值
    private float[] values = new float[3];
    private float[] gravity = null;
    private float[] geomagnetic = null;
    SensorEventListener sensorEventListener = null;
    float dx = 0, dy = 0, dz = 0;
    float ty1 = 0,ty2 = 0,ty3 = 0;
    float tx1 = 0;
    int tap = 0;
    ArrayList<Float> ylist = new ArrayList<Float>();
    ArrayList<Float> xlist = new ArrayList<Float>();
    String info1 = "";
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        mRadarView = (RadarView)findViewById(R.id.radarView);
        ArrayList<Float> rData = new ArrayList<Float>();
        ArrayList<String> rLabel = new ArrayList<String>();
        String fs24 = "子癸丑艮寅甲卯乙辰巽巳丙午丁未坤申庚酉辛戌乾亥壬";
        for(int k=0;k<24;k++) {
            rData.add((float) (k + 1));
            rLabel.add(fs24.substring(k,k+1));
        }
        RadarData data = new RadarData(rData);
        mRadarView.addData(data);
        mRadarView.setVertexText(rLabel);
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
                        if(tap>0){
                            double dx2 = abs(dx) - Math.PI*15*(1+xlist.size())/180;
                            if(abs(dx2) < 0.05 && xlist.size() <= 24){
                                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                Ringtone rt = RingtoneManager.getRingtone(getApplicationContext(), uri);
                                rt.play();
                                tx1 = dx;
                                xlist.add(tx1);
                                dx2 = abs(dx) - Math.PI*15*(1+xlist.size())/180;
                                if(dx2 < 0)
                                    info1 = "---->";
                                else
                                    info1 = "<----";
                            }
                            if(xlist.size()>=24){
                                String info = "";
                                for(int k1 = 0;k1 < xlist.size();k1++)
                                    info = info + xlist.get(k1) + "<br/>";
                                textView.setText(Html.fromHtml(info));
                                xlist.clear();
                            }
                        }
                        float dy1 = values[1];
                        if(abs(dy - dy1)>0.01) dy = dy1;
                        float dz1 = values[2];
                        if(abs(dz - dz1)>0.01) dz = dz1;
                        String info = info1 + "<br/> x方位角＝" + dx + "<br/>y方位角=" + dy + "<br/>z方位角=" + dz;
                        if(tap==1){
                            ty1 = dy;
                            tx1 = dx;
                            xlist.add(tx1);
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
                                String add_high = pref.getString("man_add_high_setting","3.0");
                                float fadd = Float.valueOf(add_high);
                                info = info + "<br/>第2次点击：y方位角=" + ty2;
                                float h = (float) (fadd * abs(Math.tan(ylist.get(0))) / abs(Math.tan(ylist.get(0)) - Math.tan(ylist.get(1))));
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
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.man_high_setting) {
            //           Intent intent = new Intent(this,SettingActivity.class);
            setPara("man_high_setting");
        }
        if (id == R.id.man_add_high_setting) {
            //           Intent intent = new Intent(this,SettingActivity.class);
            setPara("man_add_high_setting");
        }
        return super.onOptionsItemSelected(item);
    }
    private void setPara(final String para_name){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("请输入 "+getString(getResources().getIdentifier(para_name,"string",getPackageName()))+" 参数值(cm)：");    //设置对话框标题
        builder.setIcon(android.R.drawable.btn_star);   //设置对话框标题前的图标
        final EditText edit = new EditText(MainActivity.this);
        if(para_name.indexOf("add")>=0)
            edit.setText(pref.getString(para_name,"3"));
        else
            edit.setText(pref.getString(para_name,"165"));
        builder.setView(edit);
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String data1 = edit.getText().toString();
                editor = pref.edit();
                editor.putString(para_name,data1);
                editor.commit();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "你选择了取消", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setCancelable(true);    //设置按钮是否可以按返回键取消,false则不可以取消
        AlertDialog dialog = builder.create();  //创建对话框
        dialog.setCanceledOnTouchOutside(true); //设置弹出框失去焦点是否隐藏,即点击屏蔽其它地方是否隐藏
        dialog.show();
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

