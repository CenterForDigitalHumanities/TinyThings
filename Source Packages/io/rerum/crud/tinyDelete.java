/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.rerum.crud;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import io.rerum.tokens.TinyTokenManager;

/**
 *
 * @author bhaberbe
 */
public class tinyDelete extends HttpServlet {    
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, Exception {
        response.setHeader("Content-Type", "application/json; charset=utf-8");
        response.setCharacterEncoding("UTF-8");
        TinyTokenManager manager = new TinyTokenManager();
        manager.init();
        BufferedReader bodyReader = request.getReader();
        StringBuilder bodyString = new StringBuilder();
        String line;
        String requestString;
        StringBuilder sb = new StringBuilder();
        int codeOverwrite = 500;
        boolean moveOn = true;
        while ((line = bodyReader.readLine()) != null)
        {
          bodyString.append(line);
        }
        requestString = bodyString.toString();
        
        //RERUM will catch this
        /*
        if(!requestString.contains(Constant.RERUM_ID_PATTERN)){
            //IT IS NOT a rerum object, we can't delete this
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,"Your provided id must be a RERUM URL. Pattern \""+Constant.RERUM_ID_PATTERN+"\" was not found.");
            moveOn = false;
            // If we throw instead, the return isn't needed.
        }
        */
        //If it was JSON
        String pubTok = manager.getAccessToken();
        boolean expired = manager.checkTokenExpiry();
        if(expired){
            System.out.println("Tiny thing detected an expired token, auto getting and setting a new one...");
            pubTok = manager.generateNewAccessToken();
        }
        //Point to rerum server v1
        URL postUrl = new URL(Constant.RERUM_API_ADDR + "/delete.action");
        HttpURLConnection connection = (HttpURLConnection) postUrl.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("DELETE");
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Authorization", "Bearer "+pubTok);
        connection.connect();
        try{
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            //Pass in the user provided JSON for the body of the rerumserver v1 request
            out.writeBytes(requestString);
            out.flush();
            out.close(); 
            codeOverwrite = connection.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(),"utf-8"));
            while ((line = reader.readLine()) != null){
                //Gather rerum server v1 response
                sb.append(line);
            }
            reader.close();
        }
        catch(IOException ex){
            //Need to get the response RERUM sent back.
            BufferedReader error = new BufferedReader(new InputStreamReader(connection.getErrorStream(),"utf-8"));
            String errorLine = "";
            while ((errorLine = error.readLine()) != null){  
                sb.append(errorLine);
            } 
            error.close();
        }
        connection.disconnect();
        response.setStatus(codeOverwrite);
        response.getWriter().print(sb.toString());
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception ex) {
            Logger.getLogger(tinyDelete.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Handles the HTTP <code>DELETE</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception ex) {
            Logger.getLogger(tinyDelete.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Mark an object at a known `id` as deleted, removing it from the version history.";
    }// </editor-fold>

}
