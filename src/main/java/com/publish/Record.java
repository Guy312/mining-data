package com.publish;

import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by 502669124 on 6/19/2017.
 */
public class Record {

    Date start_date;
    Date end_date;
    String job_id;
    String poly_id;
    String pile_name;
    double surface;
    double volume;

    JSONObject poly;
    String image_uri;

    private DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");


    public JSONObject toJsonObject() {
        JSONObject jo = new JSONObject();
        jo.put("start_date", start_date.getTime());
        jo.put("end_date", end_date.getTime());
        jo.put("job_id", Long.parseLong(job_id));
        jo.put("poly_id", Long.parseLong(poly_id));
        jo.put("pile_name", pile_name);
        jo.put("surface",   surface);
        jo.put("volume", volume);
        return jo;
    }

    public String updateFromJsonObject(JSONObject jo) throws Exception{
        String msg = "";

        List<String> fields = Arrays.asList("start_date", "end_date", "job_id", "poly_id", "pile_name", "surface", "volume");

        for (String s : fields) {
            if (!jo.containsKey(s)) {
                msg = "Missing " + s + " field.";
            }
        }

        if (msg.length() > 0) {
            return msg;
        }
        try {
            this.start_date = new Date((Long) jo.get("start_date"));
            this.end_date = new Date((Long) jo.get("end_date"));
            this.job_id = String.valueOf(jo.get("job_id"));
            this.poly_id = String.valueOf(jo.get("poly_id"));
            this.pile_name = (String) jo.get("pile_name");
            this.surface = Double.parseDouble( String.valueOf(jo.get("surface")));
            this.volume = Double.parseDouble( String.valueOf(jo.get("volume")));
        } catch (Exception e) {

//            FileWriter fw = new FileWriter("exception.txt", true);
//            PrintWriter pw = new PrintWriter(fw);
//            e.printStackTrace (pw);
//            fw.close();

            //e.printStackTrace();
            return e.getMessage();
//            final StringWriter sw = new StringWriter();
//            final PrintWriter pw = new PrintWriter(sw, true);
//            e.printStackTrace(pw);
//            return sw.getBuffer().toString();
        }

        return "";
    }

}

