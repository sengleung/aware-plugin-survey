package com.aware.plugin.survey.survey;

import com.aware.plugin.survey.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration file parser for ESM activation triggers and parameters.
 *
 * @author  Seng Leung
 * @version 1.0
 */
public class ConfigFile {
    private Plugin plugin;

    public ConfigFile(Plugin plugin) {
        this.plugin = plugin;

    }

    /**
     * ESM trigger options.
     */
     private enum TriggerType {
        TIME("time"), APP_OPEN_CLOSE("app-open-close");

        private String name;

        TriggerType(String name) {
            this.name = name;
        }

        @Override
        public String toString(){
            return name;
        }
    }

    /**
     * Parse the configuration file and return ESM triggers.
     *
     * @return  list of triggers
     */
    public List<Trigger> getTriggers() {
        int id = plugin.getResources().getIdentifier("esm", "raw", plugin.getPackageName());
        InputStream inputStream = plugin.getResources().openRawResource(id);

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        List<Trigger> triggers;
        try {
            String line;
            triggers = new ArrayList<>();
            TriggerType triggerType = null;

            List<Object> parameters = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                if (line.length() < 1) {
                } else if (line.charAt(0) == '[') {
                } else if (line.startsWith("Trigger", 0)) {
                    String[] triggerName = line.split("=");
                    switch (triggerName[1]) {
                        case "time":
                            parameters.add(TriggerType.TIME.toString());
                            triggerType = TriggerType.TIME;
                            break;
                        case "app-open-close":
                            parameters.add(TriggerType.APP_OPEN_CLOSE.toString());
                            triggerType = TriggerType.APP_OPEN_CLOSE;
                            break;
                        default:
                            break;
                    }
                } else if (line.startsWith("ESM", 0)) {
                    String[] esmFile = line.split("=");
                    parameters.add(esmFile[1]);
                } else {
                    switch (triggerType) {
                        case TIME:
                            if (line.startsWith("Times", 0)) {
                                String[] timesList = line.split("=");
                                String[] timesArr = timesList[1].split(",");
                                parameters.add(new ArrayList<>(Arrays.asList(timesArr)));
                                triggers.add(new TriggerTime(plugin,
                                        (String) parameters.get(0),
                                        (String) parameters.get(1),
                                        (List<String>) parameters.get(2)
                                ));
                                parameters.clear();
                            }
                            break;
                        case APP_OPEN_CLOSE:
                            if (line.startsWith("Applications", 0)) {
                                String[] appList = line.split("=");
                                String[] appsArr = appList[1].split(",");
                                parameters.add(new ArrayList<>(Arrays.asList(appsArr)));
                            } else if (line.startsWith("Open", 0)) {
                                String[] open = line.split("=");
                                boolean openBoolean = Boolean.parseBoolean(open[1]);
                                parameters.add(openBoolean);
                            } else if (line.startsWith("Close", 0)) {
                                String[] close = line.split("=");
                                boolean closeBoolean = Boolean.parseBoolean(close[1]);
                                parameters.add(closeBoolean);
                                triggers.add(new TriggerAppOpenClose(plugin,
                                        (String) parameters.get(0),
                                        (String) parameters.get(1),
                                        (List<String>) parameters.get(2),
                                        (Boolean) parameters.get(3),
                                        (Boolean) parameters.get(4)
                                ));
                                parameters.clear();
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error in reading CSV file: " + ex);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                throw new RuntimeException("Error while closing input stream: " + e);
            }
        }
        return triggers;
    }
}
