package com.aware.plugin.survey.survey;

import com.aware.plugin.survey.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Trigger superclass.
 */
public class Trigger {
    public Plugin plugin;
    public String trigger;
    public String esm;

    /**
     * Trigger superclass constructor.
     *
     * @param plugin    survey plugin
     * @param trigger   trigger name
     * @param esmFile   ESM file name
     */
    public Trigger(Plugin plugin,
                   String trigger,
                   String esmFile) {
        this.plugin = plugin;
        this.trigger = trigger;
        this.esm = readESMFile(esmFile);
    }

    /**
     * Parse ESM file and convert to string.
     *
     * @param  esmFile  ESM file name
     * @return string of ESM
     */
    private String readESMFile(String esmFile) {
        int id = plugin.getResources().getIdentifier(esmFile, "raw", plugin.getPackageName());
        InputStream inputStream = plugin.getResources().openRawResource(id);
        String esms = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                esms += line + "\n";
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error in reading JSON file: " + ex);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                throw new RuntimeException("Error while closing input stream: " + e);
            }
        }
        return esms;
    }
}
