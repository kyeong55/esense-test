package kr.ac.kaist.taegyeong.e_senseapp;

import android.annotation.SuppressLint;
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

    private final String DEVICE_NAME_DEFAULT = "eSense-0853";
    private final int DEVICE_TIMEOUT_MS = 2000;
    private final int DEVICE_SAMPLING_RATE = 100;

    private final int STATE_CONNECTED = 0;
    private final int STATE_DISCONNECTED = 1;
    private final int STATE_DEVICE_FOUND = 2;
    private final int STATE_DEVICE_NOTFOUND = 3;

    private final int GRAPH_DATA_POINTS = 1000;
    private final int GRAPH_Y_MAX = 5;
    private final int GRAPH_Y_SCALE = 1;

    private ESenseManager manager;
    private ESenseConfig sensorConfig;

    private TextView viewConnState;
    private TextView textSensorValue;

    private LineGraphSeries<DataPoint> seriesAccelMag;
    private LineGraphSeries<DataPoint> seriesAccelX;
    private LineGraphSeries<DataPoint> seriesAccelY;
    private LineGraphSeries<DataPoint> seriesAccelZ;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        EditText textDeviceName = findViewById(R.id.text_device_name);
        Button btnConnect = findViewById(R.id.btn_connect);
        Button btnDisconnect = findViewById(R.id.btn_disconnect);
        Button btnConfig = findViewById(R.id.btn_config);
        viewConnState = findViewById(R.id.conn_state);
        textSensorValue = findViewById(R.id.text_value);

        setConnected(STATE_DISCONNECTED);
        textDeviceName.setText(DEVICE_NAME_DEFAULT);
        textSensorValue.setText("N/A");

        GraphView graphAccelMag = findViewById(R.id.graph_accel_mag);
        GraphView graphAccelX = findViewById(R.id.graph_accel_x);
        GraphView graphAccelY = findViewById(R.id.graph_accel_y);
        GraphView graphAccelZ = findViewById(R.id.graph_accel_z);

        graphAccelMag.setTitle("Accel Magnitude");
        graphAccelX.setTitle("Accel X");
        graphAccelY.setTitle("Accel Y");
        graphAccelZ.setTitle("Accel Z");

        ArrayList<GraphView> graphViews = new ArrayList<>();
        graphViews.add(graphAccelMag);
        graphViews.add(graphAccelX);
        graphViews.add(graphAccelY);
        graphViews.add(graphAccelZ);

        for(GraphView graphView : graphViews) {
            graphView.getViewport().setXAxisBoundsManual(true);
            graphView.getViewport().setMinX(0);
            graphView.getViewport().setMaxX(GRAPH_DATA_POINTS);

            graphView.getViewport().setYAxisBoundsManual(true);
            graphView.getViewport().setMinY(-GRAPH_Y_MAX);
            graphView.getViewport().setMaxY(GRAPH_Y_MAX);

//            graphView.getGridLabelRenderer().setVerticalLabelsVisible(false);
            graphView.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        }
        graphAccelMag.getViewport().setMinY(0);
        seriesAccelMag = new LineGraphSeries<>();
        seriesAccelX = new LineGraphSeries<>();
        seriesAccelY = new LineGraphSeries<>();
        seriesAccelZ = new LineGraphSeries<>();
        graphAccelMag.addSeries(seriesAccelMag);
        graphAccelX.addSeries(seriesAccelX);
        graphAccelY.addSeries(seriesAccelY);
        graphAccelZ.addSeries(seriesAccelZ);


        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manager = new ESenseManager(DEVICE_NAME_DEFAULT, MainActivity.this.getApplicationContext(),
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
                            public void onConnected(final ESenseManager manager) {
                                Log.d(TAG, "onConnected");
                                setConnected(STATE_CONNECTED);
                                Log.d(TAG, "registerSensorListener");
                                manager.registerSensorListener(
                                        new ESenseSensorListener() {
                                            @Override
                                            public void onSensorChanged(ESenseEvent evt) {
                                                if (sensorConfig != null)
                                                    drawSensorValue(evt);
                                            }
                                        }, DEVICE_SAMPLING_RATE);
                                Log.d(TAG, "registerEventListener");
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
//                manager.getAccelerometerOffset();
//                manager.getBatteryVoltage();
//                manager.getDeviceName();
            }
        });
    }

    private long startTime = -1;

    private int count = 0;

    @SuppressLint("DefaultLocale")
    public void drawSensorValue(ESenseEvent evt) {
        if (startTime < 0)
            startTime = evt.getTimestamp();
        final int timestamp = (int)(evt.getTimestamp() - startTime);
//        short[] accel = evt.getAccel();
//        short[] gyro = evt.getGyro();

        final double[] accel = evt.convertAccToG(sensorConfig);
        final double accel_mag = Math.sqrt(accel[0]*accel[0] + accel[1]*accel[1] + accel[2]*accel[2]);

        final StringBuilder accelResult = new StringBuilder();
        accelResult.append("Timestamp: ").append(timestamp)
                .append(String.format("\n\nAccel X: %8.3f", accel[0]))
                .append(String.format("\nAccel Y: %8.3f", accel[1]))
                .append(String.format("\nAccel Z: %8.3f", accel[2]));
//        accelResult.append(String.format("\n\nGyro X: %8d", gyro[0]))
//                .append(String.format("\nGyro Y: %8d", gyro[1]))
//                .append(String.format("\nGyro Z: %8d", gyro[2]));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                seriesAccelMag.appendData(new DataPoint(count++, accel_mag*GRAPH_Y_SCALE), true, GRAPH_DATA_POINTS);
                seriesAccelX.appendData(new DataPoint(count++, accel[0]*GRAPH_Y_SCALE), true, GRAPH_DATA_POINTS);
                seriesAccelY.appendData(new DataPoint(count++, accel[1]*GRAPH_Y_SCALE), true, GRAPH_DATA_POINTS);
                seriesAccelZ.appendData(new DataPoint(count++, accel[2]*GRAPH_Y_SCALE), true, GRAPH_DATA_POINTS);
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
