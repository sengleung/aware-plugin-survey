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
    // Attributes to accommodate application-triggered ESMs.
    private static final int PREVIOUS_APP_SIZE = 6;
    private static final String NEW_DAY = "NEW_DAY" ;

    private boolean appTriggered;
    private static List<Trigger> triggerList;
    private static Queue<String> prevApps;
    private static List<TimerInfo> timerInfos;

    private static ContextProducer pluginContext;
    private static String triggerApp;
    private static String currentESM;
    private static String surveyTrigger;
    private static String answer = "";
    private static String question;
    private static int questionIndex;
    private static int numOfSurveys = 0;

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
                ContentValues rowData = new ContentValues();
                rowData.put(Provider.Plugin_Survey_Data.TIMESTAMP, System.currentTimeMillis());
                rowData.put(Provider.Plugin_Survey_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                rowData.put(Provider.Plugin_Survey_Data.SURVEY_ID, numOfSurveys);
                rowData.put(Provider.Plugin_Survey_Data.QUESTION, question);
                rowData.put(Provider.Plugin_Survey_Data.ANSWER, answer);
                rowData.put(Provider.Plugin_Survey_Data.TRIGGER, surveyTrigger);
                rowData.put(Provider.Plugin_Survey_Data.APPLICATION, triggerApp);

//                Log.d("SERVER", Arrays.toString(DATABASE_TABLES));
//                Log.d("SERVER", Arrays.toString(TABLES_FIELDS));
//                Log.d("SERVER", Arrays.toString(CONTEXT_URIS));
                Log.d(TAG, "Sending data " + rowData.toString());
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
        CONTEXT_URIS = new Uri[]{Provider.Plugin_Survey_Data.CONTENT_URI}; //this syncs dummy Plugin_Survey_Data to server

        // Clear previous Scheduler.
//        System.out.println("Clearing previous Scheduler.");
//        for (int i = 0; i < 24; i++) {
//            for (int j = 0; j < 60; j++) {
//                Scheduler.removeSchedule(getApplicationContext(),
//                        "ESM_TIME_TRIGGER_" + String.format("%02d", i) +
//                                ":" + String.format("%02d", j),
//                        getPackageName()
//                );
//            }
//        }

        // Parse configuration file and ESM JSONs.
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, false);
        ConfigFile cf = new ConfigFile(this);
        triggerList = cf.getTriggers();

        // Initialise triggers.
        System.out.println("Initialising triggers.");
        timerInfos = new ArrayList<>();
        boolean appTriggered = false;
        boolean appDelayTrigger = false;
        for (Trigger trigger : triggerList) {
            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);
            if (trigger instanceof TriggerTime) {
                //Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);
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
            //TO get esm answers
            contextFilter.addAction(ESM.ACTION_AWARE_ESM_EXPIRED);
            contextFilter.addAction(ESM.ACTION_AWARE_ESM_DISMISSED);
            contextFilter.addAction(ESM.ACTION_AWARE_ESM_ANSWERED);
            contextFilter.addAction(ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE);
            contextFilter.addAction(NEW_DAY);
            registerReceiver(contextReceiver, contextFilter);
        }

        //
        if (appDelayTrigger) {
            DelayTimer tx = new DelayTimer();
            tx.start();
        }
        //[14:00,21:10,13:12,20:54,20:55]
        //Set the newday broacast to send at 0:00 p.m.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
       // calendar.set(Calendar.MINUTE, 25);
        Scheduler.Schedule newDay = new Scheduler.Schedule("day");
        try {
            newDay.setTimer(calendar)
                    .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                    .setActionIntentAction(NEW_DAY);
            Scheduler.saveSchedule(getApplicationContext(), newDay);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        //join study !REQUIERED to sync data to server
        Aware.joinStudy(this,
                "https://api.awareframework.com/index.php/webservice/index/1118/s7VgPquEj8aM");
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

        Aware.setSetting(this, Settings.STATUS_SURVEY_PLUGIN, false);

        //Stop AWARE's instance running inside the plugin package
        //Aware.stopAWARE();
        Aware.stopAWARE(this);
    }

    private static ContextReceiver contextReceiver = new ContextReceiver();

    /**
     * Class for application opening/closing detection.
     */
    public static class ContextReceiver extends BroadcastReceiver {

        /**
         * Interrupt handling for application opening/closing.
         *
         * @param context Context
         * @param intent  Intent
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND)) {
                // Detect application name.
                boolean triggered = false;

                String currentApp = getCurrentAppName(intent);

                Log.d("NUM", String.valueOf(numOfSurveys));
                for (Trigger trigger : triggerList) {
                    if (!surveyJustCompleted && numOfSurveys<MAX_NUM_OF_SURVEYS &&
                            (trigger instanceof TriggerAppOpenClose ||
                            trigger instanceof TriggerAppDelay)) {
                        appTrigger(context, currentApp);
                    }
                }
            }else if(intent.getAction().equals(ESM.ACTION_AWARE_ESM_ANSWERED)){
                //set question and anser to save
                question = setQusetion();
                answer = intent.getStringExtra(ESM.EXTRA_ANSWER);
                questionIndex++;
                Log.d("ESM ", "Answer: "+intent.getStringExtra(ESM.EXTRA_ANSWER));
                //Share context to broadcast and send to database
                if (Plugin.pluginContext != null)
                    Plugin.pluginContext.onContext();

            }else if(intent.getAction().equals(ESM.ACTION_AWARE_ESM_DISMISSED)){
                Log.d("ESM ", "Dismissed");
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
                Log.d("ESM ", "queue complete");
                //Disable open/close triggered surveys for oneMinute after one has completed
                surveyJustCompleted = true;
                int oneMinute = 60000;
                new CountDownTimer(oneMinute/60, oneMinute/60) {

                    public void onTick(long millisUntilFinished) {
                    }

                    public void onFinish() {
                        surveyJustCompleted = false;
                        Log.d("PAUSE", "Surveys can be triggered");
                    }
                }.start();
            }else if(intent.getAction().equals(NEW_DAY)){
                Log.d("NEW DAY", String.valueOf(numOfSurveys));
                numOfSurveys = 0;
                Log.d("NEW DAY", String.valueOf(numOfSurveys));

            }else{
                Log.d("BROADCAST", String.valueOf(intent));
            }

        }

        private static String getCurrentAppName(Intent intent) {
            // Specify application name.
            String currentApp1 = "";
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                ContentValues appForground = (ContentValues) bundle.get(Applications.EXTRA_DATA);
                currentApp1 = (String) appForground.get("application_name");

                Log.d(">>>>>>>>>>>>>>>>>>>>>>>", currentApp1);

                Log.d("APP_OPEN_CLOSE", currentApp1);

//                //Share context to broadcast and send to database
//                if (Plugin.pluginContext != null)
//                    Plugin.pluginContext.onContext();
            }
            return currentApp1;
        }

        private static boolean hasAppOpened(String currentApp, String app) {
            return (app.equals(currentApp) && !prevApps.contains(currentApp));
        }

        /**
         * Detect application closing.
         *
         * @param queue Previous application queue.
         * @param app   Application name.
         * @return has application closed
         */
        private static boolean hasAppClosed(Queue<String> queue, String app) {
            boolean appBefore = false;
            boolean onePixelLauncher = false;
            for (String s : queue) {
                if (s.equals(app)) {
                    appBefore = true;
                    Log.d("CLOSE", "appBefore");
                }
                if(appBefore && onePixelLauncher && s.equals("Pixel Launcher")){
                    //pixel launcher detected as muliple new opens (for count)
                    Log.d("CLOSE", "multipl Pix");
                    return false;
                }else if (appBefore && s.equals("Pixel Launcher")) {
                    onePixelLauncher = true;
                    Log.d("CLOSE", "one Pix");
                }
            }
            return appBefore && onePixelLauncher;
        }

        /**
         * Application opening/closing interrupt handler.
         *
         * @param context    context
         * @param currentApp current app name
         */
        private static void appTrigger(Context context, String currentApp) {
            // Detect application opening.
            for (Trigger trigger : triggerList) {
                // Application Open/Close Trigger.
                if (trigger instanceof TriggerAppOpenClose) {
                    for (String app : ((TriggerAppOpenClose) trigger).applications) {
                        if (hasAppOpened(currentApp, app) && ((TriggerAppOpenClose) trigger).open) {
                            Log.d("APP_OPEN_CLOSE", "Application opened. ESM delivered.");
                            //set esm trigger value
                            trigger.setTrigger("App opend: "+currentApp);
                            ESM.queueESM(context, trigger.esm);
                            setSurveyValues(trigger, currentApp, "Opened");
                            numOfSurveys++;
                            Log.d("NUM", "open increase: "+String.valueOf(numOfSurveys));
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
                            setSurveyValues(trigger, currentApp, "Delay of "+((TriggerAppDelay) trigger).delay+" s");
                        }
                    }
                    if (setTimer) {
                        for (TimerInfo timerInfo : timerInfos) {
                            if (timerInfo.triggerAppDelay == trigger) {
                                Log.d("APP_DELAY", "Timer Set.");
                                timerInfo.setTimer();
                            }
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
            Log.d("APP_OPEN_CLOSE", prevApps.toString());

            // Detect application closing.
            for (Trigger trigger : triggerList) {
                // Application Open/Close Trigger.
                if (trigger instanceof TriggerAppOpenClose) {
                    for (String app : ((TriggerAppOpenClose) trigger).applications) {
                        if (hasAppClosed(prevApps, app) && ((TriggerAppOpenClose) trigger).close) {
                            Log.d("APP_OPEN_CLOSE", "Application closed. ESM delivered.");
                            //set esm trigger value
                            trigger.setTrigger("App closed: "+app);
                            ESM.queueESM(context, trigger.esm);
                            numOfSurveys++;
                            Log.d("Close NUM", "increase: "+String.valueOf(numOfSurveys));
                            setSurveyValues(trigger, currentApp, "Closed");
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

        private static void setSurveyValues(Trigger trigger, String currentApp, String triggerType) {
            currentESM = trigger.esm;
            triggerApp = currentApp;
            surveyTrigger = triggerType;
            questionIndex = 0;
        }

        private static String setQusetion() {
            try {
                JSONArray esm = new JSONArray(currentESM);
                String titel = esm.getJSONObject(questionIndex).getJSONObject("esm").getString("esm_title");
                String instruction = esm.getJSONObject(questionIndex).getJSONObject("esm").getString("esm_instructions");
                Log.d("QUESTION",titel + ", "+ instruction+" at "+questionIndex);
                return titel + ", "+ instruction;
            } catch (JSONException e) {
                //e.printStackTrace();
                Log.d("QUESTION","ERROR at "+questionIndex);
                return "";
            }
        }
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
                        if (timerInfo.set && System.currentTimeMillis() > timerInfo.activation) {
                            Log.d("APP_DELAY", "Application duration elapsed. ESM delivered.");
                            timerInfo.disableTimer();
                            //set esm trigger value
                            //!!!! TODO triggerApp might not work here
                            timerInfo.triggerAppDelay.setTrigger(triggerApp+ " open for " +
                                    timerInfo.triggerAppDelay.delay + " seconds");
                            ESM.queueESM(getApplicationContext(), timerInfo.triggerAppDelay.esm);
                            numOfSurveys++;
                            Log.d("NUM", "Delay increase: "+String.valueOf(numOfSurveys));
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
    }
}
