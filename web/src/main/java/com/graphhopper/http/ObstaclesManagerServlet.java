package com.graphhopper.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ObstaclesManagerServlet extends GHBaseServlet {
	
	private static final long serialVersionUID = -114035053392620919L;

	@Override
	   protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
	    {
		   super.doGet(req, resp);
	    }
	   
		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp){
			try {
				super.doPost(req, resp);
			} catch (ServletException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

}
