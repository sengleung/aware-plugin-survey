package com.aware.plugin.survey.survey;

import com.aware.plugin.survey.Plugin;

import java.util.List;

/**
 * Application time delay ESM trigger.
 *
 * @author  Seng Leung
 * @version 1.0
 */
public class TriggerAppDelay extends Trigger {
    public List<String> applications;
    public final int delay;

    /**
     * Trigger Application Delay constructor.
     * @param plugin        survey plugin
     * @param trigger       trigger name
     * @param esmFile       ESM file
     * @param applications  list of applications
     * @param delay         app delay time in seconds
     */
    public TriggerAppDelay(Plugin plugin,
                               String trigger,
                               String esmFile,
                               List<String> applications,
                               int delay) {
        super(plugin, trigger, esmFile);
        this.applications = applications;
        this.delay = delay;
    }
}
