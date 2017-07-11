package com.publish;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by 502669124 on 6/21/2017.
 */
public class PostgresConnection {

    private final Environment env;

    public Connection connection = null;

    @Autowired
    public PostgresConnection(Environment env) {
        this.env = env;
    }

    public boolean connect() {
        String host = env.getProperty("postgress.host");
        String port = env.getProperty("postgress.port");
        String db_name = env.getProperty("postgress.db_name");
        String user = env.getProperty("postgress.user");
        String password = env.getProperty("postgress.password");

        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/" + db_name + "?user=" + user + "&password=" + password);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void closeConnection() throws Exception {
        this.connection.close();
    }

    public void insertRecord(Record rec) throws Exception {
        if (this.connection == null) throw new Exception("no connection");

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        Statement stmt = this.connection.createStatement();
        stmt.execute("INSERT INTO joblog (job_id,poly_id,pile_name,start_date,end_date,surface,volume) " + "VALUES (" + rec.job_id + "," + rec.poly_id + ",'" + rec.pile_name + "','" + dateFormat.format(rec.start_date) + "','" + dateFormat.format(rec.end_date) + "'," + rec.surface + "," + rec.volume + ");");
    }


    public Record selectRecord(String job_id) throws Exception {
        if (this.connection == null) throw new Exception("no connection to database");
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * from joblog WHERE job_id=" + job_id);
        if (rs.next()==false) {
            throw new Exception("job #"+job_id+" not found");
        };
        ////////
        Record rec = new Record();
        rec.job_id = rs.getString("job_id");
        rec.poly_id = rs.getString("poly_id");
        rec.pile_name = rs.getString("pile_name");

        rec.start_date = new Date(rs.getTimestamp("start_date").getTime());
        rec.end_date = new Date(rs.getTimestamp("end_date").getTime());
        rec.surface = Double.parseDouble(rs.getString("surface"));
        rec.volume = Double.parseDouble(rs.getString("volume"));
        return rec;
    }

    public void deleteRecord(String job_id) throws Exception {
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * from joblog WHERE job_id=" + job_id);
        if (rs.next()==false) {
            throw new Exception("job #"+job_id+" not found");
        };
        stmt.execute("DELETE from joblog WHERE job_id=" + job_id);
    }
}
