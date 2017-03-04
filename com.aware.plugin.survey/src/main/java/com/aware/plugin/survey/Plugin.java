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
import android.widget.Toast;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.plugin.survey.survey.ConfigFile;
import com.aware.plugin.survey.survey.Trigger;
import com.aware.plugin.survey.survey.TriggerAppOpenClose;
import com.aware.plugin.survey.survey.TriggerTime;
import com.aware.providers.Scheduler_Provider;
import com.aware.ui.ESM_Queue;
import com.aware.ui.PermissionsHandler;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Main plugin functionalities.
 *
 * @author  Seng Leung
 * @version 1.0
 */
public class Plugin extends Aware_Plugin {

    private boolean appTriggered;
    private static List<Trigger> triggerList;
    private static Queue<String> prevApps;
    private static final int PREVIOUS_APP_SIZE = 4;

    private static ContextProducer pluginContext;
    private static String currentApp;
    private static long timestamp;
    private static String surveyTrigger;
    private static String surveyAnswers;
    
    @Override
    public void onCreate() {
        super.onCreate();
        prevApps = new LinkedList<>();

        TAG = "AWARE::"+ getResources().getString(R.string.app_name);

        /**
         * Plugins share their current status, i.e., context using this method.
         * This method is called automatically when triggering
         * {@link Aware#ACTION_AWARE_CURRENT_CONTEXT}
         **/
        pluginContext = new ContextProducer() {
            @Override
            public void onContext() {
                //Save rowData in database 
                ContentValues rowData = new ContentValues();
                rowData.put(Provider.Plugin_Survey_Data.TIMESTAMP, 0);
                rowData.put(Provider.Plugin_Survey_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                rowData.put(Provider.Plugin_Survey_Data.NAME, currentApp);
                rowData.put(Provider.Plugin_Survey_Data.BIG_NUMBER, timestamp);
//                rowData.put(Provider.Plugin_Survey_Data.TRIGGER, surveyTrigger);
//                rowData.put(Provider.Plugin_Survey_Data.ANSWERS, surveyAnswers);
                rowData.put(Provider.Plugin_Survey_Data.TEST, surveyAnswers);

                Log.d(TAG,"Sending data "+rowData.toString());
                //Toast.makeText(getApplicationContext(), "Sending data "+currentApp, Toast.LENGTH_LONG).show();
                getContentResolver().insert(Provider.Plugin_Survey_Data.CONTENT_URI, rowData);

                //broadcast?
            }
        };
        CONTEXT_PRODUCER = pluginContext;


        //Add permissions you need (Android M+).
        //By default, AWARE asks access to the #Manifest.permission.WRITE_EXTERNAL_STORAGE

        //REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        //To sync data to the server, you'll need to set this variables from your ContentProvider
        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Provider.Plugin_Survey_Data.CONTENT_URI }; //this syncs dummy Plugin_Survey_Data to server

        Log.d("SERVER", String.valueOf(DATABASE_TABLES));
        Log.d("SERVER", String.valueOf(TABLES_FIELDS));
        Log.d("SERVER", String.valueOf(CONTEXT_URIS));

        System.out.println("Initialising triggers...");

        Scheduler.removeSchedule(getApplicationContext(),
                                 Scheduler_Provider.Scheduler_Data.SCHEDULE_ID,
                                 getPackageName()
        );

        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, false);
        ConfigFile cf = new ConfigFile(this);
        triggerList = cf.getTriggers();

        appTriggered = false;
        for (Trigger trigger : triggerList) {
            if (trigger instanceof TriggerAppOpenClose) {
                appTriggered = true;
            }
            if (trigger instanceof  TriggerTime) {
                Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);
                ((TriggerTime) trigger).setESM();
            }
        }

        if (appTriggered) {
            IntentFilter contextFilter = new IntentFilter();
            //Check the sensor/plugin documentation for specific context broadcasts.
            contextFilter.addAction(Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND);
            //TO get esm answers
            contextFilter.addAction(ESM.ACTION_AWARE_ESM_EXPIRED);
            contextFilter.addAction(ESM.ACTION_AWARE_ESM_DISMISSED);
            contextFilter.addAction(ESM.ACTION_AWARE_ESM_ANSWERED);
            contextFilter.addAction(ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE);
            registerReceiver(contextReceiver, contextFilter);
        }

        //join study !REQUIERED to sync data to server
        Aware.joinStudy(this,
                "https://api.awareframework.com/index.php/webservice/index/1118/s7VgPquEj8aM");

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
//            Aware.setSetting(this, Settings.STATUS_SURVEY_PLUGIN, true);
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

        Aware.setSetting(this, Settings.STATUS_SURVEY_PLUGIN, false);

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
//        Aware.setSetting(this, Settings.STATUS_SURVEY_PLUGIN, true);
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

    private static ContextReceiver contextReceiver = new ContextReceiver();
    public static class ContextReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND)){
                // Detect application name.
                boolean triggered = false;
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    for (String key : bundle.keySet()) {
                        Object value = bundle.get(key);
//                        Log.d(TAG, String.format("K: %s V: %s c: (%s)", key,
//                                value.toString(), value.getClass().getName()));
//                        System.out.println("Key: "+key+" VAL: "+value.toString()+" "+value.getClass().getName());
                        if (value instanceof ContentValues) {
                            ContentValues val = (ContentValues) value;
//                            System.out.println("VALS: " + val.toString());
//                            System.out.println("APP: " + val.get("application_name"));
                            currentApp = (String) val.get("application_name");
                            timestamp = System.currentTimeMillis();

                            Log.d(">>>>>>>>>>>>>>>>>>>>>>>", currentApp);


                        }
                    }
                }

                // Detect application opening.
                for (Trigger trigger : triggerList) {
                    if (trigger instanceof TriggerAppOpenClose) {

                        for (String app : ((TriggerAppOpenClose) trigger).applications) {

                            if ((app.equals(currentApp) && !prevApps.contains(currentApp)) &&
                                    ((TriggerAppOpenClose) trigger).open) {
                                Aware.setSetting(context, Aware_Preferences.STATUS_ESM, true);
                                Log.d(">>>>>>>>>>>>>>>>>>>>>>>", "TRIGGERED");
                                ESM.queueESM(context, trigger.esm);
                                triggered = true;
                                surveyTrigger = "App opend: "+currentApp;
                            }
                        }
                    }
                }

                // Update previous application queue.
                if (prevApps.size() >= PREVIOUS_APP_SIZE) {
                    prevApps.remove();
                    prevApps.add(currentApp);
                } else {
                    prevApps.add(currentApp);
                }

                Log.d(">>>>>>>>>>>>>>>>>>>>>>>", prevApps.toString());

                // Detect application closing.
                for (Trigger trigger : triggerList) {
                    if (trigger instanceof TriggerAppOpenClose) {

                        for (String app : ((TriggerAppOpenClose) trigger).applications) {

                            if (hasAppClosed(prevApps, app) && ((TriggerAppOpenClose) trigger).close) {
                                Aware.setSetting(context, Aware_Preferences.STATUS_ESM, true);
                                Log.d(">>>>>>>>>>>>>>>>>>>>>>>", "TRIGGERED");
                                ESM.queueESM(context, trigger.esm);
                                triggered = true;
                                surveyTrigger = "App closed: "+currentApp;
                            }
                        }
                    }
                }

                //Share context to broadcast and send to database
                if ( triggered && Plugin.pluginContext != null) //add if survey_triggered
                    Plugin.pluginContext.onContext();
            }else if(intent.getAction().equals(ESM.ACTION_AWARE_ESM_ANSWERED)){
                String answer = intent.getStringExtra(ESM.EXTRA_ANSWER);
                Log.d("SURVEY",answer);
                //add string to answers
                surveyAnswers += answer + ", ";
            }else if(intent.getAction().equals(ESM.ACTION_AWARE_ESM_DISMISSED)){
                //add empty string to answers
                Log.d("SURVEY","dismissed");
                surveyAnswers += " dismissed.";
                //store trigger & answers in database
            }else if(intent.getAction().equals(ESM.ACTION_AWARE_ESM_EXPIRED)){
                //add empty string to answers
                Log.d("SURVEY","expiered");
                surveyAnswers += " expiered.";
                //only 1 question expieres at a time.
            }

            }

    }

    /**
     * Detect application closing.
     * @param  queue Previous application queue.
     * @param  app   Application name.
     * @return has application closed
     */
    private static boolean hasAppClosed(Queue<String> queue, String app) {
        boolean appBefore = false;
        for (String s : queue) {
            if (s.equals(app)) {
                appBefore = true;
            }
            if (appBefore && s.equals("Pixel Launcher")) {
                return true;
            }
        }
        return false;
    }

}
