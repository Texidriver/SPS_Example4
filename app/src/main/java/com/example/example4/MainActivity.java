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

class AccessPoint implements Serializable{
    private String BSSID;
    private int level;

    public AccessPoint(String BSSID, int level) {
        this.BSSID = BSSID;
        this.level = level;
    }

    public String getBSSID() {
        return BSSID;
    }

    public int getLevel() {
        return level;
    }
}

class compareByLevel implements Serializable, Comparator<AccessPoint> {
    @Override
    public int compare(AccessPoint x, AccessPoint y) {
        return Integer.compare(x.getLevel(), y.getLevel());
    }
}

class TrainingPoint implements Serializable{
    private TreeSet<AccessPoint> accessPoints;
    private int position;

    public TrainingPoint(TreeSet<AccessPoint> accessPoints, int position) {
        this.accessPoints = accessPoints;
        this.position = position;
    }

    public TreeSet<AccessPoint> getAccessPoints() {
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
        try{
            ret = (TrainingPoint) ois.readObject();
        }
        catch (EOFException e){
            ret = null;
        }
        return ret;
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
    private Button buttonRssi;
    private Button buttonLoad;

    private String filename = "trainingData.bin";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create items.
        textRssi = (TextView) findViewById(R.id.textRSSI);
        buttonRssi = (Button) findViewById(R.id.buttonRSSI);
        // Set listener for the button.
        buttonRssi.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set text.
                textRssi.setText("\n\tScan all access points:");
                // Set wifi manager.
                wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                // Start a wifi scan.
                wifiManager.startScan();
                // Store results in a list.
                List<ScanResult> scanResults = wifiManager.getScanResults();

                TreeSet<AccessPoint> accessPoints = new TreeSet<AccessPoint>(new compareByLevel());
                for (ScanResult scanResult : scanResults) {
                    textRssi.setText(textRssi.getText() + "\n\tBSSID = "
                            + scanResult.BSSID + "    RSSI = "
                            + scanResult.level + "dBm");
                    accessPoints.add(new AccessPoint(scanResult.BSSID, scanResult.level));
                }
                TrainingPoint scanned = new TrainingPoint(accessPoints, 0);
                try {
                    FileOutputStream fos = getApplicationContext().openFileOutput(filename, Context.MODE_PRIVATE);
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    scanned.save(oos);
                    oos.close();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        buttonLoad = (Button) findViewById(R.id.buttonLoad);
        buttonLoad.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TrainingPoint readPoint = null;
                try {
                    FileInputStream fis = getApplicationContext().openFileInput(filename);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    readPoint = TrainingPoint.load(ois);
                    ois.close();
                    fis.close();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }

                textRssi.setText("\n\tScan all access points:");
                for (AccessPoint accessPoint : readPoint.getAccessPoints().descendingSet()) {
                    textRssi.setText(textRssi.getText() + "\n\tBSSID = "
                            + accessPoint.getBSSID() + "    RSSI = "
                            + accessPoint.getLevel() + "dBm");
                }
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