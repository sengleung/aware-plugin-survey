package com.aware.plugin.survey.survey;

import android.os.CountDownTimer;
import android.util.Log;

import com.aware.plugin.survey.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Time-specific ESM trigger.
 *
 * @author  Seng Leung
 * @version 1.0
 */
public class TriggerAppOpenClose extends Trigger {
    public List<String> applications;
    public List<String> surveyTriggered;
    public final boolean open;
    public final boolean close;

    /**
     * Trigger Application Open/Close constructor.
     * @param plugin        survey plugin
     * @param trigger       trigger name
     * @param esmFile       ESM file
     * @param applications  list of applications
     * @param open          trigger when app open
     * @param close         trigger when app close
     */
    public TriggerAppOpenClose(Plugin plugin,
                               String trigger,
                               String esmFile,
                               List<String> applications,
                               boolean open,
                               boolean close) {
        super(plugin, trigger, esmFile);
        this.applications = applications;
        this.surveyTriggered = new ArrayList<String>();
        this.open = open;
        this.close = close;
    }

    public void setPause(final String application){
        this.surveyTriggered.add(application);
        int oneMinute = 60000;
        Log.d("PAUSE", application+" added");
        new CountDownTimer(oneMinute, oneMinute) {

            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                surveyTriggered.remove(application);
                Log.d("PAUSE", application+" removed");
            }
        }.start();
    }
}
