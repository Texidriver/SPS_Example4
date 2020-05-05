package com.example.example4;

import java.lang.*;
import java.util.*;
import java.io.*;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

class TrainingPoint implements Serializable {
    private HashMap<String, Integer> accessPoints;
    private int position;

    public TrainingPoint(HashMap<String, Integer> accessPoints, int position) {
        this.accessPoints = accessPoints;
        this.position = position;
    }

    public HashMap<String, Integer> getAccessPoints() {
        return accessPoints;
    }

    public int getPosition() {
        return position;
    }

    public void save(ObjectOutputStream oos) throws IOException {
        oos.writeObject(this);
    }

    public static TrainingPoint load(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        TrainingPoint ret;
        try {
            ret = (TrainingPoint) ois.readObject();
        } catch (EOFException e) {
            ret = null;
        }
        return ret;
    }

    public double signalStrength(String BSSID) {
        double sigStr;
        if (this.accessPoints.containsKey(BSSID)) {
            int level = this.accessPoints.get(BSSID);
            if (level > -50) {
                sigStr = 4;
            } else if (level > -60) {
                sigStr = 3;
            } else if (level > -70) {
                sigStr = 2;
            } else {
                sigStr = 1;
            }
        } else {
            sigStr = 0;
        }
        return sigStr;
    }

    public static double distance(TrainingPoint inPointA, TrainingPoint inPointB) {
        double dist = 0;
        HashSet<String> unionBSSID = new HashSet<String>();
        unionBSSID.addAll(inPointA.getAccessPoints().keySet());
        unionBSSID.addAll(inPointB.getAccessPoints().keySet());
        for (String s : unionBSSID) {
            double cur_dist = inPointA.signalStrength(s) - inPointB.signalStrength(s);
            cur_dist *= cur_dist;  // square
            dist += cur_dist;  // sum of square
        }
        return Math.sqrt(dist);
    }
}

/**
 * Smart Phone Sensing Example 4. Wifi received signal strength.
 */
public class MainActivity extends Activity implements SensorEventListener {

    /**
     * The wifi manager.
     */
    private WifiManager wifiManager;
    /**
     * The text view.
     */
    private TextView textRssi;
    private TextView action_info;
    private SensorManager sensorManager;
    /**
     * The accelerometer.
     */
    private Sensor accelerometer;

    private double aX = 0;
    private double aY = 0;
    private double aZ = 0;

    private double data_action[][] = {
            {3.761,1.539,1}, {2.229,1.108,1}, {2.980,1.324,1}, {2.687,1.770,1}, {1.207,0.826,1}, {1.900,0.308,1},
            {2.506,0.813,1}, {0.879,0.053,1}, {2.274,0.662,1}, {2.342,0.652,1}, {3.544,2.388,1}, {1.971,1.442,1},
            {3.423,1.667,1}, {2.591,1.592,1}, {2.336,1.153,1}, {1.336,1.357,1}, {2.023,1.254,1}, {1.213,0.882,1},
            {3.210,1.245,1}, {3.795,2.202,1},{19.170,37.432,2},{14.690,20.494,2}, {13.433,13.879,2}, {12.5340,19.640,2},
            {10.660,15.232,2}, {13.480,20.556,2}, {13.220,23.621,2}, {8.760,10.417,2}, {13.420, 25.415,2},
            {10.520,13.695,2}, {9.210,9.210,2}, {9.570,15.103,2}, {8.365,11.23,2}, {12.10,20.045,2}, {7.583,10.021,2},
            {11.280,19.832,2}, {14.520,13.691,2}, {12.560,24.621,2}, {9.670,13.425,2},{7.210,10.294,2},
            {0.521,0.002,3}, {0.627,0.077,3}, {0.757,0.042,3}, {1.001,0.093,3}, {0.210,0.033,3}, {0.073,0.010,3},
            {0.080,0.000,3}, {0.088,0.022,3}, {0.218,0.021,3}, {0.444,0.003,3}, {0.310,0.002,3}, {0.673,0.098,3},
            {0.780,0.527,3}, {0.030,0.000,3}, {0.223,0.001,3}, {0.143,0.002,3}, {0.713,0.070,3}, {0.057,0.001,3},
            {0.893,0.681,3}, {0.193,0.013,3}};
    /**
     * The button.
     */
    private Button buttonUL;
    private Button buttonUR;
    private Button buttonLL;
    private Button buttonLR;
    private Button buttonRssi;
    private Button buttonLoad;

    private String filename;

    private HashSet<TrainingPoint> trainingPoints;

    protected void saveTrainingPoints (HashSet<TrainingPoint> trainingPoints) {
        try {
            FileOutputStream fos = getApplicationContext().openFileOutput(filename, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(trainingPoints);
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected HashMap<String, Integer> wifiScan () {
        // Start a wifi scan.
        wifiManager.startScan();
        // Store results in a list.
        List<ScanResult> scanResults = wifiManager.getScanResults();
        // Set text.
        // textRssi.setText("\n\tScan all access points:");
        HashMap<String, Integer> accessPoints = new HashMap<String, Integer>();
        for (ScanResult scanResult : scanResults) {
            // textRssi.setText(textRssi.getText() + "\n\tBSSID = "
            //         + scanResult.BSSID + "    RSSI = "
            //         + scanResult.level + "dBm");
            accessPoints.putIfAbsent(scanResult.BSSID, scanResult.level);
        }
        return accessPoints;
    }

    protected void wifiTrain(int position) {
        trainingPoints.add(new TrainingPoint(wifiScan(), position));
        saveTrainingPoints(trainingPoints);
        textRssi.setText("Total number of training points is " + trainingPoints.size());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        action_info = (TextView) findViewById(R.id.actionbox);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // set accelerometer
            accelerometer = sensorManager
                    .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            // register 'this' as a listener that updates values. Each time a sensor value changes,
            // the method 'onSensorChanged()' is called.
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // No accelerometer!
        }

        final ArrayList<Double> Testresult = new ArrayList();
        Timer timer = new Timer();
        TimerTask t = new TimerTask() {
            @Override
            public void run() {

                double total_acc = Math.sqrt((Math.pow(aX, 2) + Math.pow(aY, 2) + Math.pow(aZ, 2)));
                if (Testresult.size() == 10) {
                    Testresult.remove(0);
                    Testresult.add(total_acc);
                    double avg = 0;
                    double variance = 0;
                    double distance_activity[][] = new double[2][60];

                    double diff = Collections.max(Collections.singleton(total_acc)) - Collections.min(Collections.singleton(total_acc));
                    for (int i = 0; i < 10; i++) {
                        avg += (0.1 * Testresult.get(i));
                    }

                    for (int j = 0; j < 10; j++) {
                        variance += Math.pow((Testresult.get(j) - avg), 2);
                    }
                    variance /= 9;

                    for (int i = 0; i < 60; i++) {
                        distance_activity[0][i] = i;
                        distance_activity[1][i] = 0;
                    }
                    for (int j = 0; j < 60; j++) {
                        distance_activity[1][j] = Math.pow((diff - data_action[j][0]), 2)
                                + Math.pow((variance - data_action[j][1]), 2);
                    }

                    for (int i = 0; i < 60; i++) {
                        for (int j = 0; j < 59; j++) {
                            if (distance_activity[1][j + 1] < distance_activity[1][j]) {
                                double temp = distance_activity[1][j + 1];
                                distance_activity[1][j + 1] = distance_activity[1][j];
                                distance_activity[1][j] = temp;

                                double temp_index = distance_activity[0][j + 1];
                                distance_activity[0][j + 1] = distance_activity[0][j];
                                distance_activity[0][j] = temp_index;
                            }
                        }
                    }

                    int counter_act = 0;
                    int num_mov = 0;
                    int num_squ = 0;
                    int num_sta = 0;
                    int index2;
                    while (counter_act < 9) {
                        index2 = (int) distance_activity[0][counter_act];
                        if (data_action[index2][2] == 1) num_mov++;
                        else if (data_action[index2][2] == 2) num_squ++;
                        else if (data_action[index2][2] == 3) num_sta++;
                        counter_act++;
                    }
                    action_info.setText("");
                    if(num_mov >= num_squ && num_mov >= num_sta){
                        action_info.setText("Moving");
                    }else if(num_squ >= num_mov && num_squ >= num_sta){
                        action_info.setText("Squat");
                    }else{
                        action_info.setText("Standing");
                    }
                }else{
                    System.out.println("Notstarted");
                    action_info.setText("Wait");
                    Testresult.add(total_acc);}

            }
        };
        timer.scheduleAtFixedRate(t,100,100);

        filename = "trainingData.bin";
        trainingPoints = new HashSet<TrainingPoint>();

        // Set wifi manager.
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Create items.
        textRssi = (TextView) findViewById(R.id.textRSSI);

        buttonUL = (Button) findViewById(R.id.buttonTrainUL);
        buttonUL.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiTrain(0);
            }
        });

        buttonUR = (Button) findViewById(R.id.buttonTrainUR);
        buttonUR.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiTrain(1);
            }
        });

        buttonLL = (Button) findViewById(R.id.buttonTrainLL);
        buttonLL.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiTrain(2);
            }
        });

        buttonLR = (Button) findViewById(R.id.buttonTrainLR);
        buttonLR.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiTrain(3);
            }
        });

        buttonRssi = (Button) findViewById(R.id.buttonRSSI);
        // Set listener for the button.
        buttonRssi.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TrainingPoint scanned = new TrainingPoint(wifiScan(), -1);

                int k = (int) Math.ceil(Math.sqrt(trainingPoints.size()));
                if (k % 2 == 0){
                    k++;
                }
                k = Math.min(k, trainingPoints.size());

                TreeMap<Double, TrainingPoint> kNN = new TreeMap<Double, TrainingPoint>();
                for (TrainingPoint trainingPoint : trainingPoints) {
                    double distance = TrainingPoint.distance(trainingPoint, scanned);
                    if (kNN.size() < k) {
                        kNN.put(distance, trainingPoint);
                    } else if (distance < kNN.lastKey()) {
                        kNN.pollLastEntry();
                        kNN.put(distance, trainingPoint);
                    }
                }

                int[] freq = new int[4];
                for (Map.Entry<Double, TrainingPoint> m : kNN.entrySet()) {
                    freq[m.getValue().getPosition()]++;
                }
                int maxAt = 0;
                for (int i = 0; i < 4; i++) {
                    maxAt = freq[i] > freq[maxAt] ? i : maxAt;
                }

                switch (maxAt) {
                    case 0:
                        textRssi.setText("\tYou are at ↖️");
                        break;
                    case 1:
                        textRssi.setText("\tYou are at ↗️");
                        break;
                    case 2:
                        textRssi.setText("\tYou are at ↙️️");
                        break;
                    case 3:
                        textRssi.setText("\tYou are at ↘️");
                        break;
                    default:
                        textRssi.setText("");
                        break;
                }
            }
        });

        buttonLoad = (Button) findViewById(R.id.buttonLoad);
        buttonLoad.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    FileInputStream fis = getApplicationContext().openFileInput(filename);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    trainingPoints = (HashSet<TrainingPoint>) ois.readObject();
                    ois.close();
                    fis.close();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
                // textRssi.setText("\n\tScan all access points:");
                // for (Map.Entry<String, Integer> m : readPoint.getAccessPoints().entrySet()) {
                //     textRssi.setText(textRssi.getText() + "\n\tBSSID = "
                //             + m.getKey() + "    RSSI = "
                //             + m.getValue() + "dBm");
                // }
                textRssi.setText("Total number of training points is " + trainingPoints.size());
            }
        });
    }

    // onResume() registers the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    // onPause() unregisters the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing.
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        aX = event.values[0];
        aY = event.values[1];
        aZ = event.values[2];
    }
}
