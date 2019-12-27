package com.example.user.phase_wifi_direct;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;

public class WiFiDirectBroadcastTwoWay extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WiFiTwoWay mService;


    public WiFiDirectBroadcastTwoWay(WifiP2pManager mManager, WifiP2pManager.Channel mChannel, WiFiTwoWay mService) {
        this.mManager = mManager;
        this.mChannel = mChannel;
        this.mService = mService;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
            // Log.d("In here","yuh peers");

            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
            if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                Toast.makeText(context,"Wifi is ON",Toast.LENGTH_SHORT).show();

            }else{
                Toast.makeText(context,"Wifi is OFF",Toast.LENGTH_SHORT).show();

            }
        }else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
            if(mManager!=null){
                Log.d("Request","Request peers");
                // Toast.makeText(context,"peers connected",Toast.LENGTH_SHORT).show();
                mManager.requestPeers(mChannel,mService.peerListListener);

            }
        } else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            if(mManager==null){
                //  Toast.makeText(context,"Device diconnected",Toast.LENGTH_SHORT).show();
                Log.d("see","change");
                return;

            }

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if(networkInfo.isConnected()){
                //    mManager.requestConnectionInfo(mChannel,mService.connectionInfoListener);
            }else{

            }

        }else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
            //   String device_name= "null";
            WifiP2pDevice myDevice =(WifiP2pDevice)intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            Log.d("Wifi Direct: My Device",myDevice.deviceName);
            String device_name=myDevice.deviceName;
        }


        if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(intent.getAction()))
        {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, 10000);
            if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED)
            {
                // Wifi P2P discovery started.
                Toast.makeText(context,"Discovery started",Toast.LENGTH_SHORT).show();
            }
            else if(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED){
                Toast.makeText(context,"Discovery stopped",Toast.LENGTH_SHORT).show();
                //  context.startService(new Intent(context, WiFiTwoWay.class));
            }
            else
            {
                // Wifi P2P discovery stopped.
                // Do what you want to do when discovery stopped
                Toast.makeText(context,"Discovery stopped",Toast.LENGTH_SHORT).show();

            }
        }



    }



}


