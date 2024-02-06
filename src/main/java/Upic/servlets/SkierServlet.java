package Upic.servlets; /**
 * @author zoetian
 * @create 1/24/24
 */


import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Map;


public class SkierServlet extends HttpServlet {
    private String message;


    public void init() {
        message = "Hello World!";
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
        try{
            StringBuilder sb = new StringBuilder();
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
            res.setStatus(HttpServletResponse.SC_OK);
            int resortID = Integer.parseInt(urlParts[1]);
            String seasonID = urlParts[3];
            String dayID = urlParts[5];
            int skierID = Integer.parseInt(urlParts[7]);
            String result = "successfully write a new lift ride for the skier " + skierID;
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