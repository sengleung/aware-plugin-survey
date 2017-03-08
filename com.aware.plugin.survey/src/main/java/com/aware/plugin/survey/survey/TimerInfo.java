package com.aware.plugin.survey.survey;

/**
 * Timer information for the Application Delay Trigger.
 *
 * @author  Seng Leung
 * @version 1.0
 */
public class TimerInfo {
    public TriggerAppDelay triggerAppDelay;
    public boolean set;
    public long start;
    public long activation;

    /**
     * Timer Information constructor.
     * @param triggerAppDelay TriggerAppDelay class
     */
    public TimerInfo(TriggerAppDelay triggerAppDelay) {
        this.triggerAppDelay = triggerAppDelay;
        this.set = false;
        this.start = 0;
        this.activation = Long.MAX_VALUE;
    }

    /**
     * Initiate the timer for application delay trigger.
     */
    public void setTimer() {
        set = true;
        start = System.currentTimeMillis();
        activation = System.currentTimeMillis() + (triggerAppDelay.delay * 1000);
    }

    /**
     * Disable the timer for application delay trigger.
     */
    public void disableTimer() {
        set = false;
    }
}
