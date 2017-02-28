package com.aware.plugin.survey;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.survey.survey.ConfigFile;
import com.aware.plugin.survey.survey.Trigger;
import com.aware.plugin.survey.survey.TriggerTime;
import com.aware.ui.PermissionsHandler;
import com.aware.utils.Aware_Plugin;

import java.util.List;

public class Plugin extends Aware_Plugin {

    private static boolean SURVEY_DELIVERED;

    private static String s;
    @Override
    public void onCreate() {
        super.onCreate();
        SURVEY_DELIVERED = false;

        TAG = "AWARE::"+ getResources().getString(R.string.app_name);

        /**
         * Plugins share their current status, i.e., context using this method.
         * This method is called automatically when triggering
         * {@link Aware#ACTION_AWARE_CURRENT_CONTEXT}
         **/
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                //Broadcast your context here
            }
        };

        //Add permissions you need (Android M+).
        //By default, AWARE asks access to the #Manifest.permission.WRITE_EXTERNAL_STORAGE

        //REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        //To sync data to the server, you'll need to set this variables from your ContentProvider
        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Provider.TableOne_Data.CONTENT_URI }; //this syncs dummy TableOne_Data to server


        IntentFilter contextFilter = new IntentFilter();
        //Check the sensor/plugin documentation for specific context broadcasts.
        contextFilter.addAction(Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND);
        registerReceiver(contextReceiver, contextFilter);

        System.out.println("..............................................................");

        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, false);
        ConfigFile cf = new ConfigFile(this);
        List<Trigger> triggerList = cf.getTriggers();
        TriggerTime s = (TriggerTime) triggerList.get(0);
        for (Trigger t : triggerList) {
            System.out.println(t.esm + " " + t.trigger);
        }


        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);
        s.setESM();

        //Activate plugin -- do this ALWAYS as the last thing (this will restart your own plugin and apply the settings)
        Aware.startPlugin(this, "com.aware.plugin.survey");
    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//
//        boolean permissions_ok = true;
//        for (String p : REQUIRED_PERMISSIONS) {
//            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
//                permissions_ok = false;
//                break;
//            }
//        }
//
//        if (permissions_ok) {
//            //Check if the user has toggled the debug messages
//            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");
//
//            //Initialize our plugin's settings
//            Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, true);
//
//        } else {
//            Intent permissions = new Intent(this, PermissionsHandler.class);
//            permissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
//            permissions.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(permissions);
//        }
//
//        return super.onStartCommand(intent, flags, startId);
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, false);

        //Stop AWARE's instance running inside the plugin package
        Aware.stopAWARE();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        boolean permissions_ok = true;
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                permissions_ok = false;
                break;
            }
        }

        if (permissions_ok) {

            //Ask AWARE to start ESM
//            Log.d("SET", Boolean.toString(APPLICATION_OPEN));



//            for (Trigger x : cf.getTriggers()) {
//                System.out.println(x.trigger);
//            }

//            InputStream inputStream = getResources().openRawResource(R.raw.esm1);
//            ConfigFile csvFile = new ConfigFile(inputStream);
//            String scoreList = csvFile.read();

            //System.out.println(scoreList);
//Define the ESM to be displayed
//            String esmString = "[{'esm':{'esm_type':"+ESM.TYPE_ESM_TEXT+",'esm_title':'ESM Freetext'," +
//                    "'esm_instructions':'The user can answer an open ended question.','" +
//                    "esm_submit':'Next','esm_expiration_threshold':60,'esm_trigger':'AWARE Tester'}}]";
//
//            esmString = scoreList;

//Queue the ESM to be displayed when possible
//            Intent esm = new Intent(ESM.ACTION_AWARE_QUEUE_ESM);
//            esm.putExtra(ESM.EXTRA_ESM, esmString);
            //sendBroadcast(esm);

//            s = scoreList;


            //Setting morning question

            //Setting evening question
//            try {
//                //Using Likert scale to get users' rating of the day
//                ESMFactory esmFactory = new ESMFactory();
//
//                ESM_Likert evening_question = new ESM_Likert();
//                evening_question.setLikertMax(5)
//                        .setLikertMinLabel("Awful")
//                        .setLikertMaxLabel("Awesome!")
//                        .setLikertStep(1)
//                        .setTitle("Evening!")
//                        .setInstructions("How would you rate today?")
//                        .setExpirationThreshold(0) //no expiration = shows a notification the user can use to answer at any time
//                        .setNotificationTimeout(5 * 60) //the notification is automatically removed and the questionnaire expired after 5 minutes ( 5 * 60 seconds)
//                        .setSubmitButton("OK");
//
//                esmFactory.addESM(evening_question);
//
//                ESM.queueESM(this, esmFactory.build());
//
//                //Schedule this question for the evening, only if not yet defined
//                Scheduler.Schedule evening = Scheduler.getSchedule(this, "evening_question");
//                if (evening == null) {
//                    evening = new Scheduler.Schedule("evening_question"); //schedule with morning_question as ID
//                    //evening.addHour(20); //8 PM (24h format), every day
//                    evening.setInterval(1);
//                    evening.setActionType(Scheduler.ACTION_TYPE_BROADCAST); //sending a request to the client via broadcast
//                    evening.setActionClass(ESM.ACTION_AWARE_QUEUE_ESM); //with the action of ACTION_AWARE_QUEUE_ESM, i.e., queueing a new ESM
//                    evening.addActionExtra(ESM.EXTRA_ESM, esmFactory.build()); //add the questions from the factory
//
//                    Scheduler.saveSchedule(this, evening); //save the questionnaire and schedule it
//                }
//
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            ExecuteTx tx = new ExecuteTx(this);
//            tx.start();

        } else {
            Intent permissions = new Intent(this, PermissionsHandler.class);
            permissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
            permissions.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(permissions);
        }

//        Cursor sensorData = getContentResolver().query(Applications_Provider.Applications_Foreground.CONTENT_URI,
//                new String[]{""}, "", new String[]{""}, "");
//        sensorData.getString(1);

        return super.onStartCommand(intent, flags, startId);
    }

    class ExecuteTx extends Thread {

        Plugin p;

        ExecuteTx(Plugin p) {
            this.p = p;
        }

        public void run() {
//            while (true) {
//                try {
//                    //printForegroundTask();
//                    Thread.sleep(10000);
//
//                    Process logcat;
//                    final StringBuilder log = new StringBuilder();
//                    try {
//                        logcat = Runtime.getRuntime().exec(new String[]{"logcat", "-d"});
//                        BufferedReader br = new BufferedReader(new InputStreamReader(logcat.getInputStream()),4*1024);
//                        String line;
//                        String separator = System.getProperty("line.separator");
//                        while ((line = br.readLine()) != null) {
//                            System.out.println(line);
//                            log.append(line);
//                            log.append(separator);
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//
//
//
//                    try {
//                        Runtime.getRuntime().exec(new String[]{"logcat", "-c"});
//                    } catch (Exception e1) {
//                        e1.printStackTrace();
//                    }
//
//                } catch (Exception e) {
//
//                }
//            }
//            while (true) {
//                System.out.println(getApplicationName(p.getPackageName()));
//                Cursor sensorData = getContentResolver().
//                        query(Applications_Provider.Applications_Foreground.CONTENT_URI,
//                                null, null, null, null);
//
//                for (int i = 0; i < 5; i++) {
//                    System.out.println(sensorData.getString(i));
//                }
//
//                //System.out.println(sensorData.getString(4) + " " + sensorData.getString(3) + " " + sensorData.getString(2));
//                sensorData.close();
////                System.out.println(APPLICATION_OPEN);
////                if (APPLICATION_OPEN ) {
////                    sendBroadcast(esm);
////                }
//            }
        }
    }


//    private void deliverSurvey() {
//        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");
//
//        //Initialize our plugin's settings
//        Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, true);
//        Aware.setSetting(this, Aware_Preferences.STATUS_ESM, true);
//        Aware.startESM(this);
//        try {
//            Log.d("STR", Boolean.toString(APPLICATION_OPEN));
//            ESMFactory factory = new ESMFactory();
//
//            //define ESM question
//            Scheduler.Schedule sch = Scheduler.getSchedule(this, "freetext");
//            ESM_Freetext esmFreetext = new ESM_Freetext();
//            esmFreetext.setTitle("Freetext")
//                    .setTrigger("an esm queue from AWARE")
//                    .setSubmitButton("OK")
//                    .setInstructions("Open-ended text input");
//
//            //add them to the factory
//            factory.addESM(esmFreetext);
//
//            ESMFactory esmFactory = new ESMFactory();
//
//            ESM.queueESM(this, factory.build());
//
//            ESM_Likert evening_question = new ESM_Likert();
//            evening_question.setLikertMax(5)
//                    .setLikertMinLabel("Awful")
//                    .setLikertMaxLabel("Awesome!")
//                    .setLikertStep(1)
//                    .setTitle("Evening!")
//                    .setInstructions("How would you rate today?")
//                    .setExpirationThreshold(0) //no expiration = shows a notification the user can use to answer at any time
//                    .setNotificationTimeout(5 * 60) //the notification is automatically removed and the questionnaire expired after 5 minutes ( 5 * 60 seconds)
//                    .setSubmitButton("OK");
//
//            esmFactory.addESM(evening_question);
//
//            //Queue them
//            ESM.queueESM(this, esmFactory.build());
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }

    private String getApplicationName(String package_name) {
        PackageManager packageManager = getPackageManager();
        ApplicationInfo appInfo;
        try {
            appInfo = packageManager.getApplicationInfo(package_name, PackageManager.GET_META_DATA);
        } catch (final PackageManager.NameNotFoundException e) {
            appInfo = null;
        }
        String appName = "";
        if (appInfo != null && packageManager.getApplicationLabel(appInfo) != null) {
            appName = (String) packageManager.getApplicationLabel(appInfo);
        }
        return appName;
    }


    private static ContextReceiver contextReceiver = new ContextReceiver();
    public static class ContextReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
        //Action for context here.

//            Uri applicationUri = Applications_Provider.Applications_Foreground.CONTENT_URI;
//            String i = Aware.getSetting(context, Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND);
//            String j = Aware.getSetting(context, Aware_Preferences.DEVICE_ID);
//            String k = Aware.getSetting(context, applicationUri.getQueryParameter("application_name"));
//            String m = Aware.getSetting(context, Applications_Provider.Applications_Foreground.APPLICATION_NAME);
//            Log.d("CONNECTION", "->" + Aware.getSetting(context, Applications_Provider.Applications_Foreground.TIMESTAMP));
//            //CONTENT_URI is the sensor's or plugin's table URI
//
//            Log.d("REC i:" +i + " j:" + j + " k:"+ k + " m:" + m, Boolean.toString(APPLICATION_OPEN));
//            Object t = intent.getExtras().get("rowData");

            //System.out.println(context.getApplicationInfo().processName + context.);
            //context.getApplicationInfo().

                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        for (String key : bundle.keySet()) {
                            Object value = bundle.get(key);
                            Log.d(TAG, String.format("K: %s V: %s c: (%s)", key,
                                    value.toString(), value.getClass().getName()));
                            System.out.println("Key: "+key+" VAL: "+value.toString()+" "+value.getClass().getName());
                            if(value instanceof ContentValues) {
                                ContentValues val = (ContentValues) value;
                                System.out.println("VALS: " + val.toString());
                                System.out.println("APP: " + val.get("application_name"));
                                String currentApp = (String)val.get("application_name");
                                long timestamp = System.currentTimeMillis();

                                Log.d(">>>>>>>" + currentApp, currentApp);

//                                //Share context to broadcast and send to database
//                                if (Plugin.pluginContext != null)
//                                    Plugin.pluginContext.onContext();

                            }
                        }
                    }


            deliverSurvey();
            if (!SURVEY_DELIVERED) {
                Aware.setSetting(context, Aware_Preferences.STATUS_ESM, true);
                //ESM.queueESM(context, s);
                SURVEY_DELIVERED = true;
            }


        }
    }

    private static void deliverSurvey() {

    }

}
