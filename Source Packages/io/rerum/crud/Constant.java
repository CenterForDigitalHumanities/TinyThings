/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.rerum.crud;

/**
 *
 * @author bhaberbe
 */
public class Constant {
    //AWS   http://18.218.227.205:8080/rerum_server
    public static String RERUM_REGISTRATION_URL = "http://18.218.227.205:8080/rerum_server/";
    public static String RERUM_API_ADDR = "http://18.218.227.205:8080/rerum_server/api";
    //public static String RERUM_ID_PATTERN = "//devstore.rerum.io/v1/id";
    public static String RERUM_ACCESS_TOKEN_URL = "http://18.218.227.205:8080/rerum_server/api/accessToken.action";
    public static String RERUM_REFRESH_TOKEN_URL = "http://18.218.227.205:8080/rerum_server/api/refreshToken.action";
    
    //https://stackoverflow.com/questions/2395737/java-relative-path-of-a-file-in-a-java-web-application
    public static String PROPERTIES_FILE_NAME = "tiny.properties";
}
