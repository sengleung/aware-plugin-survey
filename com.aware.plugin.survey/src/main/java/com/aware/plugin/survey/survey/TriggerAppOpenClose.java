package com.aware.plugin.survey.survey;

import com.aware.plugin.survey.Plugin;

import java.util.List;

/**
 * Time-specific ESM trigger.
 *
 * @author  Seng Leung
 * @version 1.0
 */
public class TriggerAppOpenClose extends Trigger {
    public List<String> applications;
    public final boolean open;
    public final boolean close;

    public TriggerAppOpenClose(Plugin plugin,
                       String trigger,
                       String esmFile,
                       List<String> applications,
                               boolean open,
                               boolean close) {
        super(plugin, trigger, esmFile);
        this.applications = applications;
        this.open = open;
        this.close = close;
    }

    public void setESM() {
//        try {
//            for (String t : times) {
//                String[] time = t.split(":");
//                Scheduler.Schedule scheduler = new Scheduler.Schedule("ESM_TIME_TRIGGER"); //schedule with morning_question as ID
//                scheduler.addHour(Integer.parseInt(time[0])); //8 PM (24h format), every day
//                scheduler.addMinute(Integer.parseInt(time[1]));
//                scheduler.setInterval(1);
//                scheduler.setActionType(Scheduler.ACTION_TYPE_BROADCAST); //sending a request to the client via broadcast
//                scheduler.setActionClass(ESM.ACTION_AWARE_QUEUE_ESM); //with the action of ACTION_AWARE_QUEUE_ESM, i.e., queueing a new ESM
//                scheduler.addActionExtra(ESM.EXTRA_ESM, esm); //add the questions from the factory
//                Scheduler.saveSchedule(plugin, scheduler); //save the questionnaire and schedule it
//                System.out.println("Setting time: " + time[0] + ":" + time[1]);
//            }
//        } catch (Exception e) {
//        }
    }

}
