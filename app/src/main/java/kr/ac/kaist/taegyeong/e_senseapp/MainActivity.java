package kr.ac.kaist.taegyeong.e_senseapp;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

import io.esense.esenselib.*;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "Main_esense";

    private final String PREFKEY_DEVICE_NAME = "prefkey_device_name";
    private final String DEVICE_NAME_DEFAULT = "eSense-0853";

    private final int DEVICE_TIMEOUT_MS = 2000;
    private final int DEVICE_SAMPLING_RATE = 100;

    private final int STATE_CONNECTED = 0;
    private final int STATE_DISCONNECTED = 1;
    private final int STATE_DEVICE_FOUND = 2;
    private final int STATE_DEVICE_NOTFOUND = 3;

    private final int GRAPH_DATA_POINTS = 1000;
    private final int GRAPH_ACCEL_MAX = 5;
    private final int GRAPH_GYRO_MAX = 800;

    private final double STEP_THREASHOLD = 1.2;
    private final int SOUND_DURATION = 50;

    private ESenseManager manager;
    private ESenseConfig sensorConfig;

    private TextView viewConnState;
    private TextView textSensorValue;
    private TextView textStepCount;

    private LineGraphSeries<DataPoint> seriesAccelMag;
    private LineGraphSeries<DataPoint> seriesAccelX;
    private LineGraphSeries<DataPoint> seriesAccelY;
    private LineGraphSeries<DataPoint> seriesAccelZ;
    private LineGraphSeries<DataPoint> seriesGyroX;
    private LineGraphSeries<DataPoint> seriesGyroY;
    private LineGraphSeries<DataPoint> seriesGyroZ;

    private SoundManager mSoundManager;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final EditText textDeviceName = findViewById(R.id.text_device_name);
        Button btnConnect = findViewById(R.id.btn_connect);
        Button btnDisconnect = findViewById(R.id.btn_disconnect);
        Button btnConfig = findViewById(R.id.btn_config);
        Button btnPlay = findViewById(R.id.btn_play);
        Button btnStop = findViewById(R.id.btn_stop);
        viewConnState = findViewById(R.id.conn_state);
        textSensorValue = findViewById(R.id.text_value);
        textStepCount = findViewById(R.id.text_step);

        setConnected(STATE_DISCONNECTED);
        textDeviceName.setText(DEVICE_NAME_DEFAULT);
        textSensorValue.setText("N/A");

        GraphView graphAccelMag = findViewById(R.id.graph_accel_mag);
        GraphView graphAccelX = findViewById(R.id.graph_accel_x);
        GraphView graphAccelY = findViewById(R.id.graph_accel_y);
        GraphView graphAccelZ = findViewById(R.id.graph_accel_z);
        GraphView graphGyroX = findViewById(R.id.graph_gyro_x);
        GraphView graphGyroY = findViewById(R.id.graph_gyro_y);
        GraphView graphGyroZ = findViewById(R.id.graph_gyro_z);

        graphAccelMag.setTitle("Accel Magnitude");
        graphAccelX.setTitle("Accel X");
        graphAccelY.setTitle("Accel Y");
        graphAccelZ.setTitle("Accel Z");
        graphGyroX.setTitle("Gyro X");
        graphGyroY.setTitle("Gyro Y");
        graphGyroZ.setTitle("Gyro Z");

        ArrayList<GraphView> accelGraphViews = new ArrayList<>();
        ArrayList<GraphView> gyroGraphViews = new ArrayList<>();
        accelGraphViews.add(graphAccelMag);
        accelGraphViews.add(graphAccelX);
        accelGraphViews.add(graphAccelY);
        accelGraphViews.add(graphAccelZ);
        gyroGraphViews.add(graphGyroX);
        gyroGraphViews.add(graphGyroY);
        gyroGraphViews.add(graphGyroZ);

        for(GraphView graphView : accelGraphViews) {
            graphView.getViewport().setXAxisBoundsManual(true);
            graphView.getViewport().setMinX(0);
            graphView.getViewport().setMaxX(GRAPH_DATA_POINTS);

            graphView.getViewport().setYAxisBoundsManual(true);
            graphView.getViewport().setMinY(-GRAPH_ACCEL_MAX);
            graphView.getViewport().setMaxY(GRAPH_ACCEL_MAX);

            graphView.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        }
        for(GraphView graphView : gyroGraphViews) {
            graphView.getViewport().setXAxisBoundsManual(true);
            graphView.getViewport().setMinX(0);
            graphView.getViewport().setMaxX(GRAPH_DATA_POINTS);

            graphView.getViewport().setYAxisBoundsManual(true);
            graphView.getViewport().setMinY(-GRAPH_GYRO_MAX);
            graphView.getViewport().setMaxY(GRAPH_GYRO_MAX);

            graphView.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        }
        graphAccelMag.getViewport().setMinY(0);
        seriesAccelMag = new LineGraphSeries<>();
        seriesAccelX = new LineGraphSeries<>();
        seriesAccelY = new LineGraphSeries<>();
        seriesAccelZ = new LineGraphSeries<>();
        seriesGyroX = new LineGraphSeries<>();
        seriesGyroY = new LineGraphSeries<>();
        seriesGyroZ = new LineGraphSeries<>();
        graphAccelMag.addSeries(seriesAccelMag);
        graphAccelX.addSeries(seriesAccelX);
        graphAccelY.addSeries(seriesAccelY);
        graphAccelZ.addSeries(seriesAccelZ);
        graphGyroX.addSeries(seriesGyroX);
        graphGyroY.addSeries(seriesGyroY);
        graphGyroZ.addSeries(seriesGyroZ);
        seriesAccelMag.setColor(getColor(R.color.colorGraphBlue));
        seriesAccelX.setColor(getColor(R.color.colorGraphRed));
        seriesAccelY.setColor(getColor(R.color.colorGraphYellow));
        seriesAccelZ.setColor(getColor(R.color.colorGraphGreen));
        seriesGyroX.setColor(getColor(R.color.colorGraphRed));
        seriesGyroY.setColor(getColor(R.color.colorGraphYellow));
        seriesGyroZ.setColor(getColor(R.color.colorGraphGreen));

        mSoundManager = new SoundManager();


        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manager = new ESenseManager(textDeviceName.getText().toString(), MainActivity.this.getApplicationContext(),
                        new ESenseConnectionListener() {
                            @Override
                            public void onDeviceFound(ESenseManager manager) {
                                Log.d(TAG, "onDeviceFound");
                                setConnected(STATE_DEVICE_FOUND);
                            }

                            @Override
                            public void onDeviceNotFound(ESenseManager manager) {
                                Log.d(TAG, "onDeviceNotFound");
                                setConnected(STATE_DEVICE_NOTFOUND);
                            }

                            @Override
                            public void onConnected(ESenseManager manager) {
                                Log.d(TAG, "onConnected");
                                setConnected(STATE_CONNECTED);
                                registerListeners(manager);
                            }

                            @Override
                            public void onDisconnected(ESenseManager manager) {
                                Log.d(TAG, "onDisconnected");
                                setConnected(STATE_DISCONNECTED);
                            }
                        });
                manager.connect(DEVICE_TIMEOUT_MS);
            }
        });

        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manager.unregisterSensorListener();
                manager.disconnect();
            }
        });

        btnConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "getSensorConfig");
                manager.getSensorConfig();
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSoundManager.play(100);
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSoundManager.pause();
            }
        });
    }

    public void registerListeners(ESenseManager manager) {
        manager.registerSensorListener(
                new ESenseSensorListener() {
                    @Override
                    public void onSensorChanged(ESenseEvent evt) {
                        if (sensorConfig != null)
                            processSensorEvent(evt);
                    }
                }, DEVICE_SAMPLING_RATE);
        manager.registerEventListener(new ESenseEventListener() {
            @Override
            public void onBatteryRead(double voltage) {
                Log.d(TAG, "onBatteryRead");
            }

            @Override
            public void onButtonEventChanged(boolean pressed) {
                Log.d(TAG, "onButtonEventChanged");
            }

            @Override
            public void onAdvertisementAndConnectionIntervalRead(int minAdvertisementInterval, int maxAdvertisementInterval, int minConnectionInterval, int maxConnectionInterval) {
                Log.d(TAG, "onAdvertisementAndConnectionIntervalRead");
            }

            @Override
            public void onDeviceNameRead(String deviceName) {
                Log.d(TAG, "onDeviceNameRead");
            }

            @Override
            public void onSensorConfigRead(ESenseConfig config) {
                Log.d(TAG, "onSensorConfigRead");
                sensorConfig = config;
            }

            @Override
            public void onAccelerometerOffsetRead(int offsetX, int offsetY, int offsetZ) {
                Log.d(TAG, "onAccelerometerOffsetRead");
            }
        });
    }

    private long startTime = -1;
    private boolean inStep = false;
    private int stepCount = 0;

    @SuppressLint("SetTextI18n")
    public void processSensorEvent(ESenseEvent evt) {
        if (startTime < 0)
            startTime = evt.getTimestamp();

        // Relative timestamp
        int timestamp = (int)(evt.getTimestamp() - startTime);

        // Sensor data
        double[] accel = evt.convertAccToG(sensorConfig);
        double[] gyro = evt.convertGyroToDegPerSecond(sensorConfig);

        double accel_mag = Math.sqrt(accel[0]*accel[0] + accel[1]*accel[1] + accel[2]*accel[2]);

        drawSensorValue(timestamp, accel_mag, accel, gyro);

//        if (accel_mag > STEP_THREASHOLD)
//            mSoundManager.play(SOUND_DURATION);

        if (accel_mag > STEP_THREASHOLD && !inStep) {
            inStep = true;
            mSoundManager.play(SOUND_DURATION);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stepCount++;
                    textStepCount.setText(stepCount + "");
                }
            });
        } else if (accel_mag < STEP_THREASHOLD) {
//            mSoundManager.pause();
            inStep = false;
        }
    }

    private int count = 0;

    @SuppressLint("DefaultLocale")
    public void drawSensorValue(final int timestamp, final double accel_mag, final double[] accel, final double[] gyro) {
        final StringBuilder accelResult = new StringBuilder();
        accelResult.append(String.format("Time elapsed: %3d sec", timestamp/1000))
                .append(String.format("\n\nAccel X: %8.3f        Gyro X: %8.3f", accel[0], gyro[0]))
                .append(String.format("\nAccel Y: %8.3f        Gyro Y: %8.3f", accel[1], gyro[1]))
                .append(String.format("\nAccel Z: %8.3f        Gyro Z: %8.3f", accel[2], gyro[2]));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                seriesAccelMag.appendData(new DataPoint(count++, accel_mag), true, GRAPH_DATA_POINTS);
                seriesAccelX.appendData(new DataPoint(count++, accel[0]), true, GRAPH_DATA_POINTS);
                seriesAccelY.appendData(new DataPoint(count++, accel[1]), true, GRAPH_DATA_POINTS);
                seriesAccelZ.appendData(new DataPoint(count++, accel[2]), true, GRAPH_DATA_POINTS);
                seriesGyroX.appendData(new DataPoint(count++, gyro[0]), true, GRAPH_DATA_POINTS);
                seriesGyroY.appendData(new DataPoint(count++, gyro[1]), true, GRAPH_DATA_POINTS);
                seriesGyroZ.appendData(new DataPoint(count++, gyro[2]), true, GRAPH_DATA_POINTS);
                textSensorValue.setText(accelResult.toString());
            }
        });
    }

    public void setConnected(final int state) {
        runOnUiThread(new Runnable() {
//            @SuppressLint("ResourceAsColor")
            @Override
            public void run() {
                switch (state) {
                    case STATE_CONNECTED:
                        viewConnState.setBackgroundColor(getColor(R.color.colorPositive));
                        viewConnState.setText(R.string.text_conn);
                        break;
                    case STATE_DEVICE_FOUND:
                        viewConnState.setBackgroundColor(getColor(R.color.colorNeutral));
                        viewConnState.setText(R.string.text_found);
                        break;
                    case STATE_DISCONNECTED:
                        viewConnState.setBackgroundColor(getColor(R.color.colorNegative));
                        viewConnState.setText(R.string.text_disconn);
                        break;
                    case STATE_DEVICE_NOTFOUND:
                        viewConnState.setBackgroundColor(getColor(R.color.colorNegative));
                        viewConnState.setText(R.string.text_notfound);
                        break;
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
