package com.aware.plugin.survey.survey;

import com.aware.plugin.survey.Plugin;
import com.aware.utils.Scheduler;
import com.aware.ESM;

import java.util.List;

/**
 * Time-specific ESM trigger.
 *
 * @author  Seng Leung
 * @version 1.0
 */
public class TriggerTime extends Trigger {
    private List<String> times;

    /**
     * Time Trigger constructor.
     * @param plugin    survey plugin
     * @param trigger   trigger name
     * @param esmFile   ESM file name
     * @param times     trigger times
     */
    public TriggerTime(Plugin plugin,
                       String trigger,
                       String esmFile,
                       List<String> times) {
        super(plugin, trigger, esmFile);
        this.times = times;
    }

    /**
     * Set time-triggered ESMs.
     */
    public void setESM() {
        try {
            for (String t : times) {
                String[] time = t.split(":");
                // Formulate unique ID as to not overwrite previous Scheduler.
                Scheduler.Schedule scheduler = new Scheduler.Schedule("ESM_TIME_TRIGGER_" +
                                                                      time[0] + ":" + time[1]);
                scheduler.addHour(Integer.parseInt(time[0]));
                scheduler.addMinute(Integer.parseInt(time[1]));

                //scheduler.setInterval(1);
                scheduler.setActionType(Scheduler.ACTION_TYPE_BROADCAST); //sending a request to the client via broadcast
                scheduler.setActionClass(ESM.ACTION_AWARE_QUEUE_ESM); //with the action of ACTION_AWARE_QUEUE_ESM, i.e., queueing a new ESM
                scheduler.addActionExtra(ESM.EXTRA_ESM, esm); //add the questions from the factory
                Scheduler.saveSchedule(plugin, scheduler); //save the questionnaire and schedule it
            }
        } catch (Exception e) {
        }
    }

}
