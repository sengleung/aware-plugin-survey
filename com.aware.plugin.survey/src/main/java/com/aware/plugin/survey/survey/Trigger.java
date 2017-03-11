package com.aware.plugin.survey.survey;

import android.util.Log;

import com.aware.plugin.survey.Plugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Trigger superclass.
 */
public class Trigger {
    public Plugin plugin;
    public String trigger;
    public String esm;
    public boolean surveyTriggered;

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
        this.surveyTriggered = false;
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

    public void setTrigger(String triggerString){
        try {
            JSONArray esmArray = new JSONArray(this.esm);
            for(int i = 0; i<esmArray.length(); i++){
                JSONObject esm =  esmArray.getJSONObject(i).getJSONObject("esm");
                esm.put("esm_trigger", triggerString);
            }
            Log.d("ESM", "set trigger to: "+triggerString);
            this.esm = esmArray.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("ESM", "could not set trigger!!");
        }
    }

}
