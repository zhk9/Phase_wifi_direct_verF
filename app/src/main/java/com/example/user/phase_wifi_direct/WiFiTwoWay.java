package com.example.user.phase_wifi_direct;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.rvalerio.fgchecker.AppChecker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


import android.location.LocationManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class WiFiTwoWay extends Service {

    private WifiManager.WifiLock wifiLock;
    String ACTIVITY_TAG = getClass().getSimpleName();


    WifiManager wifiManager;

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    long screentime = 0;
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;
    static String appName = "12345";
    static int count = 0;
    String pacName = "null";
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001;
    public static final String CHANNEL_1_ID = "channel1";
    public static final String CHANNEL_2_ID = "channel2";
    public static final String CHANNEL_3_ID = "channel3";

//    private static final int PEER_CONNECTION_USER_ACCEPT =   BASE + 6;

    static PendingIntent pendingIntent;
    static PendingIntent pendingIntent_cancel;


    private List<String> listPackageName = new ArrayList<>();
    private List<String> listAppName = new ArrayList<>();
    private Timer timer = new Timer();
    private android.os.Handler handler = new android.os.Handler();

    private AppChecker appChecker;
    String current = "NULL";
    String previous = "NULL";
    String timeleft = "NULL";
    String current_app = "NULL";

    Boolean isConnected = false;
    long startTime = 0;
    long previousStartTime = 0;
    long endTime = 0;
    long totlaTime = 0;
    long screen_On = 0;
    long screen_Off = 0;
    long screen_active = 0;
    long notif_time = 0;
    long notif_tot = 0;
    long notif_time_start = 0;
    private long screenOnTime;
    private long screen_Time;
    private final long TIME_ERROR = 1000;

    double longitude;
    double latitude;
    double elevation;
//    LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    //   private LocationListener mLocationListener;
    //   private final LocationServiceBinder binder = new LocationServiceBinder();


    boolean notif_chec = true;

    @Override
    public void onCreate() {
        super.onCreate();

        startChecker();
        Intent intent5 = new Intent(this, MainActivity.class);
        intent5.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        pendingIntent = PendingIntent.getActivity(this, 0, intent5, 0);
      //  buildNotification();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("use", "new ");
          //  startMyOwnForeground();
            buildNotification();

        } else {
            Log.d("use", "old");
            startForeground(1, new Notification());

        }
        Log.d("fafa", "WifiTwoWay made");


        initialWork();
        tim();
      //  loca();
          requestLocationUpdates();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        exqListener();

                    }
                });
            }
        }, 0, 20000);


//        LocationManger lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);


     //   Updates for location every 1 minute

//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                handler.post(new Runnable() {
//                    @Override
//                    public void run() {
//
//                       requestLocationUpdates();
//
//                    }
//                });
//            }
//        }, 0, 60000);

        registerReceiver(mReceiver, mIntentFilter);
        appName = "12345";
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        installedapp();

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                retrieveStats();
                repeat_notif();
            }
        }, 0, 900000);

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "MyWifiLock");
        wifiLock.acquire();

    }

    private void startMyOwnForeground() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
            String channelName = "My Background Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.noti)
                    .setContentTitle("App is running in background")
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();
            startForeground(2, notification);

        }
    }




    private void buildNotification() {
        String stop = "stop";
        registerReceiver(stopReceiver, new IntentFilter(stop));
        PendingIntent broadcastIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(stop), PendingIntent.FLAG_UPDATE_CURRENT);

// Create the persistent notification//
        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.tracking_enabled_notif))

//Make this notification ongoing so it can’t be dismissed by the user//

                .setOngoing(true)
                .setContentIntent(broadcastIntent)
                .setSmallIcon(R.drawable.tracking_enabled);
        startForeground(12345678, getNotification());
    }


    @TargetApi(Build.VERSION_CODES.O)
    private Notification getNotification() {


        NotificationChannel channel = new NotificationChannel("channel_01", "My Channel", NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification.Builder builder = new Notification.Builder(getApplicationContext(), "channel_01").setAutoCancel(true);
        return builder.build();
    }


    protected BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

//Unregister the BroadcastReceiver when the notification is tapped//

            unregisterReceiver(stopReceiver);

//Stop the Service//

            stopSelf();
        }
    };

    private void createNotification(String chanid, String channame, String title, String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String NOTIFICATION_CHANNEL_ID = chanid;
            String channelName = channame;
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);


            Intent intent_cancel = new Intent(this, MainActivity.class);
            intent_cancel.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            pendingIntent_cancel = PendingIntent.getActivity(this, 0, intent_cancel, 0);

            notif_time = System.currentTimeMillis();
            Log.d("pen", "clutch" + notif_time);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder
                    //.setOngoing(false)
                    .setSmallIcon(R.drawable.noti)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setTimeoutAfter(20000)
                    .build();
            manager.notify(1, notification);


        }
    }

    @Override
    public void onDestroy() {
        Log.d("Tis", "WE have destroyed service");
        super.onDestroy();
        mManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d("cancel", "Success");
                unregisterReceiver(mReceiver);

            }

            @Override
            public void onFailure(int reason) {
                Log.d("cancel", "Failure");


            }
        });
        System.exit(0);

        if (wifiLock != null) {
            if (wifiLock.isHeld()) {
                wifiLock.release();
            }
        }
    }


    public void disconnect() {
        Log.d("Discone", "In the dicsone");
        if (mManager != null && mChannel != null) {
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {

                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && mManager != null && mChannel != null
                            && group.isGroupOwner()) {
                        Log.d("Removal", "Removing group");
                        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d("Removal", "removeGroup onSuccess -");

                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d("Removal", "removeGroup onFailure -" + reason);
                                //       Toast.makeText(getApplicationContext(), "No Device Found", Toast.LENGTH_SHORT).show();

                            }
                        });
                    }
                }
            });

        }
    }


    private void exqListener() {
        KeyguardManager myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (myKM.inKeyguardRestrictedInputMode()) {
            if (mManager != null) {


                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.d("conn", "disc");
                        //   createNotification(CHANNEL_1_ID, "Discovery", "Discovering Peers", "Tap to reopen App");
                        //      Toast.makeText(getApplicationContext(), "Discovering for peers", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d("conn", "failed");
                        // createNotification(CHANNEL_1_ID, "Discovery", "Failed to Discover Peers", "Tap to reopen App");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            getApplicationContext().startForegroundService(new Intent(getApplicationContext(), WiFiTwoWay.class));
                        } else {
                            getApplicationContext().startService(new Intent(getApplicationContext(), MainActivity.class));

                        }

                    }
                });
            }
        } else {
            if (mManager != null) {


                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.d("conn", "disc");
                        //   createNotification(CHANNEL_1_ID, "Discovery", "Discovering Peers", "Tap to reopen App");
                        //      Toast.makeText(getApplicationContext(), "Discovering for peers", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d("conn", "failed");
                        // createNotification(CHANNEL_1_ID, "Discovery", "Failed to Discover Peers", "Tap to reopen App");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            getApplicationContext().startForegroundService(new Intent(getApplicationContext(), WiFiTwoWay.class));
                        } else {
                            getApplicationContext().startService(new Intent(getApplicationContext(), MainActivity.class));

                        }

                    }
                });
            }
        }
    }

    private void retrieveStats() {


    }

    private void initialWork() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        mReceiver = new WiFiDirectBroadcastTwoWay(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    }

    //Looking for peers:

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {

        @Override
        public void onPeersAvailable(final WifiP2pDeviceList peerList) {
            Log.d("Peers", "Getting Peers");
            if (count != 1) {
                if (!peerList.getDeviceList().equals(peers)) {
                    peers.clear();
                    peers.addAll(peerList.getDeviceList());


                    deviceNameArray = new String[peerList.getDeviceList().size()];

                }

                if (peerList.getDeviceList().size() != 0) {

                    deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];
                    int index = 0;


                    for (WifiP2pDevice devices : peerList.getDeviceList()) {
                        deviceNameArray[0] = devices.deviceName;
                        deviceArray[0] = devices;
                        Log.d("WTH", devices.deviceName);
                        //  [Phone] Galaxy S8 [Phone] Dipa [Phone] Hubby  PIXEL 3A  Toast.makeText(getApplicationContext(), "Device nearby is: " +devices.deviceName, Toast.LENGTH_SHORT).show();
                        if (devices.deviceName.equals("PIXEL 3A")) {
                            isConnected = true;
                            String date_dev = String.valueOf(android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", new java.util.Date()));
                            Log.d("dev", "patel" + date_dev);
                            //     Toast.makeText(getApplicationContext(), "Device nearby is: " +devices.deviceName, Toast.LENGTH_SHORT).show();
                            Log.d("sanam", "karam");
                            //GET TIMEERRR //CUNTDOWN TIMER
                        } else {
                            isConnected = false;
                        }

                        if (isConnected) {
                            //  locked_check();
                            //   tim_loc();
                            KeyguardManager myKM2 = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                            if (myKM2.inKeyguardRestrictedInputMode()) {
                                Log.d("Locked", "Good" + screen_Off);
                                screen_Off = System.currentTimeMillis();
                                notif_chec = true;

                            } else {
                                screen_On = System.currentTimeMillis();
                                if (screen_On != 0 && screen_Off != screen_Time) {
                                    screen_active = screen_On - screen_Off;
                                    Log.d("samZ ", "letsZ " + screen_active + "\t" + screen_On + "\t" + screen_Off);
                                }

                                if ((screen_active > (900000)) && notif_chec) {
                                    //    Toast.makeText(getApplicationContext(), "STOP!!", Toast.LENGTH_SHORT).show();
                                    // notifyThis("Phone usage","Take a break");

//                                        createNotification(CHANNEL_1_ID, "App Usage Increased", "Communication is key to healthy relationship", "Take a break from your device and talk to each other");
                                    createNotification(CHANNEL_1_ID, "App Usage Increased", Quote.getRandomQuoteTitle(), "Take a break from your device and talk to each other");


                                    //  screen_Off=0;
//                                        notif_tot=10;
                                    notif_chec = false;
                                    new QuoteOfTheDay();

                                    notif_time_start = System.currentTimeMillis();
                                    notif_tot = notif_time - notif_time_start;
                                    Log.d("pen2", "clutch2" + notif_time + "\t" + notif_time_start + "\t" + notif_tot);

//
                                }

//
                                AppChecker appChecker = new AppChecker();
                                current_app = appChecker.getForegroundApp(getBaseContext());
                                if (current_app != null) {
                                    screentime = System.currentTimeMillis();
                                    if (screentime != 0 && startTime != screenOnTime) {
                                        endTime = screentime - startTime;
                                        Log.d("Glass", "TISS " + current_app + " App time" + endTime + "\t" + startTime + "\t" + screentime);
                                    }
                                }


                                if (current_app != null) {

//                                        if(screen_active>300000){
//                                        createNotification(CHANNEL_2_ID, "App Usage Increased", "Stop!!", "Tap to reopen App");
//
//                                    }

//                                        if(endTime>60000 && !current_app.equals("com.sec.android.app.launcher") ){
//                                            // 5 MINYTES : 300000
//                                            createNotification(CHANNEL_1_ID, "App Usage Increased", "Let's Talk", "Tap to reopen App");
//                                            //  Toast.makeText(getApplicationContext(), "STOP!!", Toast.LENGTH_SHORT).show();
//                                            Log.d("jet", String.valueOf(endTime));
////                        Intent notifi = new Intent(getBaseContext(),NotificationReceive.class);
////                    //    intent.putExtra("Technique",techniqueAsk);
////                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////                        startActivity(notifi);
//                                            //startservice(notif):
//                                        }
                                }
//                                    if(endTime>60000 && !current_app.equals("com.google.android.apps.nexuslauncher") ){
//                                        // 5 MINYTES : 300000
//                                        createNotification(CHANNEL_1_ID, "App Usage Increased", "Let's Talk", "Tap to reopen App");
//                                      //  Toast.makeText(getApplicationContext(), "STOP!!", Toast.LENGTH_SHORT).show();
//                                        Log.d("jet", String.valueOf(endTime));
////                        Intent notifi = new Intent(getBaseContext(),NotificationReceive.class);
////                    //    intent.putExtra("Technique",techniqueAsk);
////                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////                        startActivity(notifi);
//                                        //startservice(notif):
//                                    }
                            }
                        }
                    }
//                        if(!notif_chec && screen_active>300000){
//                            createNotification(CHANNEL_2_ID, "App Usage Increased", "Stop!!", "Tap to reopen App");
//                        }


                    if (peers.size() == 0) {
                        //    Toast.makeText(getApplicationContext(), "No pairs available", Toast.LENGTH_SHORT).show();
                        return;
                        //    createNotification(CHANNEL_3_ID, "Connection", "NO device found", "Tap to reopen App");
                    }


                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray);
                    //   createNotification(CHANNEL_3_ID, "Device", "Connected to the device", deviceArray[0].toString());
                    Log.d("part", deviceArray[0].toString());
                    final WifiP2pDevice device = deviceArray[0];
                    final WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = device.deviceAddress;

                }

            }


            if (peerList.getDeviceList().size() == 0) {
                // Toast.makeText(getApplicationContext(), "No Device Found", Toast.LENGTH_SHORT).show();
                //  createNotification(CHANNEL_3_ID, "Connection", "NO device found", "Tap to reopen App");
                disconnect();
            }
        }


    };

    //Repeating notification

    public void repeat_notif() {
        if (!notif_chec && screen_active > 1740000) {
            createNotification(CHANNEL_2_ID, "App Usage Increased", Quote.getRandomQuoteTitle(), "Take a break from your device and talk to each other");
        }
    }

    //NAME OF THE INSTALLED APPLICATION


    public void installedapp() {
        List<PackageInfo> packageList = getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packageList.size(); i++) {
            PackageInfo packageInfo = packageList.get(i);

            String appName = packageInfo.applicationInfo.loadLabel(getPackageManager()).toString();
            pacName = packageInfo.packageName;

            listAppName.add(appName);
            listPackageName.add(pacName);


            Log.e("APPNAME", "app is " + appName + "----" + pacName + "\n");

            String app = appName + "\t" + pacName + "\t" + "\n";


            try {
                File data3 = new File("appname.txt");
//                if(data3.exists()){
//                    data3.delete();
//                }
//                else{
//
//                    //  FileOutputStream fos = openFileOutput("appname.txt", Context.MODE_APPEND);
//                    FileOutputStream fos = new FileOutputStream(data3, false);
//                    fos.write((app).getBytes());
//                    fos.close();
////                FileWriter fw =new FileWriter("appname.txt", false);
////                fw.write(app);
////                fw.close();
//
//                }
                FileOutputStream fos = openFileOutput("appname.txt", Context.MODE_APPEND);
               //    FileOutputStream fos = new FileOutputStream(data3, false);
                fos.write((app).getBytes());
                fos.close();
//                FileWriter fw =new FileWriter("appname.txt", false);
//                fw.write(app);
//                fw.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }

    //checking the current running application after 5 seconds:


    private void startChecker() {

        appChecker = new AppChecker();
        appChecker
                .when(getPackageName(), new AppChecker.Listener() {
                    @Override
                    public void onForeground(String packageName) {
                        //  Toast.makeText(getBaseContext(), "Our app is in the foreground.", Toast.LENGTH_SHORT).show();
                    }
                })
                .whenOther(new AppChecker.Listener() {
                    @Override
                    public void onForeground(String packageName) {
                        //    Toast.makeText(getBaseContext(), "Foreground: " + packageName, Toast.LENGTH_SHORT).show();
                        Log.d("finalZ1", packageName);
                        int index = listPackageName.indexOf(packageName);
                        appName = listAppName.get(index);
                        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//
//
                        KeyguardManager myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                        if (myKM.inKeyguardRestrictedInputMode()) {

//                Log.e("APPLICATION8", "Current App in foreground is: " + currentApp);
//
                                String date = String.valueOf(android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", new java.util.Date()));
                                String curr = date + "\t" + latitude+"\t" + longitude + "\t" +  packageName + "\t" + appName + "\n";
//                //  if (notifications.equals(0)) {
                                try {
                                    File data2 = new File("details_locked.txt");
                                    FileOutputStream fos = openFileOutput("details_locked.txt", Context.MODE_APPEND);
                                    fos.write((curr).getBytes());
                                    fos.close();
                                } catch (FileNotFoundException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }

                        } else {

//                Log.e("APPLICATION8", "Current App in foreground is: " + currentApp);
                                String date = String.valueOf(android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", new java.util.Date()));
                                String curr = date + "\t"+ latitude+"\t" + longitude + "\t"  + packageName + "\t" + appName + "\n";
                                try {
                                    File data2 = new File("details_unlocked.txt");
                                    FileOutputStream fos = openFileOutput("details_unlocked.txt", Context.MODE_APPEND);
                                    fos.write((curr).getBytes());
                                    fos.close();
                                } catch (FileNotFoundException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }

                        }
                    }
                })
                .timeout(1000)
                .start(this);


    }

    //Checking the aggregation of usage:

    public void aggregationapp() {
        String lastknown = "NULL";
        String appName = "NULL";
        String previous1 = "NULL";
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
        Date systemDate = Calendar.getInstance().getTime();
        String myDate = sdf.format(systemDate);
        AppChecker appChecker = new AppChecker();
        current = appChecker.getForegroundApp(getBaseContext());

        java.text.DateFormat df = new java.text.SimpleDateFormat("hh:mm:ss");
        {
            if (current != null) {
                if (!current.equals(previous)) {
                    Log.d("panda", "zebra" + previous);
                    Log.d("side", "dish" + current);
                    Log.d("tims", "Horton" + myDate);

                    //  previous = appChecker.getForegroundApp(getBaseContext());
                    startTime = System.currentTimeMillis();


//
                    int index = listPackageName.indexOf(previous);
                    if (index < 0) {
                        appName = "Null";
                    } else {
                        appName = listAppName.get(index);
                    }


                    if (startTime != previousStartTime && previousStartTime != 0) {
                        totlaTime = 0;

                        totlaTime = startTime - previousStartTime;

                        //  totlaTime=previousStartTime-startTime;
//
                    }

                    Log.d("FinalZ2", "app name " + previous + " App time" + totlaTime + "\t" + previousStartTime + "\t" + startTime);
//
                    LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                            // TODO: Consider calling
//                            //    ActivityCompat#requestPermissions
//                            // here to request the missing permissions, and then overriding
//                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                            //                                          int[] grantResults)
//                            // to handle the case where the user grants the permission. See the documentation
//                            // for ActivityCompat#requestPermissions for more details.
//                            return;
//                        }
//
//                        // Added to chcke if the phone is locked vs unlocked//
//
                    String status = "NULL";
                    KeyguardManager myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                    if (myKM.inKeyguardRestrictedInputMode()) {
                        status = "locked";
                    } else {
                        status = "unlocked";
                    }
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    if (!current.equals("NULL")) {

                            String date = String.valueOf(android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", new java.util.Date()));
                            String appt = date + "\t"+ latitude+"\t" + longitude + "\t" + previous + "\t" + appName + "\t" + totlaTime + "\t" + status + "\n";
                            try {
                                File data7 = new File("individual.txt");
                                FileOutputStream fos = openFileOutput("individual.txt", Context.MODE_APPEND);
                                fos.write((appt).getBytes());
                                fos.close();
                            } catch (FileNotFoundException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                            previousStartTime = startTime;

                    }
                } else if (current.equals(previous)) {
                    Log.d("Birds", "crow" + lastknown);
                }
                previous = current;

                Log.d("zoo", "animals" + previous);

            }

        }

    }

    //calling aggregation of the app usage

    public void tim(){
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        aggregationapp();




                    }
                });
            }
        }, 0, 1000);
    }







//Location Updates

//    private void requestLocationUpdates() {
//        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//        if (location!=null){
//             longitude = location.getLongitude();
//             latitude = location.getLatitude();
//             elevation= location.getAltitude();
//            String date = String.valueOf(android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", new java.util.Date()));
//            String data1 = date + "\t" + latitude + "\t" + longitude + "\t" + elevation + "\n";
//
//            try {
//                File file1 = new File("gps_coordinates.txt");
//                FileOutputStream fos = openFileOutput("gps_coordinates.txt", Context.MODE_APPEND);
//                fos.write((data1).getBytes());
//                fos.close();
//            } catch (FileNotFoundException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//
//        }
//
//    }

    private void requestLocationUpdates() {


        LocationRequest request = new LocationRequest();


//Specify how often your app should request the device’s location//

       // request.setInterval(900000);
        //request.setInterval(1000);
      //  request.setInterval(60000);
      //  request.setInterval(1200000);
        request.setInterval(1800000);

//Get the most accurate location data available//

        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        //   final String path = getString(R.string.firebase_path);
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

//If the app currently has access to the location permission...//

        if (permission == PackageManager.PERMISSION_GRANTED) {

//...then request location updates//


            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {

                    android.location.Location location = locationResult.getLastLocation();
                    if (location != null) {
                        String date = String.valueOf(android.text.format.DateFormat.format("dd/MM/yy HH:mm:ss", new java.util.Date()));
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
//                        double elevation= location.getAltitude();
                        String loc = date + "\t" + latitude + "\t" + longitude + "\t"  + "\n";
                        try {
                            File data = new File("GPS.txt");
                            FileOutputStream fos = openFileOutput("GPS.txt", Context.MODE_APPEND);
                            fos.write((loc).getBytes());
                            fos.close();
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

//
                    }
                }
            }, null);
        }
    }




        private static class Quote {
        private static final String[]  QUOTES = {
                "Communication is key to healthy relationship",
                "A great relation has great communication",
                "Relationship cannot grow without communication",
                "Communication is the fuel that keeps the fire of your relationship burning ",
                "Communication is the lifeline of any relationship.",
                "Communication is to relationships what breath is to life.",
                "Communication is your ticket to successful relation",
                "Communication is the first pillar of love.",
//                "Life isn't about getting and having, it's about giving and being. –Kevin Kruse",
//                "Whatever the mind of man can conceive and believe, it can achieve. –Napoleon Hill",
//                "Strive not to be a success, but rather to be of value. –Albert Einstein",
        };

        public static String getRandomQuoteTitle() {
            int randomIndex = ((int) Math.round(Math.random() * Quote.QUOTES.length));
            randomIndex = (randomIndex < 0) ? 0 : ((randomIndex >= Quote.QUOTES.length) ? Quote.QUOTES.length - 1 : randomIndex);
            return Quote.QUOTES[randomIndex];
        }
    }

    public class QuoteOfTheDay {

        public void main(String[] args) {
            System.out.println();

            System.out.println(Quote.QUOTES[(int) Math.round(Math.random() * Quote.QUOTES.length)]);
        }
    }



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
