package com.example.scg_ecg_test;

import static android.content.ContentValues.TAG;

import static java.lang.Double.min;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.FloatBuffer;
import java.sql.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.Spliterator;

public class MainActivity extends AppCompatActivity {

    //    控件
    private Button btnPlay;
    private Button btnStop;
    //    private ImageView imgScg;
    private ImageView imgEcg;
    private SensorManager sensorManager;
    private Sensor sensor;
    private MySensorEventListener listener;
    private ProgressBar progressBar;
    private TextView progressText;
    private TextView heartRateText;
    private TextView heartRateText2;

//    private boolean isplay = false;

    private LineChart line,lineEcg;
    List<Entry>list=new ArrayList<>();
    List<Entry>listEcg=new ArrayList<>();
    List<Float>listdata = new ArrayList<>();

    Queue<Float> Sensorqueue = new LinkedList<>();
    Queue<Float> readscgqueue = new LinkedList<>();
    Queue<Float> readecgqueue = new LinkedList<>();
    Queue<Float> outqueue = new LinkedList<>();
    Queue<Float> inputqueue = new LinkedList<>();

    private final static float RecordTime = 6.4f;
    private final static int SAMPLE_RATE = 10000;//采样率100hz，延迟时间单位微妙
    private final static int scgOnceDatalimit = 640;
    private final static int scgOnceDatalimit2 = 20;
    private volatile boolean initdatafull = false; //初始数据640是否满
    public volatile boolean runState = false; //记录运行状态
    public volatile boolean outState = false; //记录输出状态
    public volatile boolean onceDataFull = false; //640是否满
    private int progressSingal = 0; //进度条数字
    private int heartRate = 80;
    private int numtest = 0;

    FloatBuffer scgRecordBuffer = FloatBuffer.allocate(10*scgOnceDatalimit);
    FloatBuffer scgOnceBuffer = FloatBuffer.allocate(scgOnceDatalimit);
    float[] floatOnceData = new float[scgOnceDatalimit];
    float[] floatAddData = new float[scgOnceDatalimit];
    float medianData [] = new float [scgOnceDatalimit];

    private Module module;

    long timeSeconds1,timeSeconds2;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnPlay = (Button) findViewById(R.id.start_button);
        btnStop = (Button) findViewById(R.id.stop_button);
//        imgEcg = (ImageView) findViewById(R.id.ecg_img);
//        imgScg = (ImageView) findViewById(R.id.scg_img);
        lineEcg = (LineChart) findViewById(R.id.ecg_lineChart);
        line = (LineChart) findViewById(R.id.scg_lineChart);
        progressBar=findViewById(R.id.progressbar_ecg);
        progressText=findViewById(R.id.loading_text);
        heartRateText=findViewById(R.id.heartRateText);
        heartRateText2=findViewById(R.id.heartRateText2);

        // 得到sensor管理器
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);// get the accelerometer sensor,TYPE_GYROSCOPE,TYPE_ACCELEROMETER，TYPE_LINEAR_ACCELERATION
        listener = new MySensorEventListener();


        line.setNoDataText("Empty data");
        line.setNoDataTextColor(Color.parseColor("#808080"));
        lineEcg.setNoDataText("Empty data");
        lineEcg.setNoDataTextColor(Color.parseColor("#808080"));

        progressBar.setVisibility(View.GONE);
        heartRateText.setVisibility(View.GONE);
        heartRateText2.setVisibility(View.GONE);


//        String fileNamescg = "myscg.txt";
//        String fileNameecg = "myecg.txt";
//        Log.d(TAG, "onCreate: start read filedata ");

//        String [] stringscg = scginitAssets();
//        String [] stringecg = ecginitAssets();
//        for(String a :stringscg) readscgqueue.add(Float.valueOf(a));
//        for(String a :stringecg) readecgqueue.add(Float.valueOf(a));


//        try (Scanner sc = new Scanner(new FileReader("myscg.txt"))) {
//            Log.d(TAG, "onCreate: read filedata ");
//            while (sc.hasNextLine()) {  //按行读取字符串
//
//                String line = sc.nextLine();
//
//                Log.d(TAG, "onCreate: readstringdata "+" "+ line);
//                readscgqueue.add(Float.valueOf(line));
//                System.out.println(line);
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
////        Log.d(TAG, "onCreate: readscgdata :" );
//        try (Scanner sc = new Scanner(new FileReader(fileNameecg))) {
//            while (sc.hasNextLine()) {  //按行读取字符串
//                String line = sc.nextLine();
//                readecgqueue.add(Float.valueOf(line));
//                System.out.println(line);
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }


//        progressText.setVisibility(View.GONE);

        //monitor the "play" button
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnPlay.setEnabled(false);
                btnStop.setEnabled(true);//avoid the second press
//                runState = true;

                if (module == null) {
                    module = LiteModuleLoader.load(assetFilePath(getApplicationContext(), "wxw9.ptl"));
//                    module = LiteModuleLoader.load(assetFilePath(getApplicationContext(), "ND_train3.ptl"));
                }

                sensorManager.registerListener(listener, sensor, SAMPLE_RATE);
//                new ThreadInstantRecord().start();
                if(progressSingal != 100 ){
                    progressText.setText("Loading...");
                    progressText.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(progressSingal);
                }
                else {
                    progressText.setText("Showing");
                }

                Log.d(TAG, "run: start recoding 开始记录数据");
            }
        });

        //monitor the "stop" button
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnPlay.setEnabled(true);
                btnStop.setEnabled(false);
//                runState=false;
//                progressBar.setVisibility(View.GONE);
                progressText.setText("Pausing");
                try{
                    sensorManager.unregisterListener(listener);
                }catch (Exception e) {
                    //TODOL handle this
                }
            }
        });

    }


    private int Sensorindex = 0;
    

    private void inputmodel(float[] floatInputDataInit) {


        Tensor inTensor = Tensor.fromBlob(floatInputDataInit, new long[]{1,1, scgOnceDatalimit});
        final Tensor outputTensor = module.forward(IValue.from(inTensor)).toTensorList();

        final float [] outdata = outputTensor.getDataAsFloatArray();

        list.clear();
        listEcg.clear();

        int i = 0;
        for(float a : floatInputDataInit) list.add(new Entry(i++,a));

        // ecg归一化
        i = 0;
        double []outdata2 = new double[640];
        for(float a :outdata){
            outdata2[i++] = (double)a;
        }

        float maxdata =(float) Arrays.stream(outdata2).max().getAsDouble();
        float mindata =(float) Arrays.stream(outdata2).min().getAsDouble();

        i = 0;
        for(float a :outdata){
            Random r = new Random();
//            Log.d(TAG, "inputmodel: outdataecg " + i + " " + a );
            listEcg.add(new Entry(i++,(a-mindata)/(maxdata-mindata)));
//            listEcg.add(new Entry(i++,(a*100)));
//            listEcg.add(new Entry(i++,a+(float)((r.nextInt(5)*1.0/10))));
            Log.d(TAG, "inputmodel: outdataecg " + i + " " + a );
        }


        numtest ++;
        Log.d(TAG, "inputmodel: outdataecgsize " + outdata.length);

        chartViewShow();
//        峰值
//        int [] peakdatax = heartRateEstimate(outdata);


//        inputqueue.add(a);
//        for(float a : outdata) outqueue.add(a);
//        chartViewInit();

    }


    private int[] heartRateEstimate(float[] data){
        int length = 640;
        int minCnt = 0, peakRadi = 0;
        int peak[] = new int[length];
        for(int i = 1; i < length /2 +1; i++){ // 找峰与峰之间最长距离
            int cnt = 0;
            for(int j = i; j < length - i ; j++)
                if(data[j] > data[j-i] && data[j] > data[j+i])
                    cnt -= 1;
            if(cnt < minCnt) {
                minCnt = cnt;
                peakRadi = i;
            }
        }
        for(int i = 1; i < peakRadi+1; i++ )
            for(int j = i; j < length-i ; j++)
                if(data[j] >  data[j-i] && data[j] > data[j+i] )
                    peak[j] += 1;

        double sum = 0;
        int peakLast = -1, num = 0;
        int datax[] = new int[length];
        for(int i = 0; i < length; i++){
            if(peak[i] == peakRadi) {
                if(peakLast == -1) peakLast = i;
                else if(i-peakLast > 30 && i-peakLast < 140){
                    sum += i-peakLast;
                    peakLast = i;
                    datax[num++] = i;
                }
            }
        }
//            int heartRate = 80;
        if(sum != 0 && num!= 0){
            int now =(int) (60/(sum/num)*100);
            if(now > heartRate) heartRate +=1;
            else if(now < heartRate) heartRate -=1;
            heartRateText.setText("" + heartRate);
        }

        return datax;
    }

    private int [] peakfind(float [] data){
        int len = data.length;
        float [][]mki = new float[len][len];
        float []rk=new float[len];

        for(int k = 1; k < len/2+1; k++)
            for(int i = k; i < len-k; i++)
                mki[k][i] = (float)((data[i]>data[i-k]&&data[i]>data[i+k])?0:1.2);

        int pindex = 1;
        for(int k = 1; k < len/2+1; k++){
            for(int i = k; i < len-k; i++)
                rk[k] += mki[k][i];
            if(rk[k] < rk[pindex]) pindex = k;
        }

        int[] peak = new int[len];
        float[] cnt = new float[len];
        for(int i = 0; i < len; i++)
            for(int k = 1; k < pindex; k++){
                float a = 0;
                for(int kk = 1; kk < pindex; kk++) a += mki[kk][i];
                cnt[i] +=(float)(1.0/(pindex-1)*Math.sqrt((Math.pow((mki[k][i] - 1.0/pindex*a),2))));
            }


        int ii = 0;
        for(int i = 0; i < len; i++){
            if(cnt[i] < 1e-1) peak[ii++] = i;
        }
        return peak;
    }


    private void chartViewShow(){
        if(progressSingal == 100){
            progressText.setText("Showing");
            progressBar.setVisibility(View.GONE);
            heartRateText.setVisibility(View.VISIBLE);
            heartRateText2.setVisibility(View.VISIBLE);
        }
        else{
            for(int i = progressSingal; i <= 100; i++)
                progressBar.setProgress(i);
            progressSingal = 100;
        }

        Log.d(TAG, "chartViewShow: listdatasize: " + list.size());
        Log.d(TAG, "chartViewShow: listecgdatasize: " + listEcg.size());

        LineDataSet lineDataSet = new LineDataSet(list, "scg"); //list-scg数据
        LineData lineData = new LineData(lineDataSet);
        lineDataSet.setDrawCircles(false); //不显示圆点
        lineDataSet.setLineWidth(1.3f); // //线宽度
        lineDataSet.setColor(Color.rgb(255,165,0)); //线颜色 -橙色
        line.setBackgroundColor(Color.rgb(255,255,255)); //背景黑色 - 白色
//            line.getDescription().setEnabled(false); // 不显示description
        line.setData(lineData);
        Description description = new Description();
//            description.setText("SCG Signal");
        description.setTextColor(Color.rgb(255,255,255)); // 黑色
//            description.setPosition(100,100);

        description.setTextSize(20);
        line.setDescription(description);


        line.getLegend().setEnabled(false); //不显示图例

        line.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        //隐藏右边的Y轴
        line.getAxisRight().setEnabled(false);
        line.getAxisLeft().setEnabled(false); //隐藏左边y轴
        line.getXAxis().setEnabled(false); //隐藏x轴
        line.invalidate();
        line.notifyDataSetChanged();


        //ecg
        LineDataSet lineDataSetEcg = new LineDataSet(listEcg, "ecg");
        LineData lineDataEcg = new LineData(lineDataSetEcg);
        lineDataSetEcg.setDrawCircles(false); //不显示圆点
        lineDataSetEcg.setLineWidth(1.5f); // //线宽度
        lineDataSetEcg.setColor(Color.rgb(255,165,0)); //线颜色
        lineEcg.setBackgroundColor(Color.rgb(255,255,255)); //背景黑色
//            lineEcg.getDescription().setEnabled(false); // 不显示description
        Description descriptionecg = new Description();
//            descriptionecg.setText("ecg");
        descriptionecg.setTextColor(Color.rgb(255,255,255));
//            descriptionecg.setPosition(100,100);
//            descriptionecg.setTextSize(20);
//            lineEcg.setDescription(descriptionecg);
//            Log.d(TAG, "chartViewShow: outecg ------");

        lineEcg.setDescription(descriptionecg);
        lineEcg.getLegend().setEnabled(false); //不显示图例


        lineEcg.setData(lineDataEcg);
        lineEcg.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        //隐藏右边的Y轴
        lineEcg.getAxisRight().setEnabled(false);
        lineEcg.getAxisLeft().setEnabled(false); //隐藏左边y轴
        lineEcg.getXAxis().setEnabled(false); //隐藏x轴
        lineEcg.invalidate();
        lineEcg.notifyDataSetChanged();

        for(int ii =0 ; ii < listEcg.size(); ii++){
            Log.d(TAG, "inputmodel: outdatatest " + numtest +" "+ ii + " " + listEcg.get(ii));
        }

//            list.clear();
//            listEcg.clear();

//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

    }



    private String assetFilePath(Context context, String assetName) {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, assetName + ": " + e.getLocalizedMessage());
        }
        return null;
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        sensorManager.registerListener(listener, sensor, SAMPLE_RATE);
//        sensorManager.unregisterListener(listener);
//        progressText.setText("Pausing");
//    }
}






