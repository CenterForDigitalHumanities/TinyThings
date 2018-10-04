
package io.rerum.tokens;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.rerum.crud.Constant;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import net.sf.json.JSONObject;

/**
 * @author bhaberbe
 * This is the token manager for this application.  It handles all token interactions.
 */
public class TinyTokenManager{
    //Notice that when it is initialized, nothing is set.
    private String currentAccessToken = "";
    private String currentRefreshToken = "";
    private String propFileLocation = "tiny.properties";
    private String apiSetting = "";
    private Properties props = new Properties();
    
    /**
     * Initializer for a TinyTokenManager that reads in the properties File
     * @throws IOException if no properties file
     */
    public TinyTokenManager() throws IOException {
        init();
    }
    
    /**
     * After initializing, read in the properties you have and set the class values.
     * @return A Properties object containing the properties from the file.
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public final Properties init() throws FileNotFoundException, IOException{
        /*
            Your properties file must be in the deployed .war file in WEB-INF/classes/tokens.  It is there automatically
            if you have it in Source Packages/java/tokens when you build.  That is how this will read it in without defining a root location
            https://stackoverflow.com/questions/2395737/java-relative-path-of-a-file-in-a-java-web-application
        */
        String fileLoc =TinyTokenManager.class.getResource(Constant.PROPERTIES_FILE_NAME).toString();
        fileLoc = fileLoc.replace("file:", "");
        setFileLocation(fileLoc);
        InputStream input = new FileInputStream(propFileLocation);
        props.load(input);
        currentAccessToken = props.getProperty("access_token");
        currentRefreshToken = props.getProperty("refresh_token");
        apiSetting = props.getProperty("open_api_cors");
        return props;
    }
    
    /**
     * 
     * @param prop The property to write or overwrite
     * @param propValue The value of the property
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public void writeProperty (String prop, String propValue) throws FileNotFoundException, IOException{
        OutputStream output = null;
        output = new FileOutputStream(propFileLocation);
        // set the properties value
        props.setProperty(prop, propValue);
        // save properties to propFileLocation
        props.store(output, null);
    }
    
    /**
     * Check if the token being used is expired or not.  Expired access tokens can be replaced with a new one so long as your property file
     * has a valid refresh_token value stored.
     * @param token to check
     * @return Boolean true if expired or could not read token, false if token is not yet expired.
     */
    public boolean checkTokenExpiry(String token) {
        System.out.println(token);
        Date now = new Date();
        long nowTime = now.getTime();
        //Date expire;
        Date tokenEXPClaim;
        long expires;
        try {
            DecodedJWT recievedToken = JWT.decode(token);
            tokenEXPClaim = recievedToken.getExpiresAt();
            expires = tokenEXPClaim.getTime();
            return nowTime >= expires;
        } 
        catch (Exception exception){
            System.out.println("Problem with token, no way to check expiry");
            System.out.println(exception);
            return true;
        }
  
    }
    
    /**
     * Check if the token being used is expired or not.  Expired access tokens can be replaced with a new one so long as your property file
     * has a valid refresh_token value stored.
     * @return Boolean true if expired or could not read token, false if token is not yet expired.
     */
    public boolean checkTokenExpiry() {
        Date now = new Date();
        long nowTime = now.getTime();
        //Date expire;
        Date tokenEXPClaim;
        long expires;
        try {
            DecodedJWT recievedToken = JWT.decode(currentAccessToken);
            tokenEXPClaim = recievedToken.getExpiresAt();
            expires = tokenEXPClaim.getTime();
            return nowTime >= expires;
        } 
        catch (Exception exception){
            System.out.println("Problem with token, no way to check expiry");
            System.out.println(exception);
            return true;
        }
  
    }
    
    /**
     * Expired access tokens can be replaced with valid ones.
     * Note you must have read your properties in already so I know the currentRefreshToken.  
     * @see init()
     * @return A new valid access token.
     * @throws SocketTimeoutException
     * @throws IOException
     * @throws Exception 
     */
    public String generateNewAccessToken() throws SocketTimeoutException, IOException, Exception{
        System.out.println("Tiny Things has to get a new access token...");
        String newAccessToken = "";
        JSONObject jsonReturn = new JSONObject();
        JSONObject tokenRequestParams = new JSONObject();
        tokenRequestParams.element("refresh_token", currentRefreshToken);
        if(currentRefreshToken.equals("")){
            //You must read in the properties first!
            System.out.println("You must read in the properties first with init()");
            Exception noProps = new Exception("You must read in the properties first with init().  There was no refresh token set.");
            throw noProps;
        }
        else{
            try{
                System.out.println("Connecting to RERUM with refresh token...");
                URL rerum = new URL(Constant.RERUM_ACCESS_TOKEN_URL);
                HttpURLConnection connection = (HttpURLConnection) rerum.openConnection();
                connection.setRequestMethod("POST"); 
                connection.setConnectTimeout(5*1000); 
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.connect();
                DataOutputStream outStream = new DataOutputStream(connection.getOutputStream());
                //Pass in the user provided JSON for the body 
                outStream.writeBytes(tokenRequestParams.toString());
                outStream.flush();
                outStream.close(); 
                //Execute rerum server v1 request
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(),"utf-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null){
                    //Gather rerum server v1 response
                    sb.append(line);
                }
                reader.close();
                jsonReturn = JSONObject.fromObject(sb.toString());
                System.out.println("RERUM responded with access token...");
                newAccessToken = jsonReturn.getString("access_token");
            }
            catch(java.net.SocketTimeoutException e){ //This specifically catches the timeout
                System.out.println("The RERUM token endpoint is taking too long...");
                jsonReturn = new JSONObject(); //We were never going to get a response, so return an empty object.
                jsonReturn.element("error", "The RERUM endpoint took too long");
                throw e;
                //newAccessToken = "error";
            }
        }
        setAccessToken(newAccessToken);
        writeProperty("access_token", newAccessToken);
        System.out.println("Tiny Things has a new access token, and it is written to the properties file...");
        return newAccessToken;
    }
    
    /**
     * Expired access tokens can be replaced with valid ones.
     * If you have not read your properties file in, you can use this to pass the refresh_token directly.  
     * @param refresh_token The refresh token to use to get a new access token
     * @return A new valid access token.
     * @throws SocketTimeoutException
     * @throws IOException
     * @throws Exception 
     */
    public String generateNewAccessToken(String refresh_token) throws SocketTimeoutException, IOException, Exception{
        String newAccessToken = "";
        JSONObject jsonReturn = new JSONObject();
        JSONObject tokenRequestParams = new JSONObject();
        tokenRequestParams.element("refresh_token", refresh_token);
        if(currentRefreshToken.equals("")){
            //You must read in the properties first!
            Exception noProps = new Exception("You must read in the properties first with init().  There was no refresh token set.");
            throw noProps;
        }
        else{
            try{
                URL rerum = new URL(Constant.RERUM_ACCESS_TOKEN_URL);
                HttpURLConnection connection = (HttpURLConnection) rerum.openConnection();
                connection.setRequestMethod("POST"); 
                connection.setConnectTimeout(5*1000); 
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.connect();
                DataOutputStream outStream = new DataOutputStream(connection.getOutputStream());
                //Pass in the user provided JSON for the body 
                outStream.writeBytes(tokenRequestParams.toString());
                outStream.flush();
                outStream.close(); 
                //Execute rerum server v1 request
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(),"utf-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null){
                    //Gather rerum server v1 response
                    sb.append(line);
                }
                reader.close();
                jsonReturn = JSONObject.fromObject(sb.toString());
                newAccessToken = jsonReturn.getString("access_token");
            }
            catch(java.net.SocketTimeoutException e){ //This specifically catches the timeout
                System.out.println("The Auth0 token endpoint is taking too long...");
                jsonReturn = new JSONObject(); //We were never going to get a response, so return an empty object.
                jsonReturn.element("error", "The Auth0 endpoint took too long");
                throw e;
            }
        }
        setAccessToken(newAccessToken);
        writeProperty("access_token", newAccessToken);
        return newAccessToken;
    }
    
    public void setAPISetting(String setting){
        //ensure invalid strings result to false
        if(!setting.equals("true")){
            setting = "false";
        }
        apiSetting = setting;
    }
    
    public void setFileLocation(String location){
        propFileLocation = location;
    }
    
    public void setAccessToken(String newToken){
        currentAccessToken = newToken;
    }
    
    public void setRefreshToken(String newToken){
        currentRefreshToken = newToken;
    }
    
    public String getAPISetting(){
        //ensure invalid strings result to false
        if(!apiSetting.equals("true")){
            apiSetting = "false";
        }
        return apiSetting;
    }
    
    public String getAccessToken(){
        return currentAccessToken;
    }
    
    public String getRefreshToken(){
        return currentRefreshToken;
    }
    
    public String getFileLocation(){
        return propFileLocation;
    }
    
}
