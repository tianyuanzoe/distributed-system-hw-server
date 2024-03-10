package Upic.servlets; /**
 * @author zoetian
 * @create 1/24/24
 */


import Upic.servlets.model.LiftRide;
import Upic.servlets.model.Response;
import Upic.servlets.rmqpool.RMQChannelFactory;
import Upic.servlets.rmqpool.RMQChannelPool;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Map;
import java.util.concurrent.TimeoutException;


public class SkierServlet extends HttpServlet {
    private static RMQChannelPool rmqChannelPool;
    private static ConnectionFactory factory;
//
    private static RMQChannelFactory rmqChannelFactory;

    private static final int MAX_CHANNEL = 100;

    private static final String QUEUE_NAME = "post_queue";
    private static final String USER_NAME = "yuan";
    private static final String PASSWORD = "yuan";
    private static final String HOST ="34.211.202.216";
//private static final String HOST ="localhost";


    private static final int PORT = 5672;


    public void init() {
        factory = new ConnectionFactory();
        factory.setHost(HOST);
        factory.setPort(PORT);
        factory.setUsername(USER_NAME);
        factory.setPassword(PASSWORD);
        try {
            Connection connection = factory.newConnection();
            rmqChannelFactory = new RMQChannelFactory(connection);
            rmqChannelPool = new RMQChannelPool(MAX_CHANNEL, rmqChannelFactory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
        System.out.println("hello init");


    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        String urlPath = req.getPathInfo();

        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("missing paramterers");
            return;
        }

        String[] urlParts = urlPath.split("/");

        if (!isValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("the path is not a valid one");
        } else {
            res.setStatus(HttpServletResponse.SC_OK);
            // do any sophisticated processing with urlParts which contains all the url params
            // TODO: process url params in `urlParts`
            int resortID = Integer.parseInt(urlParts[1]);
            String seasonID = urlParts[3];
            String dayID = urlParts[5];
            int skierID = Integer.parseInt(urlParts[7]);
            String result = getSkierDayVertical(resortID,seasonID,dayID,skierID);
            res.getWriter().write(result);
        }
    }

    private void sendToQueue(String result) throws Exception {
        Channel channel = rmqChannelPool.borrowObject();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        channel.basicPublish("", QUEUE_NAME, null, result.getBytes());
        rmqChannelPool.returnObject(channel);
    }


    protected  void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        Gson gson = new Gson();
        String urlPath = req.getPathInfo();
        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("missing paramterers");
            return;
        }
        StringBuilder sb = new StringBuilder();
        try{
            String s;
            while((s = req.getReader().readLine()) != null){
                sb.append(s);
            }
            Map<String, Object> jsonMap = gson.fromJson(sb.toString(), Map.class);
            if(payloadIsNotValid(jsonMap)){
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().write("Invalid JSON structure: does not meet validation criteria");
                return;
            }

        } catch (IOException e) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("Invalid JSON structure: does not meet validation criteria");
            throw new RuntimeException(e);
        }
        String[] urlParts = urlPath.split("/");
        if (!isValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("the path is not a valid one");
        }else{
            LiftRide lifeRide = gson.fromJson(sb.toString(), LiftRide.class);
            res.setStatus(HttpServletResponse.SC_OK);
            int resortID = Integer.parseInt(urlParts[1]);
            String seasonID = urlParts[3];
            String dayID = urlParts[5];
            int skierID = Integer.parseInt(urlParts[7]);
            Response response = new Response(lifeRide,resortID,seasonID,dayID,skierID);
            String result = gson.toJson(response);
            try {
                sendToQueue(result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            res.getWriter().write("Successfully post the request");
            res.getWriter().write(result);
        }






    }

    private boolean payloadIsNotValid(Map<String, Object> jsonMap) {
        if(!jsonMap.containsKey("time") || !jsonMap.containsKey("liftID")){
            return true;
        }
        return false;
    }

    private String getSkierDayVertical(int resortID, String seasonID, String dayID, int skierID) {
        return "The vertical for skier " + skierID + " at resort " + resortID+ " in season " + seasonID
                + " at day" + dayID + " is " + "99999";
    }

    private boolean isParameterValid(String[] urlPath) {
        int resortID = Integer.parseInt(urlPath[1]);
        String seasonID = urlPath[3];
        String dayID = urlPath[5];
        int skierID = Integer.parseInt(urlPath[7]);
        if(resortID < 0){
            return false;
        }
        try{
            int day = Integer.parseInt(dayID);
            if(day <= 0 || day >= 366){
                return false;
            }
        }catch(NumberFormatException e){
            return false;

        }
        if(seasonID  == null){
            return false;
        }
        if(skierID < 0){
            return false;
        }
        return true;
    }

    private boolean isUrlValid(String[] urlPath) {
        // TODO: validate the request url path according to the API spec
        // /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
        // urlPath  = "/1/seasons/2019/day/1/skier/123"
        // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
        if(urlPath.length != 8){
            return false;
        }
        return "seasons".equals(urlPath[2])
                && "days".equals(urlPath[4])
                && "skiers".equals(urlPath[6]);
        //array of regular expression


    }
    private boolean isValid(String[] urlPath){
        return isUrlValid(urlPath) && isParameterValid(urlPath);
    }

    public void destroy() {
    }
}