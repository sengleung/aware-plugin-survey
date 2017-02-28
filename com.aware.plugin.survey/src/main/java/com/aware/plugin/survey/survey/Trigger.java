package com.aware.plugin.survey.survey;

import com.aware.plugin.survey.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Trigger {
    public Plugin plugin;
    public String trigger;
    public String esm;
    //public List<String> applications;

    public Trigger(Plugin plugin,
                   String trigger,
                   String esmFile) {
        this.plugin = plugin;
        this.trigger = trigger;
        this.esm = readESMFile(esmFile);
        //this.applications = applications;
    }

    public String readESMFile(String esmFile) {
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
            throw new RuntimeException("Error in reading CSV file: " + ex);
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
