/* Copyright (c) 2001 - 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geoserver.ows.kvp.FormatOptionsKvpParser;
import org.geoserver.security.impl.GeoServerUser;
import org.geotools.data.ows.HTTPClient;
import org.geotools.data.ows.HTTPResponse;
import org.geotools.data.ows.SimpleHttpClient;
import org.springframework.util.StringUtils;

/**
 * AuthenticationMapper using an external REST webservice to get username for a given authkey.
 * The web service URL can be configured using a template in the form:
 * 
 *  http://<server>:<port>/<webservice>?<key>={key}
 *  
 *  where {key} will be replaced by the received authkey.
 *  
 *  The webservice have to return 
 */
public class WebServiceAuthenticationKeyMapper extends AbstractAuthenticationKeyMapper {

    FormatOptionsKvpParser parser = new FormatOptionsKvpParser();

    
    private String webServiceUrl;
    private String searchUser = "^\\s*(.*)\\s*$";
    Pattern searchUserRegex = null;
    
    int connectTimeout = 5;
    int readTimeout = 10;
    
    private HTTPClient httpClient = null;
    
    
    
    public WebServiceAuthenticationKeyMapper() {
        super();
        
    }

    private HTTPClient getHttpClient() {
        if(httpClient == null) {
            httpClient = new SimpleHttpClient();
        }
        return httpClient;
    }
   
    public String getWebServiceUrl() {
        return webServiceUrl;
    }

    public void setWebServiceUrl(String webServiceUrl) {
        this.webServiceUrl = webServiceUrl;
    }
    
    public String getSearchUser() {
        return searchUser;
    }

    public void setSearchUser(String searchUser) {
        this.searchUser = searchUser;
        searchUserRegex = Pattern.compile(searchUser);
    }
    
    public void setHttpClient(HTTPClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    protected void checkProperties() throws IOException {
        super.checkProperties();
        if (StringUtils.hasLength(getWebServiceUrl())==false) {
            throw new IOException ("Web service url is unset");
        }

    }


    public boolean supportsReadOnlyUserGroupService() {
        return false;
    }

    @Override
    public GeoServerUser getUser(String key) throws IOException {
        checkProperties();
        String username = callWebService(key);
        if(username == null) {
            return null;
        }
        
        return (GeoServerUser) getUserGroupService().loadUserByUsername(username);
    }




    private String callWebService(String key) {
        String url = webServiceUrl.replace("{key}", key);
        HTTPClient client = getHttpClient();
        
        client.setConnectTimeout(connectTimeout);
        client.setReadTimeout(readTimeout);
        try {
            HTTPResponse response = client.get(new URL(url));
            BufferedReader reader = null;
            InputStream responseStream = response.getResponseStream();
            StringBuilder result = new StringBuilder();
            try {
                reader = new BufferedReader(new InputStreamReader(responseStream));
                String line = null;
                while((line = reader.readLine()) != null) {
                    result.append(line);
                }
                if(searchUserRegex == null) {
                    return result.toString();
                } else {
                    Matcher matcher = searchUserRegex.matcher(result);
                    if(matcher.find())  {
                        return matcher.group(1);
                    }
                }
            } finally {
                reader.close();
            }
        } catch (MalformedURLException e) {
            LOGGER.log(Level.SEVERE, "Error in WebServiceAuthenticationKeyMapper, web service url is invalid: " + url, e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error in WebServiceAuthenticationKeyMapper, error in web service communication", e);
        } finally {
            
        }
        return null;
    }

    public void setParameters(String parameters) {
        if(parameters != null && ! "".equals(parameters)) {
            try {
                Map mapperParams = (Map)parser.parse(parameters);
                if(mapperParams != null) {
                    if(mapperParams.containsKey("webServiceUrl")) {
                        webServiceUrl = (String)mapperParams.get("webServiceUrl");
                    }
                    if(mapperParams.containsKey("searchUser")) {
                        searchUser = (String)mapperParams.get("searchUser");
                    }
                    if(mapperParams.containsKey("connectTimeout")) {
                        try {
                            connectTimeout = Integer.parseInt((String)mapperParams.get("connectTimeout"));
                        } catch(NumberFormatException e) {
                            LOGGER.log(Level.SEVERE, "WebServiceAuthenticationKeyMapper connectTimeout wrong format", e);
                        }
                    }
                    if(mapperParams.containsKey("readTimeout")) {
                        try {
                            readTimeout = Integer.parseInt((String)mapperParams.get("readTimeout"));
                        } catch(NumberFormatException e) {
                            LOGGER.log(Level.SEVERE, "WebServiceAuthenticationKeyMapper readTimeout wrong format", e);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "WebServiceAuthenticationKeyMapper parameters parsing error", e);
            }
        }
    }
    
    @Override
    synchronized public int synchronize() throws IOException {
        checkProperties();
        return 0;
    }
}
