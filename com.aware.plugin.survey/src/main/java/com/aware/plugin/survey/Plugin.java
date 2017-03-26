package com.aware.plugin.survey;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.plugin.survey.survey.ConfigFile;
import com.aware.plugin.survey.survey.TimerInfo;
import com.aware.plugin.survey.survey.Trigger;
import com.aware.plugin.survey.survey.TriggerAppDelay;
import com.aware.plugin.survey.survey.TriggerAppOpenClose;
import com.aware.plugin.survey.survey.TriggerTime;
import com.aware.providers.Applications_Provider;
import com.aware.ui.PermissionsHandler;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;
import com.aware.utils.PluginsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Main survey plugin functionalities.
 *
 * @author  Seng Leung
 * @version 1.0
 */
public class Plugin extends Aware_Plugin {

    //MAXIMUM NUMBER OF SURVEYS PER DAY
    private static final int MAX_NUM_OF_SURVEYS = 4;
    private static final int TIME_BETWEEN_SURVEYS = 30; //in seconds
    private static final String PREV_NUM_OF_SURVEYS = "Pref";
    private static final String PREV_KEY = "num_of_surveys";

    // Attributes to accommodate application-triggered ESMs.
    private static final int PREVIOUS_APP_SIZE = 6;
    private static final String NEW_DAY = "NEW_DAY" ;

    private boolean appTriggered;
    private static List<Trigger> triggerList;
    private static List<String> prevApps;
    private static List<TimerInfo> timerInfos;
    private static List<ConfigFile.Tuple> timeBetweenApps;
    private static boolean[] appDelays;

    private static ContextProducer pluginContext;
    private static String triggerApp = "";
    private static String currentApp = "";
    private static String prevApp = "";
    private static String currentESM;
    private static String surveyTrigger;
    private static String answer = "";
    private static String question = "";
    private static long timestamp;
    private static long surveyTimestamp;
    private static long duration;
    private static long currentDuration;
    private static int questionIndex;
    private static int numOfSurveys;
    private static int currentAppID;
    private static int triggerAppID;

    SharedPreferences pref;

    private static boolean surveyJustCompleted = false;
    /**
     * Initialise survey plugin.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        prevApps = new LinkedList<>();

        TAG = "AWARE::" + getResources().getString(R.string.app_name);

        /**
         * Plugins share their current status, i.e., context using this method.
         * This method is called automatically when triggering
         * {@link Aware#ACTION_AWARE_CURRENT_CONTEXT}
         **/
        pluginContext = new ContextProducer() {
            @Override
            public void onContext() {
                //Save rowData in database
                if(!(question.equals("") && answer.equals(""))) {
                    ContentValues rowData = new ContentValues();
                    rowData.put(Provider.Plugin_Survey_Data.TIMESTAMP, System.currentTimeMillis());
                    rowData.put(Provider.Plugin_Survey_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                    rowData.put(Provider.Plugin_Survey_Data.SURVEY_ID, numOfSurveys);
                    rowData.put(Provider.Plugin_Survey_Data.QUESTION_ID, questionIndex);
                    rowData.put(Provider.Plugin_Survey_Data.QUESTION, question);
                    rowData.put(Provider.Plugin_Survey_Data.ANSWER, answer);
                    rowData.put(Provider.Plugin_Survey_Data.TRIGGER, surveyTrigger);
                    rowData.put(Provider.Plugin_Survey_Data.APPLICATION, triggerApp);
                    rowData.put(Provider.Plugin_Survey_Data.DURATION, duration);
                    int index = prevApps.indexOf(triggerApp);
                    String previousApp = (index > 0) ? prevApps.get(index - 1) : "";
                    rowData.put(Provider.Plugin_Survey_Data.PREV_APPLICATION, previousApp);
                    rowData.put(Provider.Plugin_Survey_Data.APP_TABLE_ID, surveyTimestamp);

                    Log.d(TAG, "Sending data " + rowData.toString());
                    getContentResolver().insert(Provider.Plugin_Survey_Data.CONTENT_URI, rowData);
                    //broadcast?

                }
            }
        };
        CONTEXT_PRODUCER = pluginContext;

        //Add permissions you need (Android M+).
        //By default, AWARE asks access to the #Manifest.permission.WRITE_EXTERNAL_STORAGE

        //REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        //To sync data to the server, you'll need to set this variables from your ContentProvider
        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{Provider.Plugin_Survey_Data.CONTENT_URI}; //this syncs dummy Plugin_Survey_Data to server

        //RESTORE Count of max num (0 for mode private)
        pref = getSharedPreferences(PREV_NUM_OF_SURVEYS, 0);
        numOfSurveys = pref.getInt(PREV_KEY, 0);

        // Clear previous Scheduler.
//        System.out.println("Clearing previous Scheduler.");
//        for (int i = 0; i < 24; i++) {
//            for (int j = 0; j < 60; j++) {
//                Scheduler.removeSchedule(getApplicationContext(),
//                        "ESM_TIME_TRIGGER_" + String.format("%02d", i) +
//                                ":" + String.format("%02d", j)
//                );
//            }
//        }
//        Scheduler.removeSchedule(getApplicationContext(), "ESM_TIME_TRIGGER_12:00");

        // Parse configuration file and ESM JSONs.
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, false);
        ConfigFile cf = new ConfigFile(this);
        triggerList = cf.getTriggers();
        timeBetweenApps = cf.getDurations();

        // Initialise triggers.
        timerInfos = new ArrayList<>();
        boolean appTriggered = false;
        boolean appDelayTrigger = false;
        for (Trigger trigger : triggerList) {
           // Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);
            if (trigger instanceof TriggerTime) {
                ((TriggerTime) trigger).setESM();
            }
            if (trigger instanceof TriggerAppOpenClose) {
                appTriggered = true;
            }
            if (trigger instanceof TriggerAppDelay) {
                appTriggered = true;
                appDelayTrigger = true;
                timerInfos.add(new TimerInfo((TriggerAppDelay) trigger));
            }
        }
        // If trigger is application triggered, initiate interrupt.
        if (appTriggered) {
            IntentFilter contextFilter = new IntentFilter();
            //Check the sensor/plugin documentation for specific context broadcasts.
            contextFilter.addAction(Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND);
            contextFilter.addAction(Applications.ACTION_AWARE_APPLICATIONS_HISTORY);
            //TO get esm answers
            contextFilter.addAction(ESM.ACTION_AWARE_ESM_EXPIRED);
            contextFilter.addAction(ESM.ACTION_AWARE_ESM_DISMISSED);
            contextFilter.addAction(ESM.ACTION_AWARE_ESM_ANSWERED);
            contextFilter.addAction(ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE);
            contextFilter.addAction(NEW_DAY);
            registerReceiver(contextReceiver, contextFilter);
        }

        if (appDelayTrigger) {
            DelayTimer tx = new DelayTimer();
            tx.start();
        }
        appDelays = new boolean[timeBetweenApps.size()];
        for(int i=0; i<appDelays.length; i++){
            appDelays[i]=false;
        }

        //Set the newday broacast to send at 0:00 p.m., to reset numOfSurveys
        setNewDayTimer();

        //join study !REQUIERED to sync data to server
 //       https://api.awareframework.com/index.php/webservice/index/1058/kGP61oC2ukdh
 //       Aware.joinStudy(this,
 //               "https://api.awareframework.com/index.php/webservice/index/1118/s7VgPquEj8aM");
//        Aware.joinStudy(this,
//                "https://api.awareframework.com/index.php/webservice/index/1058/kGP61oC2ukdh");
    }

    private void setNewDayTimer() {
        Context context = getApplicationContext();
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent(NEW_DAY), PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.AM_PM, Calendar.AM);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), 1000 * 60 *60 *24, pi);
    }

    /**
     * On plugin commencement.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {
            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");
            //Initialize our plugin's settings
            Aware.setSetting(this, Settings.STATUS_SURVEY_PLUGIN, true);
            //Initialise AWARE instance in plugin
            Aware.startPlugin(this, "com.aware.plugin.survey");
            Aware.startAWARE(this);
        }
        return START_STICKY;
    }

    /**
     * On plugin cessation.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        //Save num of surveys
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(PREV_KEY, numOfSurveys);
        editor.commit();

        Aware.setSetting(this, Settings.STATUS_SURVEY_PLUGIN, false);

        //Stop AWARE's instance running inside the plugin package
        Aware.stopAWARE(this);
    }

    private  ContextReceiver contextReceiver = new ContextReceiver();

    /**
     * Class for application opening/closing detection.
     */
    public  class ContextReceiver extends BroadcastReceiver {

        /**
         * Interrupt handling for application opening/closing.
         *
         * @param context Context
         * @param intent  Intent
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND)) {
                Log.d("SURVEY>>>>>>>>>", "NEW APP");
                newApp(context, intent);
            }else if(intent.getAction().equals(Applications.ACTION_AWARE_APPLICATIONS_HISTORY)){

            }else if(intent.getAction().equals(NEW_DAY)){
                Log.d(TAG+"NEW DAY", String.valueOf(numOfSurveys));
                Toast.makeText(getApplicationContext(), "NEW DAY",
                        Toast.LENGTH_SHORT).show();
                numOfSurveys = 0;
                Log.d(TAG+"NEW DAY", String.valueOf(numOfSurveys));
            }else if(intent.getAction().equals(ESM.ACTION_AWARE_ESM_ANSWERED)){
                //set question and anser to save
                question = setQusetion();
                answer = intent.getStringExtra(ESM.EXTRA_ANSWER);
                questionIndex++;
                Log.d(TAG+"ESM ", "Answer: "+intent.getStringExtra(ESM.EXTRA_ANSWER));
                //Share context to broadcast and send to database
                if (Plugin.pluginContext != null)
                    Plugin.pluginContext.onContext();

            }else if(intent.getAction().equals(ESM.ACTION_AWARE_ESM_DISMISSED)){
                Log.d(TAG+"ESM ", "Dismissed");
                question = setQusetion();
                answer = "dismissed";
                while(!question.equals("")){
                    //answer = "dismissed";
                    questionIndex++;
                    Log.d("ESM ", "Answer: "+intent.getStringExtra(ESM.EXTRA_ANSWER));
                    //Share context to broadcast and send to database
                    if (Plugin.pluginContext != null)
                        Plugin.pluginContext.onContext();
                    question = setQusetion();
                }
            }else if(intent.getAction().equals(ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE)){
                Log.d(TAG+"ESM ", "queue complete");
                //Disable  surveys for oneMinute after one has completed
                surveyJustCompleted = true;
                int oneMinute = 60000;
                int oneSecond = 1000;
                new CountDownTimer(oneSecond*TIME_BETWEEN_SURVEYS, oneSecond*TIME_BETWEEN_SURVEYS) {
                    public void onTick(long millisUntilFinished) {
                    }
                    public void onFinish() {
                        surveyJustCompleted = false;
                        Log.d(TAG+"PAUSE", "Surveys can be triggered");
                    }
                }.start();
                final int index = getIndex(triggerApp, timeBetweenApps);
                if(index>=0){
                    appDelays[index] = true;
                    Log.d(TAG+"PAUSE", "Surveys paused: "+index+" "+triggerApp);
                    int time = timeBetweenApps.get(index).getInt();
                    new CountDownTimer(oneSecond*time, oneSecond*time) {
                        public void onTick(long millisUntilFinished) {
                        }
                        public void onFinish() {
                            appDelays[index] = false;
                            Log.d(TAG+"PAUSE", "Surveys can be triggered for "+index);
                        }
                    }.start();
                }
            }else{
                Log.d(TAG+"BROADCAST", String.valueOf(intent));
            }

        }

        private void newApp(Context context, Intent intent) {
            String newApp = "";
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                ContentValues appForground = (ContentValues) bundle.get(Applications.EXTRA_DATA);
                newApp = (String) appForground.get("application_name");
                //if still the same ap do nothing
                if(newApp.equals(currentApp) || newApp.equals("Android Keyboard (AOSP)")) return;
                //TODO remove keyboard when done
                long startTime = timestamp; //start time of prev app
                timestamp = (long) appForground.get("timestamp");
                currentDuration = timestamp-startTime;
                Log.d("SURVEY>>>>>>>>>>>>>", String.valueOf(timestamp));
                prevApp = currentApp;
                currentApp = newApp;
                Log.d("SURVEY>>>>>>>>>>>>>", currentApp+" | "+prevApp+" "+timestamp+" | "+triggerApp);
            }

            if(hasAppClosed(prevApps, triggerApp)){
                //update duration
                Cursor appUnclosed = getContentResolver().query(Provider.Plugin_Survey_Data.CONTENT_URI, null, Provider.Plugin_Survey_Data.APPLICATION + " LIKE '%" + triggerApp + "%' AND "  + Provider.Plugin_Survey_Data.DURATION + "=0", null, null);
                if (appUnclosed.moveToFirst()) {
                    ContentValues rowData = new ContentValues();
                    rowData.put(Provider.Plugin_Survey_Data.DURATION, currentDuration);
                    do {
                        getContentResolver().update(Provider.Plugin_Survey_Data.CONTENT_URI, rowData, Provider.Plugin_Survey_Data._ID + "=" + appUnclosed.getInt(appUnclosed.getColumnIndex(Provider.Plugin_Survey_Data._ID)), null);
                        Log.d("SURVEY>>>>", "updated: "+appUnclosed.getInt(appUnclosed.getColumnIndex(Provider.Plugin_Survey_Data._ID)));
                    }while(appUnclosed.moveToNext());
                }
            }
            Log.d(TAG+"NUM", String.valueOf(numOfSurveys));
            if (!surveyJustCompleted && numOfSurveys<MAX_NUM_OF_SURVEYS){
                appTrigger(context, currentApp);
            }
        }

        private boolean hasAppOpened(String currentApp, String app) {
            return (app.equals(currentApp));// && !prevApps.contains(currentApp));
        }

        /**
         * Detect application closing.
         *
         * @param queue Previous application queue.
         * @param app   Application name.
         * @return has application closed
         */
        private boolean hasAppClosed(List<String> queue, String app) {
//            boolean appBefore = false;
//            boolean onePixelLauncher = false;
//            for (String s : queue) {
//                if (s.equals(app)) {
//                    appBefore = true;
//                    Log.d("CLOSE", "appBefore");
//                }
//                if(appBefore && !s.equals(app));
//                if(appBefore && onePixelLauncher && s.equals("Pixel Launcher")){
//                    //pixel launcher detected as muliple new opens (for count)
//                    Log.d("CLOSE", "multipl Pix");
//                    return false;
//                }else if (appBefore && s.equals("Pixel Launcher")) {
//                    onePixelLauncher = true;
//                    Log.d("CLOSE", "one Pix");
//                }
//            }
//            return appBefore && onePixelLauncher;
           return (prevApp.equals(app));
        }

        /**
         * Application opening/closing interrupt handler.
         *
         * @param context    context
         * @param currentApp current app name
         */
        private void appTrigger(Context context, String currentApp) {
            // Detect application opening.
            for (Trigger trigger : triggerList) {
                // Application Open/Close Trigger.
                if (trigger instanceof TriggerAppOpenClose) {
                    for (String app : ((TriggerAppOpenClose) trigger).applications) {
                        if (hasAppOpened(currentApp, app) && ((TriggerAppOpenClose) trigger).open && timeBetweenSurveysElapsed(app)) {
                            Log.d(TAG + "APP_OPEN_CLOSE", "Application opened. ESM delivered.");
                            //set esm trigger value
                            trigger.setTrigger("App opend: " + currentApp);
                            setSurveyValues(trigger, currentApp, "Opened");
                            numOfSurveys++;
                            ESM.queueESM(context, trigger.esm);
                            Log.d(TAG + "NUM", "open increase: " + String.valueOf(numOfSurveys));
                            //((TriggerAppOpenClose) trigger).setPause(currentApp);
                        }
                    }
                }
                // Application Delay Trigger.
                if (trigger instanceof TriggerAppDelay) {
                    boolean setTimer = false;
                    for (String app : ((TriggerAppDelay) trigger).applications) {
                        if (hasAppOpened(currentApp, app)) {
                            setTimer = true;
                            setSurveyValues(trigger, currentApp, "Delay of " + ((TriggerAppDelay) trigger).delay + " s");
                        }
                    }
                    if (setTimer) {
                        for (TimerInfo timerInfo : timerInfos) {
                            if (timerInfo.triggerAppDelay == trigger) {
                                Log.d(TAG + "APP_DELAY", "Timer Set.");
                                timerInfo.setTimer();
                            }
                        }
                    }
                }
            }
            // Update previous application queue.
            if (prevApps.size() >= PREVIOUS_APP_SIZE) {
                prevApps.remove(0);
                prevApps.add(currentApp);
            } else {
                prevApps.add(currentApp);
            }
            Log.d(TAG+"APP_OPEN_CLOSE", prevApps.toString());

            // Detect application closing.
            for (Trigger trigger : triggerList) {
                // Application Open/Close Trigger.
                if (trigger instanceof TriggerAppOpenClose) {
                    for (String app : ((TriggerAppOpenClose) trigger).applications) {
                        if (hasAppClosed(prevApps, app) && ((TriggerAppOpenClose) trigger).close && timeBetweenSurveysElapsed(app)) {
                            Log.d(TAG+"APP_OPEN_CLOSE", "Application closed. ESM delivered.");
                            //set esm trigger value
                            trigger.setTrigger("App closed: "+app);
                            ESM.queueESM(context, trigger.esm);
                            numOfSurveys++;
                            Log.d(TAG+"Close NUM", "increase: "+String.valueOf(numOfSurveys));
                            setSurveyValues(trigger, app, "Closed");
                        }
                    }
                }
                // Application Delay Trigger.
                if (trigger instanceof TriggerAppDelay) {
                    boolean disableTimer = false;
                    for (String app : ((TriggerAppDelay) trigger).applications) {
                        if (hasAppClosed(prevApps, app)) {
                            disableTimer = true;
                        }
                    }
                    if (disableTimer) {
                        for (TimerInfo timerInfo : timerInfos) {
                            if (timerInfo.triggerAppDelay == trigger) {
                                timerInfo.disableTimer();
                            }
                        }
                    }
                }
            }
        }

        private void setSurveyValues(Trigger trigger, String currentApp, String triggerType) {
            currentESM = trigger.esm;
            triggerApp = currentApp;
            surveyTrigger = triggerType;
            questionIndex = 0;
            triggerAppID = currentAppID;
            if(triggerType=="Closed") {
                duration = currentDuration;
                surveyTimestamp = timestamp-duration;
            }else{
                duration = 0;
                surveyTimestamp = timestamp;
            }
                //TODO make constant
        }

        private String setQusetion() {
            try {
                JSONArray esm = new JSONArray(currentESM);
                String titel = esm.getJSONObject(questionIndex).getJSONObject("esm").getString("esm_title");
                String instruction = esm.getJSONObject(questionIndex).getJSONObject("esm").getString("esm_instructions");
                Log.d(TAG+"QUESTION",titel + ", "+ instruction+" at "+questionIndex);
                return titel + ", "+ instruction;
            } catch (JSONException e) {
                //e.printStackTrace();
                Log.d(TAG+"QUESTION","ERROR at "+questionIndex);
                return "";
            }
        }
    }

    private boolean timeBetweenSurveysElapsed(String app) {
        int index = getIndex(app, timeBetweenApps);
        return ((index>=0)? !appDelays[index]:true);
    }

    private int getIndex(String app, List<ConfigFile.Tuple> timeBetweenApps) {
        int i = 0;
        for(ConfigFile.Tuple tuple : timeBetweenApps){
            if(tuple.getString().equals(app))return i;
            i++;
        }
        return -1;
    }
    /**
     * Delay timer for application delay trigger.
     */
    class DelayTimer extends Thread {
        /**
         * Initiate timer process.
         */
        public void run() {
            while (true) {
                try {
                    Thread.sleep(2000);
                    for (TimerInfo timerInfo : timerInfos) {
                        // Log.d("APP_OPEN_CLOSE", "activation status:" + Boolean.toString(timerInfo.set));
                        if (timerInfo.set && System.currentTimeMillis() > timerInfo.activation && timeBetweenSurveysElapsed(triggerApp)) {
                            Log.d(TAG+"APP_DELAY", "Application duration elapsed. ESM delivered.");
                            timerInfo.disableTimer();
                            //set esm trigger value
                            timerInfo.triggerAppDelay.setTrigger(triggerApp+ " open for " +
                                    timerInfo.triggerAppDelay.delay + " seconds");
                            ESM.queueESM(getApplicationContext(), timerInfo.triggerAppDelay.esm);
                            numOfSurveys++;
                            Log.d(TAG+"NUM", "Delay increase: "+String.valueOf(numOfSurveys));
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
    }
}
