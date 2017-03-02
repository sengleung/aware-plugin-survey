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
        this.open = open;
        this.close = close;
    }

    public void setESM() {

    }

}
