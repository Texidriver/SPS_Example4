package com.example.example4;

import java.lang.*;
import java.util.*;
import java.io.*;

import android.app.Activity;
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
public class MainActivity extends Activity {

    /**
     * The wifi manager.
     */
    private WifiManager wifiManager;
    /**
     * The text view.
     */
    private TextView textRssi;
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
//                if (pointInMem != null) {
//                    textRssi.setText(textRssi.getText() + "\n\tdistance = "
//                            + TrainingPoint.distance(pointInMem, scanned));
//                }
//                pointInMem = scanned;
                textRssi.setText("");
                for (TrainingPoint trainingPoint : trainingPoints) {
                    textRssi.setText(textRssi.getText()
                            + "Distance = "
                            + TrainingPoint.distance(trainingPoint, scanned)
                            + "\n\r");
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
    }

    // onPause() unregisters the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
    }
}