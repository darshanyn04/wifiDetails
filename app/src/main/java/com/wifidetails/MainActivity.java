package com.wifidetails;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.ProxyInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_LOCATION = 1000;
    private static final String TODO = "Open";
    private TextView rootStatusTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rootStatusTextView = findViewById(R.id.root_status);
        boolean hasRoot = hasRootAccess();
        rootStatusTextView.setText("Root access: " + (hasRoot ? "Yes" : "No"));


        // Check and request location permission if not granted
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_LOCATION);
        } else {
            // Permission is already granted, proceed with your code
            getWiFiDetails();
        }
//        askForProxySettings();
    }

    private void getWiFiDetails() {
        // Get the WiFi details
        WifiInfo wifiInfo = getWifiInfo(this);
        String proxyDetails = getProxyDetails(this);

        // Display the WiFi and Proxy details
        if (wifiInfo != null) {
            String ssid = wifiInfo.getSSID();
            String bssid = wifiInfo.getBSSID();
            int signalStrength = wifiInfo.getRssi();
            String securityType = getSecurityType(wifiInfo);

            TextView ssidTextView = findViewById(R.id.ssid);
            TextView bssidTextView = findViewById(R.id.bssid);
            TextView signalStrengthTextView = findViewById(R.id.signal_strength);
            TextView securityTypeTextView = findViewById(R.id.security_type);
            TextView proxyDetailsTextView = findViewById(R.id.proxy_details);

            ssidTextView.setText("SSID: " + ssid);
            bssidTextView.setText("BSSID: " + bssid);
            signalStrengthTextView.setText("Signal Strength: " + signalStrength + " dBm");
            securityTypeTextView.setText("Security Type: " + securityType);

            if (proxyDetails != null && !proxyDetails.isEmpty()) {
                proxyDetailsTextView.setText("Proxy Details: " + proxyDetails);
            } else {
                proxyDetailsTextView.setText("Proxy Details: Not set");
            }
        }
    }
    private boolean hasRootAccess() {
        Process process;
        try {
            process = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            int exitValue = process.waitFor();
            return (exitValue == 0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }


    private WifiInfo getWifiInfo(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            return wifiManager.getConnectionInfo();
        }
        return null;
    }

    private String getSecurityType(WifiInfo wifiInfo) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return TODO;
        }
        List<ScanResult> scanResults = wifiManager.getScanResults();

        for (ScanResult scanResult : scanResults) {
            if (scanResult.BSSID.equals(wifiInfo.getBSSID())) {
                String capabilities = scanResult.capabilities;

                if (capabilities.contains("WPA") || capabilities.contains("WPA2")) {
                    return "WPA/WPA2";
                } else if (capabilities.contains("WEP")) {
                    return "WEP";
                } else {
                    return "Open";
                }
            }
        }

        return "Unknown";
    }

    private String getProxyDetails(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = cm.getActiveNetwork();

        if (network != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);

            if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                ProxyInfo proxyInfo = cm.getLinkProperties(network).getHttpProxy();

                if (proxyInfo != null) {
                    return proxyInfo.getHost() + ":" + proxyInfo.getPort();
                }
            }
        }

        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with your code
                    getWiFiDetails();
                } else {
//                  need to write diffrent Logic
                }
                return;
            }
        }
    }

//    private void askForProxySettings() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Proxy Settings");
//
//        final EditText inputHost = new EditText(this);
//        inputHost.setHint("Host");
//        final EditText inputPort = new EditText(this);
//        inputPort.setHint("Port");
//
//        builder.setView(inputHost);
//        builder.setView(inputPort);
//
//        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                String host = inputHost.getText().toString();
//                int port = Integer.parseInt(inputPort.getText().toString());
//                setProxy(host, port);
//            }
//        });
//
//        builder.show();
//    }

    private void setProxy(String host, int port) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Handle the case where the user hasn't granted the location permission.
            // You may choose to request the permission or display a message to the user.
            return;
        }

        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();

        for (WifiConfiguration config : configuredNetworks) {
            if (config.BSSID.equals(wifiInfo.getBSSID())) {
                try {
                    Class<?> proxyInfoClass = Class.forName("android.net.ProxyInfo");
                    Constructor<?> constructor = proxyInfoClass.getConstructor(String.class, int.class, String.class);
                    Object proxyInfo = constructor.newInstance(host, port, null);

                    Method setHttpProxy = WifiConfiguration.class.getDeclaredMethod("setHttpProxy", proxyInfoClass);
                    setHttpProxy.invoke(config, proxyInfo);

                    Method save = wifiManager.getClass().getMethod("saveConfiguration");
                    save.invoke(wifiManager);

                    wifiManager.disconnect();
                    wifiManager.reconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }


}
