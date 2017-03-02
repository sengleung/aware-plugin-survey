package com.aware.plugin.survey;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.plugin.survey.survey.ConfigFile;
import com.aware.plugin.survey.survey.Trigger;
import com.aware.plugin.survey.survey.TriggerAppOpenClose;
import com.aware.plugin.survey.survey.TriggerTime;
import com.aware.ui.PermissionsHandler;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;

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
    // Attributes to accommodate application-triggered ESMs.
    private static final int PREVIOUS_APP_SIZE = 4;
    private static List<Trigger> triggerList;
    private static Queue<String> prevApps;

    /**
     * Initialise survey plugin.
     */
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

        // Clear previous Scheduler.
        System.out.println("Clearing previous Scheduler.");
        for (int i = 0; i < 24; i++) {
            for (int j = 0; j < 60; j++) {
                Scheduler.removeSchedule(getApplicationContext(),
                                            "ESM_TIME_TRIGGER_" + String.format("%02d", i) +
                                                            ":" + String.format("%02d", j),
                                            getPackageName()
                );
            }
        }

        // Parse configuration file and ESM JSONs.
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, false);
        ConfigFile cf = new ConfigFile(this);
        triggerList = cf.getTriggers();

        // Initialise triggers.
        System.out.println("Initialising triggers.");
        boolean appTriggered = false;
        for (Trigger trigger : triggerList) {
            if (trigger instanceof TriggerAppOpenClose) {
                appTriggered = true;
            }
            if (trigger instanceof  TriggerTime) {
                Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);
                ((TriggerTime) trigger).setESM();
            }
        }
        // If trigger is application triggered, initiate interrupt.
        if (appTriggered) {
            IntentFilter contextFilter = new IntentFilter();
            contextFilter.addAction(Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND);
            registerReceiver(contextReceiver, contextFilter);
        }

        // Activate plugin -- do this ALWAYS as the last thing
        // (this will restart your own plugin and apply the settings)
        Aware.startPlugin(this, "com.aware.plugin.survey");
    }

    /**
     * On plugin cessation.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        Aware.setSetting(this, Settings.STATUS_SURVEY_PLUGIN, false);

        //Stop AWARE's instance running inside the plugin package
        Aware.stopAWARE();
    }

    /**
     * On plugin commencement.
     */
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
        } else {
            Intent permissions = new Intent(this, PermissionsHandler.class);
            permissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
            permissions.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(permissions);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private static ContextReceiver contextReceiver = new ContextReceiver();

    /**
     * Class for application opening/closing detection.
     */
    public static class ContextReceiver extends BroadcastReceiver {

        /**
         * Interrupt handling for application opening/closing.
         * @param context
         * @param intent
         */
        @Override
        public void onReceive(Context context, Intent intent) {

            // Specify application name.
            String currentApp = "";
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                for (String key : bundle.keySet()) {
                    Object value = bundle.get(key);
//                    Log.d(TAG, String.format("K: %s V: %s c: (%s)", key,
//                            value.toString(), value.getClass().getName()));
//                    System.out.println("Key: "+key+" VAL: "+value.toString()+" "+value.getClass().getName());
                    if (value instanceof ContentValues) {
                        ContentValues val = (ContentValues) value;
//                            System.out.println("VALS: " + val.toString());
//                            System.out.println("APP: " + val.get("application_name"));
                        currentApp = (String) val.get("application_name");
                        long timestamp = System.currentTimeMillis();

                        Log.d("APP_OPEN_CLOSE", currentApp);

//                        //Share context to broadcast and send to database
//                        if (Plugin.pluginContext != null)
//                            Plugin.pluginContext.onContext();

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
                            Log.d("APP_OPEN_CLOSE", "Application opened. ESM delivered.");
                            ESM.queueESM(context, trigger.esm);

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
                if (trigger instanceof TriggerAppOpenClose) {
                    for (String app : ((TriggerAppOpenClose) trigger).applications) {
                        if (hasAppClosed(prevApps, app) && ((TriggerAppOpenClose) trigger).close) {
                            Aware.setSetting(context, Aware_Preferences.STATUS_ESM, true);
                            Log.d("APP_OPEN_CLOSE", "Application closed. ESM delivered.");
                            ESM.queueESM(context, trigger.esm);

                        }
                    }
                }
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
