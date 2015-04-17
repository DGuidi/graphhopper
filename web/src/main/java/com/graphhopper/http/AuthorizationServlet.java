package com.graphhopper.http;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.types.User;


public class AuthorizationServlet extends GHBaseServlet  {
//	NOTA: servlet incompleta inserita per motivi di test, da completare in futuro
	
	
	private static final long serialVersionUID = 7520429108107542745L;
	private String FACEBOOK_CLIENT_ID = "1429555897358459";
	private String FACEBOOK_SECURE_KEY = "0e7c38ef6760606a583d6f42ee01052d";
	 String redirectUri = "http://localhost:8989/auth";



	@SuppressWarnings("deprecation")
	@Override
	  protected void doGet(HttpServletRequest request, HttpServletResponse response)
	      throws IOException, ServletException {
		
		String accessToken = (String) request.getSession().getAttribute("accessToken");

        Integer expires = null;
//		if(accessToken==null){
		
		String code = request.getParameter("code");

		  if (code != null) {
			String red = "https://graph.facebook.com/oauth/access_token?client_id="
					+ FACEBOOK_CLIENT_ID
					+ "&redirect_uri="
					+ redirectUri
					+ "&client_secret=" + FACEBOOK_SECURE_KEY + "&code=" + code;
 
	            //System.out.println(red);
	             
	            URL url = new URL(red);
	             
	            try {
	                String result = readURL(url);
	                 
	                String[] pairs = result.split("&");
	                for (String pair : pairs) {
	                    String[] kv = pair.split("=");
	                    if (kv.length != 2) {
	                        throw new RuntimeException("Unexpected auth response");
	                    } else {
	                        if (kv[0].equals("access_token")) {
	                            accessToken = kv[1];
	                        }
	                        if (kv[0].equals("expires")) {
	                            expires = Integer.valueOf(kv[1]);
	                        }
	                    }
	                }                
	                 
	            } catch (IOException e) {
	                throw new RuntimeException(e);
	            }
	            request.getSession().setAttribute("accessToken", accessToken); 
	            FacebookClient facebookClient = new DefaultFacebookClient(accessToken);
	 
	           User user = facebookClient.fetchObject("me", User.class);
	           request.getSession().setAttribute("user", user);    
	           System.out.println(user.getLastName());

	             
	        } else {
	            String errorReason = request.getParameter("error_reason");
	             
	            if (errorReason != null)
	                request.setAttribute("messaggio", errorReason);
	            else
	                request.setAttribute("messaggio", "Code non presente");
	             
	            getServletConfig().getServletContext().getRequestDispatcher("/error").forward(request,response);
	        }
		  

	  }
	
	
	 private String readURL(URL url) throws IOException {
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        InputStream is = url.openStream();
	        int r;
	        while ((r = is.read()) != -1) {
	            baos.write(r);
	        }
	        return new String(baos.toByteArray());
	    }
	


	}

