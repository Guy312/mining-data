package com.publish;

import java.awt.image.BufferedImage;
import java.io.*;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.binary.Base64;

import java.util.Date;
import java.net.URI;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Response;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Created by 502669124 on 6/19/2017.
 */
@ControllerAdvice
@Controller
@PropertySource("classpath:application.properties")
public class Publish {

    private final Environment env;

    private final PostgresConnection pgc;

    @Autowired
    public Publish(Environment env, PostgresConnection pgc) {
        this.env = env;
        this.pgc = pgc;
    }

    @RequestMapping(value = "/ping", produces = "text/plain")
    @ResponseBody
    public String ping() {
        return "hello";
    }

    @RequestMapping(value = "/job", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> publishJob(HttpEntity<String> httpEntity) throws Exception {

        String msg = "";
        int total = 0;
        int success = 0;
        int fail = 0;
        JSONArray ja = new JSONArray();
        ja.add(new JSONObject());

        mainLoop:
        while (true) {
            // Parsing json body
            ////////////////////////////////////////////////////////////////////////////////
            String jsonString = httpEntity.getBody();
            JSONObject main_jo;
            JSONObject jo;
            try {
                JSONParser parser = new JSONParser();
                main_jo = (JSONObject) parser.parse(jsonString);
                ja = (JSONArray) main_jo.get("jobs");
            } catch (Exception e) {
                msg = "Failed to parse json: " + e.getMessage();
                break mainLoop;
            }

            // Connecting to postgres
            ////////////////////////////////////////////////////////////////////////////////
            pgc.connect();
            Connection connection = pgc.connection;

            ////////////////////////////////////////////////////////////////////////////////
            total = ja.size();

            for (int i = 0; i < total; i++) {
                jo = (JSONObject) ja.get(i);

                Record rec = new Record();
                String errorMsg = rec.updateFromJsonObject(jo);
                if (errorMsg.length() > 0) {
                    msg += "[" + i + "] ";
                    msg += errorMsg + "A";
                    fail++;
                } else {
                    success++;
                    // INSERT INTO
                    try {
                        pgc.insertRecord(rec);
                    } catch (Exception e) {
                        msg += "[" + i + "] ";
                        msg += e.getMessage() + "B\n";
                        final StringWriter sw = new StringWriter();
                        final PrintWriter pw = new PrintWriter(sw, true);
                        e.printStackTrace(pw);
                        msg += sw.getBuffer().toString();
                        fail++;
                        success--;
                    }
                }
            }

            break mainLoop;
        }
//        response.put("total", total);
//        response.put("success", success);
//        response.put("fail", fail);
        JSONObject response = new JSONObject();
        if ((fail > 0) || (msg.length() > 0)) {
            response.put("message", msg);
            return new ResponseEntity<Object>(response, HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<Object>("", HttpStatus.OK);
        }


    }

    @RequestMapping(value = "/job/{job_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> pullJob(@PathVariable("job_id") String job_id) {
        String msg = "";
        Record rec;
        try {
            pgc.connect();
            rec = pgc.selectRecord(job_id);
            pgc.closeConnection();

//            Gson gson = new GsonBuilder().setPrettyPrinting().create();
//            String jsonOutput = gson.toJson(rec.toJsonObject());
//            output = jsonOutput;
            return new ResponseEntity<Object>((JSONObject) rec.toJsonObject(), HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            msg = e.getMessage();
            JSONObject response = new JSONObject();
            response.put("message", msg);
            if (msg.contains("job")){
                return new ResponseEntity<Object>(response, HttpStatus.NOT_FOUND);
            } else {
                return new ResponseEntity<Object>(response, HttpStatus.BAD_REQUEST);
            }
        }
    }

    @RequestMapping(value = "/job/{job_id}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> deleteJob(@PathVariable("job_id") String job_id) {
        try {
            pgc.connect();
            pgc.deleteRecord(job_id);
            pgc.closeConnection();
            return new ResponseEntity<Object>("", HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();

            JSONObject response = new JSONObject();
            response.put("message", e.getMessage());
            return new ResponseEntity<Object>(response, HttpStatus.BAD_REQUEST);
        }
    }


    @RequestMapping(value = "/image/{job_id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<byte[]> getImage(@PathVariable("job_id") String job_id) {
        String output = "";
        try {
            pgc.connect();
            Connection connection = pgc.connection;
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from images WHERE job_id=" + job_id);
            if (rs.next() == false) {
                throw new Exception("image of job #" + job_id + " not found");
            }
            ;


            byte[] imageInByte = rs.getBytes(2);


            HttpHeaders header = new HttpHeaders();
            header.setContentType(MediaType.IMAGE_JPEG);
            header.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=image.jpg");
            header.setContentLength(imageInByte.length);

            pgc.closeConnection();
            return new ResponseEntity<byte[]>(imageInByte, header, HttpStatus.OK);
        } catch (Exception e) {
            // e.printStackTrace();
            JSONObject response = new JSONObject();
            response.put("message", e.getMessage());
            HttpHeaders header = new HttpHeaders();
            header.setContentType(MediaType.APPLICATION_JSON);

            header.setContentLength(response.toString().length());
            ResponseEntity re = new ResponseEntity<byte[]>(response.toString().getBytes(),header, HttpStatus.BAD_REQUEST);
            return re;
//            re.
////            e.printStackTrace();
//            HttpHeaders header = new HttpHeaders();
//            header.setContentType(MediaType.APPLICATION_JSON);
//
//            header.setContentLength(0);
//
//            return new HttpEntity<byte[]>(header);
        }
    }

    @RequestMapping(value = "/image/{job_id}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> deleteImage(@PathVariable("job_id") String job_id) {
        try {
            pgc.connect();
            Connection connection = pgc.connection;
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from images WHERE job_id=" + job_id);
            if (rs.next() == false) {
                throw new Exception("image with job_id #" + job_id + " not found");
            }

            boolean f = stmt.execute("DELETE from images WHERE job_id=" + job_id);
            pgc.closeConnection();
            return new ResponseEntity<Object>("", HttpStatus.OK);
        } catch (Exception e) {
            // e.printStackTrace();
            JSONObject response = new JSONObject();
            response.put("message", e.getMessage());
            return new ResponseEntity<Object>(response, HttpStatus.BAD_REQUEST);
        }

    }


    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseBody
    public String handleError2(MaxUploadSizeExceededException e, RedirectAttributes redirectAttributes) {

        //redirectAttributes.addFlashAttribute("message", e.getCause().getMessage());
        return "KUZIA";//e.getCause().getMessage();

    }


    @RequestMapping(value = "/image/{job_id}", method = RequestMethod.POST, consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<?> publishImage(@RequestParam(value = "image", required = true) MultipartFile file, @PathVariable("job_id") String job_id) {
        pgc.connect();
        Connection connection = pgc.connection;

        try {
            if (!file.getContentType().equals("image/jpeg")) {
                throw new RuntimeException("Only JPG images are accepted");
            }

            byte[] imageInByte = file.getBytes();

            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from images WHERE job_id=" + job_id);
            if (rs.next() != false) {
                stmt.execute("DELETE from images WHERE job_id=" + job_id);
            }
            ;


            ////////////////////////////////////////////////////////////////////////////////
            PreparedStatement ps = connection.prepareStatement("INSERT INTO images (job_id,image) " + "VALUES (?,?)");
            ps.setLong(1, Long.parseLong(job_id));
            ps.setBytes(2, imageInByte);
            ps.execute();
            pgc.closeConnection();
            return new ResponseEntity<Object>("", HttpStatus.OK);
        } catch (Exception e) {
            String msg = "Failed to insert image to database: " + e.getMessage();
            JSONObject response = new JSONObject();
            response.put("message", msg);
            return new ResponseEntity<Object>(response, HttpStatus.BAD_REQUEST);


        }
    }


    @RequestMapping(value = "/test000")
    @ResponseBody
    public String psg() {
        String output = "<h1> Postgres test </h1>";
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();

        String host = env.getProperty("postgress.host");
        String port = env.getProperty("postgress.port");
        String db_name = env.getProperty("postgress.db_name");
        String user = env.getProperty("postgress.user");
        String password = env.getProperty("postgress.password");
        try {
            Class.forName("org.postgresql.Driver");
            Connection connection = null;

            connection = DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/" + db_name + "?user=" + user + "&password=" + password);
            //connection = DriverManager.getConnection("postgres://"+user+":"+password+"@" + host + ":" + port + "/" + db_name );
            Statement stmt = connection.createStatement();
            // SELECT QUERY
            ResultSet rs = stmt.executeQuery("SELECT * FROM test");
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnsNumber = rsmd.getColumnCount();
            while (rs.next()) {
                for (int i = 1; i <= columnsNumber; i++) {
                    if (i > 1) output += ",  ";
                    String columnValue = rs.getString(i);
                    output += columnValue + " " + rsmd.getColumnName(i);
                }
                output += "<br>";
            }

            stmt = connection.createStatement();
            // INSERT INTO
            stmt.execute("INSERT INTO test (t001,t002) " + "VALUES ('" + dateFormat.format(date) + "',12);");

            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
            output += "(" + host + ")<br>";
            output += "(" + port + ")<br>";
            output += "(" + db_name + ")<br>";
            output += "(" + user + ")<br>";
            output += "(" + password + ")<br><br>";
            output += e.toString();

        } finally {

        }
        System.out.println("Hello World! aaaa");


        return output;
    }

    /*
        test updateFromJsonObject on Record object
     */
    @RequestMapping(value = "/test001")
    @ResponseBody
    public String test001() {
        String jsonString = "{\n" +
                "  \"start_date\": \"2017/06/19 14:20:31\",\n" +
                "  \"end_date\": \"2017/06/19 14:36:11\",\n" +
                "  \"job_id\": \"1238547\",\n" +
                "  \"poly_id\": \"85946241\",\n" +
                "  \"pile_name\": \"testPile\",\n" +
                "  \"surface\": \"58900125.541\",\n" +
                "  \"volume\": \"9554632.005\"\n" +
                "}";
        Record rec = new Record();
        try {
            JSONParser parser = new JSONParser();
            JSONObject jo = (JSONObject) parser.parse(jsonString);
            rec.updateFromJsonObject(jo);


        } catch (Exception e) {
            e.printStackTrace();
        }
        return rec.toJsonObject().toJSONString();
    }


    /*
    test insert one job record to postgres db
     */
    @RequestMapping(value = "/test002")
    @ResponseBody
    public String test002() {
        String jsonString = "{\n" +
                "  \"start_date\": \"2017/06/27 14:20:31\",\n" +
                "  \"end_date\": \"2017/06/27 14:36:11\",\n" +
                "  \"job_id\": \"1238549\",\n" +
                "  \"poly_id\": \"85946241\",\n" +
                "  \"pile_name\": \"testPile03\",\n" +
                "  \"surface\": \"58900125.541\",\n" +
                "  \"volume\": \"9554632.005\"\n" +
                "}";
        Record rec = new Record();
        try {
            JSONParser parser = new JSONParser();
            JSONObject jo = (JSONObject) parser.parse(jsonString);
            rec.updateFromJsonObject(jo);
            rec.end_date = new Date(); // changing the end date to test

            pgc.connect();
            pgc.insertRecord(rec);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return rec.toJsonObject().toJSONString();
    }


    /*
       test receive json from the body
    */
    @RequestMapping(value = "/test003")
    @ResponseBody
    public String test003(HttpEntity<String> httpEntity) {
        String jsonString = httpEntity.getBody();

        Record rec = new Record();
        try {
            JSONParser parser = new JSONParser();
            JSONObject jo = (JSONObject) parser.parse(jsonString);
            rec.updateFromJsonObject(jo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Record after reparsing:<br>" + rec.toJsonObject().toJSONString();
    }

    /*
       send json to test003
    */
    @RequestMapping(value = "/test004")
    @ResponseBody
    public String test004() {
        String output = "";
        String jsonString = "{\n" +
                "  \"start_date\": \"2017/06/19 14:20:31\",\n" +
                "  \"end_date\": \"2017/06/19 14:36:11\",\n" +
                "  \"job_id\": \"1238547\",\n" +
                "  \"poly_id\": \"85946241\",\n" +
                "  \"pile_name\": \"testPile\",\n" +
                "  \"surface\": \"58900125.541\",\n" +
                "  \"volume\": \"9554632.005\"\n" +
                "}";

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory
                = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);

        try {
            String urlOverHttps = env.getProperty("application.root") + "/test003";
            URI uri = new URI(urlOverHttps);
            // RequestEntity<String> requestEntity = new RequestEntity<String>(input, headers, HttpMethod.POST, uri);
            RequestEntity<String> requestEntity = RequestEntity.post(uri)
                    .header("Content-Type", "application/json")
                    .body(jsonString);
            RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
            ResponseEntity<String> response = restTemplate.exchange(urlOverHttps, HttpMethod.POST, requestEntity, String.class);
            output = response.getBody().toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    /*
       send json to publish
    */
    @RequestMapping(value = "/test005")
    @ResponseBody
    public String test005() {
        String output = "";
        String jsonString = "[{\n" +
                "  \"start_date\": \"2017/06/19 14:20:31\",\n" +
                "  \"end_date\": \"2017/06/19 14:36:11\",\n" +
                "  \"job_id\": \"1238547\",\n" +
                "  \"poly_id\": \"85946241\",\n" +
                "  \"pile_name\": \"testPile01\",\n" +
                "  \"surface\": \"100.541\",\n" +
                "  \"volume\": \"9554632.005\"\n" +
                "}," + "{\"start_date\": \"2017/06/19 14:20:31\",\n" +
                "  \"end_date\": \"2017/06/19 14:36:11\",\n" +
                "  \"job_id\": \"1238548\",\n" +
                "  \"poly_id\": \"85946241\",\n" +
                "  \"pile_name\": \"testPile02\",\n" +
                "  \"surface\": \"200.541\",\n" +
                "  \"volume\": \"9554632.005\"\n" +
                "}]";


        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory
                = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);

        try {
            String urlOverHttps = env.getProperty("application.root") + "/publish";
            //    String urlOverHttps = "https://mining-data.run.aws-usw02-pr.ice.predix.io/publish";
            URI uri = new URI(urlOverHttps);
            // RequestEntity<String> requestEntity = new RequestEntity<String>(input, headers, HttpMethod.POST, uri);
            RequestEntity<String> requestEntity = RequestEntity.post(uri)
                    .header("Content-Type", "application/json")
                    .body(jsonString);
            RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
            ResponseEntity<String> response = restTemplate.exchange(urlOverHttps, HttpMethod.POST, requestEntity, String.class);
            output = response.getBody().toString();
        } catch (Exception e) {
            e.printStackTrace();
            output = e.getMessage();
        }
        return output;
    }


    /*
      select from DB
   */
    @RequestMapping(value = "/test006")
    @ResponseBody
    public String test006() {
        String output = "";
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();

        String host = env.getProperty("postgress.host");
        String port = env.getProperty("postgress.port");
        String db_name = env.getProperty("postgress.db_name");
        String user = env.getProperty("postgress.user");
        String password = env.getProperty("postgress.password");


        try {

            Class.forName("org.postgresql.Driver");
            Connection connection = null;

            connection = DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/" + db_name + "?user=" + user + "&password=" + password);
            Statement stmt = connection.createStatement();
            stmt = connection.createStatement();


            ResultSet rs = stmt.executeQuery("SELECT * FROM joblog WHERE poly_id=85946241");
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnsNumber = rsmd.getColumnCount();
            JSONArray ja = new JSONArray();
            while (rs.next()) {
                JSONObject jo = new JSONObject();
                for (int i = 1; i <= columnsNumber; i++) {

                    String columnValue = rs.getString(i);
                    output += rsmd.getColumnName(i) + "," + columnValue + "<br>";
                    jo.put(rsmd.getColumnName(i), columnValue);
                }
                Record rec = new Record();
                rec.updateFromJsonObject(jo);
                ja.add(rec.toJsonObject());
            }
            output = ja.toJSONString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //output = rec.toJsonObject().toJSONString();

        return output;
    }

    /*
      receive an image Base64 encoded
   */
    @RequestMapping(value = "/test007")
    @ResponseBody
    public String test007(HttpEntity<String> httpEntity) {
        try {
            String imageString = httpEntity.getBody();
            Base64 base = new Base64(false);
            byte[] imageInByte = base.decode(imageString);
            InputStream in = new ByteArrayInputStream(imageInByte);
            BufferedImage im = ImageIO.read(in);
            ImageIO.write(im, "jpg", new File("C:\\Users\\502669124\\Box Sync\\JavaProjects\\AIROBOTICS\\mining-data\\src\\main\\webapp\\resources\\img\\myoutimage.jpg"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        String output = "image written to file";
        return output;
    }

    /*
      send an image Base64 encoded
   */
    @RequestMapping(value = "/test008")
    @ResponseBody
    public String test008() {
        String encodedImage;
        String output = "";
        try {
            BufferedImage img = ImageIO.read(new File("C:\\Users\\502669124\\Box Sync\\JavaProjects\\AIROBOTICS\\mining-data\\src\\main\\webapp\\resources\\img\\myimage.jpg"));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            baos.flush();
            Base64 base = new Base64(false);
            encodedImage = base.encodeToString(baos.toByteArray());
            baos.close();
            // encodedImage = java.net.URLEncoder.encode(encodedImage, "ISO-8859-1");
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }


        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory
                = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);

        try {
            String urlOverHttps = env.getProperty("application.root") + "/test007";
            URI uri = new URI(urlOverHttps);
            // RequestEntity<String> requestEntity = new RequestEntity<String>(input, headers, HttpMethod.POST, uri);
            RequestEntity<String> requestEntity = RequestEntity.post(uri)
                    .header("Content-Type", "image/jpeg")
                    .body(encodedImage);
            RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
            ResponseEntity<String> response = restTemplate.exchange(urlOverHttps, HttpMethod.POST, requestEntity, String.class);
            output = response.getBody().toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;

    }

    /*
     save an image to postgres DB
  */
    @RequestMapping(value = "/test009")
    @ResponseBody
    public String test009() {
        String output = "insert executed";
        try {
            BufferedImage img = ImageIO.read(new File("C:\\Users\\502669124\\Box Sync\\JavaProjects\\AIROBOTICS\\mining-data\\src\\main\\webapp\\resources\\img\\myimage.jpg"));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            baos.flush();
            byte[] imgba = baos.toByteArray();
            baos.close();

            ///////////////////////
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();

            String host = env.getProperty("postgress.host");
            String port = env.getProperty("postgress.port");
            String db_name = env.getProperty("postgress.db_name");
            String user = env.getProperty("postgress.user");
            String password = env.getProperty("postgress.password");


            Class.forName("org.postgresql.Driver");
            Connection connection = null;
            connection = DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/" + db_name + "?user=" + user + "&password=" + password);
            Statement stmt = connection.createStatement();
            //stmt = connection.createStatement();
            // INSERT INTO
            PreparedStatement ps = connection.prepareStatement("INSERT INTO joblog2 (image) " + "VALUES (?)");
            ps.setBytes(1, imgba);
            ps.execute();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    /*
        get an image from potgres DB
     */
    @RequestMapping(value = "/test010")
    @ResponseBody
    public String test010() {
        String output = "image saved";
        String encodedImage;
        try {


            ///////////////////////
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();

            String host = env.getProperty("postgress.host");
            String port = env.getProperty("postgress.port");
            String db_name = env.getProperty("postgress.db_name");
            String user = env.getProperty("postgress.user");
            String password = env.getProperty("postgress.password");


            Class.forName("org.postgresql.Driver");
            Connection connection = null;
            connection = DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/" + db_name + "?user=" + user + "&password=" + password);
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from joblog2");
            rs.next();
            byte[] imageInByte = rs.getBytes(1);

            try {
                InputStream in = new ByteArrayInputStream(imageInByte);
                BufferedImage im = ImageIO.read(in);
                ImageIO.write(im, "jpg", new File("C:\\Users\\502669124\\Box Sync\\JavaProjects\\AIROBOTICS\\mining-data\\src\\main\\webapp\\resources\\img\\myoutimage2.jpg"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }


    /*
        get an image from potgres DB and show to browser
     */
    @RequestMapping(value = "/test011")
    @ResponseBody
    public String test011() {
        String output = "";
        try {


            ///////////////////////
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();

            String host = env.getProperty("postgress.host");
            String port = env.getProperty("postgress.port");
            String db_name = env.getProperty("postgress.db_name");
            String user = env.getProperty("postgress.user");
            String password = env.getProperty("postgress.password");


            Class.forName("org.postgresql.Driver");
            Connection connection = null;
            connection = DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/" + db_name + "?user=" + user + "&password=" + password);
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from images");
            rs.next();
            byte[] imageInByte = rs.getBytes(1);

            Base64 base = new Base64(false);
            String encodedImage = base.encodeToString(imageInByte);

            output = "<img src=\"data:image/jpeg;base64," + encodedImage + "\">";
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    /*
    send an image Base64 encoded to /image/{job_id}
 */
    @RequestMapping(value = "/test012")
    @ResponseBody
    public String test012() {
        String encodedImage;
        String output = "";
        try {
            BufferedImage img = ImageIO.read(new File("C:\\Users\\502669124\\Box Sync\\JavaProjects\\AIROBOTICS\\mining-data\\src\\main\\webapp\\resources\\img\\myimage.jpg"));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            baos.flush();
            Base64 base = new Base64(false);
            encodedImage = base.encodeToString(baos.toByteArray());
            baos.close();
            // encodedImage = java.net.URLEncoder.encode(encodedImage, "ISO-8859-1");
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }


        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory
                = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);

        try {
            //String urlOverHttps = env.getProperty("application.root") + "/image/584695217";
            String urlOverHttps = "https://mining-data.run.aws-usw02-pr.ice.predix.io/image/584695217";
            URI uri = new URI(urlOverHttps);
            // RequestEntity<String> requestEntity = new RequestEntity<String>(input, headers, HttpMethod.POST, uri);
            RequestEntity<String> requestEntity = RequestEntity.post(uri)
                    .header("Content-Type", "image/jpeg")
                    .body(encodedImage);
            RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
            ResponseEntity<String> response = restTemplate.exchange(urlOverHttps, HttpMethod.POST, requestEntity, String.class);
            output = response.getBody().toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;

    }

    /*
        receive geo-json
    */
    @RequestMapping(value = "/polygon", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> publishPolygon(HttpEntity<String> httpEntity) {
        try {
            String jsonString = httpEntity.getBody();
            JSONParser parser = new JSONParser();
            JSONObject polygonJson = (JSONObject) parser.parse(jsonString);
            if (!polygonJson.containsKey("id")) {
                throw new Exception("id field missing");
            }
            if (!polygonJson.containsKey("coordinates")) {
                throw new Exception("coordinates field missing");
            }

            String poly_id = String.valueOf(polygonJson.get("id"));
            String poly_json = polygonJson.toJSONString();
            ///// insert
            try {
                pgc.connect();
                Connection connection = pgc.connection;
                Statement stmt = connection.createStatement();
                stmt.execute("INSERT INTO polys (poly_id,poly_json) " + "VALUES (" + poly_id + ",'" + poly_json + "');");

            } catch (Exception e) {
                e.printStackTrace();
                JSONObject response = new JSONObject();
                response.put("message", e.getMessage());
                return new ResponseEntity<Object>(response, HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JSONObject response = new JSONObject();
            response.put("message", e.getMessage());
            return new ResponseEntity<Object>(response, HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<Object>("", HttpStatus.OK);
    }

    /*
    send geojson to /poly
     */
    @RequestMapping(value = "/test014")
    @ResponseBody
    public String test014() {
        String geojsonString = "{\n" +
                "  \"type\": \"FeatureCollection\",\n" +
                "  \"features\": [\n" +
                "     {\n" +
                "      \"type\": \"Feature\",\n" +
                "      \"properties\": {\"poly_id\":\"85946241\"},\n" +
                "      \"geometry\": {\n" +
                "        \"type\": \"Polygon\",\n" +
                "        \"coordinates\": [\n" +
                "          [\n" +
                "            [\n" +
                "              -325.1932257413864,\n" +
                "              32.05825914330015\n" +
                "            ],\n" +
                "            [\n" +
                "              -325.1932954788208,\n" +
                "              32.058527380013686\n" +
                "            ],\n" +
                "            [\n" +
                "              -325.1934295892715,\n" +
                "              32.05855920465632\n" +
                "            ],\n" +
                "            [\n" +
                "              -325.19349932670593,\n" +
                "              32.058509194498626\n" +
                "            ],\n" +
                "            [\n" +
                "              -325.19335985183716,\n" +
                "              32.05820004018972\n" +
                "            ],\n" +
                "            [\n" +
                "              -325.1932257413864,\n" +
                "              32.05825914330015\n" +
                "            ]\n" +
                "          ]\n" +
                "        ]\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\": \"Feature\",\n" +
                "      \"properties\": {\"poly_id\":\"85946242\"},\n" +
                "      \"geometry\": {\n" +
                "        \"type\": \"Polygon\",\n" +
                "        \"coordinates\": [\n" +
                "          [\n" +
                "            [\n" +
                "              -325.1932418346405,\n" +
                "              32.058509194498626\n" +
                "            ],\n" +
                "            [\n" +
                "              -325.1931077241897,\n" +
                "              32.05854556552513\n" +
                "            ],\n" +
                "            [\n" +
                "              -325.19316136837006,\n" +
                "              32.058800162305616\n" +
                "            ],\n" +
                "            [\n" +
                "              -325.19332230091095,\n" +
                "              32.05882744049009\n" +
                "            ],\n" +
                "            [\n" +
                "              -325.19337594509125,\n" +
                "              32.0587592450137\n" +
                "            ],\n" +
                "            [\n" +
                "              -325.1932418346405,\n" +
                "              32.058509194498626\n" +
                "            ]\n" +
                "          ]\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";


        String output = "";
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory
                = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);

        try {
            String urlOverHttps = env.getProperty("application.root") + "/poly";
            //String urlOverHttps = "https://mining-data.run.aws-usw02-pr.ice.predix.io/poly";
            URI uri = new URI(urlOverHttps);
            // RequestEntity<String> requestEntity = new RequestEntity<String>(input, headers, HttpMethod.POST, uri);
            RequestEntity<String> requestEntity = RequestEntity.post(uri)
                    .header("Content-Type", "application/json")
                    .body(geojsonString);
            RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
            ResponseEntity<String> response = restTemplate.exchange(urlOverHttps, HttpMethod.POST, requestEntity, String.class);
            output = response.getBody().toString();
        } catch (Exception e) {
            e.printStackTrace();
            output = e.getMessage();
        }

        return output;
    }

    /*
 receive geo-json
*/
    @RequestMapping(value = "/polygon/{poly_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> getPoly(@PathVariable("poly_id") String poly_id) {
        String poly_json = "";
        try {
            pgc.connect();
            Connection connection = pgc.connection;
            Statement stmt = connection.createStatement();

            ResultSet rs = stmt.executeQuery("SELECT * FROM polys WHERE poly_id=" + poly_id);

            if (rs.next() == false) {
                throw new Exception("poly_id #" + poly_id + " not found");
            }
            poly_json = rs.getString(2);

            pgc.closeConnection();
            return new ResponseEntity<Object>(poly_json, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject response = new JSONObject();
            String msg = e.getMessage();
            response.put("message", msg);
            if (msg.contains("poly_id")){
                return new ResponseEntity<Object>(response, HttpStatus.NOT_FOUND);
            } else {
                return new ResponseEntity<Object>(response, HttpStatus.BAD_REQUEST);
            }
        }

    }

    @RequestMapping(value = "/polygon/{poly_id}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> deletePoly(@PathVariable("poly_id") String poly_id) {
        try {
            pgc.connect();
            Connection connection = pgc.connection;
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from polys WHERE poly_id=" + poly_id);
            if (rs.next() == false) {
                throw new Exception("poly_id #" + poly_id + " not found");
            }
            ;
            boolean f = stmt.execute("DELETE from polys WHERE poly_id=" + poly_id);
            pgc.closeConnection();
            return new ResponseEntity<Object>("", HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject response = new JSONObject();
            response.put("message", e.getMessage());
            return new ResponseEntity<Object>(response, HttpStatus.BAD_REQUEST);
        }
    }


    /*
        delete job 1238547
   */
    @RequestMapping(value = "/test015")
    @ResponseBody
    public String test015() {
        String output = "";
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory
                = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);

        try {
            //String urlOverHttps = env.getProperty("application.root") + "/job/1238547";
            String urlOverHttps = "https://mining-data.run.aws-usw02-pr.ice.predix.io/job/1238547";

            RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
            ResponseEntity<String> response = restTemplate.exchange(urlOverHttps, HttpMethod.DELETE, null, String.class);
            output = response.getBody().toString();
        } catch (Exception e) {
            e.printStackTrace();
            output = e.getMessage();
        }

        return output;
    }


    /*
       delete image 584695217
  */
    @RequestMapping(value = "/test016")
    @ResponseBody
    public String test016() {
        String output = "";
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory
                = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);

        try {
            //String urlOverHttps = env.getProperty("application.root") + "/image/584695217";
            String urlOverHttps = "https://mining-data.run.aws-usw02-pr.ice.predix.io/image/584695217";

            RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
            ResponseEntity<String> response = restTemplate.exchange(urlOverHttps, HttpMethod.DELETE, null, String.class);
            output = response.getBody().toString();
        } catch (Exception e) {
            e.printStackTrace();
            output = e.getMessage();
        }

        return output;
    }


    /*
       delete polygon 584695217
  */
    @RequestMapping(value = "/test017")
    @ResponseBody
    public String test017() {
        String output = "";
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory
                = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);

        try {
            //String urlOverHttps = env.getProperty("application.root") + "/poly/85946241";
            String urlOverHttps = "https://mining-data.run.aws-usw02-pr.ice.predix.io/poly/85946241";

            RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
            ResponseEntity<String> response = restTemplate.exchange(urlOverHttps, HttpMethod.DELETE, null, String.class);
            output = response.getBody().toString();
        } catch (Exception e) {
            e.printStackTrace();
            output = e.getMessage();
        }

        return output;
    }
}

