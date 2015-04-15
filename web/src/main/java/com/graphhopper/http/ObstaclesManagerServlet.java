package com.graphhopper.http;

import it.esalab.mapaal.http.MapaalGraphHopper;
import it.esalab.mapaal.http.mapservices.SnappingService;
import it.esalab.mapaal.http.parsers.JsonObstacleParser;
import it.esalab.mapaal.http.repository.Obstacle;
import it.esalab.mapaal.http.repository.ObstaclesRepository;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.graphhopper.GraphHopper;
import com.graphhopper.util.StopWatch;

public class ObstaclesManagerServlet extends GHBaseServlet {

	private static final long serialVersionUID = -114035053392620919L;
	private ObstaclesRepository obscalesHandler = new ObstaclesRepository();
	
    @Inject
    protected GraphHopper hopper;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		boolean res = false;
		if ("GET".equalsIgnoreCase(req.getMethod())) {
			String mode = req.getPathInfo();		
			if(mode.equals("/get")){
				String id = req.getParameter("id");
				if(id!=null){
					Obstacle ob = obscalesHandler.getObstacle(Integer.parseInt(id));
					writeJson(req, resp, ob.toJson());
				}
				else{
					throw new RuntimeException("Missing id Parameter");
				}
			}
			else if(mode.equals("/delete")){
				String id = req.getParameter("id");
				if(id!=null){
					Obstacle ob = obscalesHandler.getObstacle(Integer.parseInt(id));
					ob.setCancellato(true);
					res = obscalesHandler.updateObstacle(ob);
					JSONObject json = new JSONObject();
					writeJson(req, resp, json);
				}
				else{
					throw new RuntimeException("Missing id Parameter");
				}
			}
			else if(mode.equals("/list")){
				//fa un po schifo, mancano dei controlli decenti
				String permanentST = req.getParameter("permanent");
				String dateSTraw = req.getParameter("date");
				String[] dateST = dateSTraw.split(",");
				
				Timestamp[] date = {null, null};
				String deletedST = req.getParameter("deleted");
				try{
				    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				    Date parsedDate = dateFormat.parse(dateST[0]);
				    date[0] = new java.sql.Timestamp(parsedDate.getTime());
				    parsedDate = dateFormat.parse(dateST[1]);
				    date[1] = new java.sql.Timestamp(parsedDate.getTime());
				}catch(Exception e){
					throw new RuntimeException("Error in parsing timestamp: "+ dateST[0]);
				}
				long[] bounding_box = {0,0,0,0};
				String bounding_boxSTraw = req.getParameter("bounding_box");
				if(bounding_boxSTraw==null){
					bounding_box = null;
				}
				else{
				String[] bounding_boxST = bounding_boxSTraw.split(",");
					bounding_box[0]= Long.parseLong(bounding_boxST[0]);
					bounding_box[1]= Long.parseLong(bounding_boxST[1]);
					bounding_box[2]= Long.parseLong(bounding_boxST[2]);
					bounding_box[3]= Long.parseLong(bounding_boxST[3]);
				}
				int disability_type= 0;
				String disability_typeST = req.getParameter("disability_type");
				if(disability_typeST!=null){
					disability_type = Integer.parseInt(disability_typeST);
				}
				
				boolean permanent= Boolean.parseBoolean(permanentST);
				boolean deleted= Boolean.parseBoolean(deletedST);
				
				
				ArrayList<Obstacle> obstacles = obscalesHandler.getObstacleList(permanent, date, bounding_box, disability_type,deleted);

				JSONArray ja = new JSONArray();
				for( Obstacle ob: obstacles){
					ja.put(ob.toJson());
				}
				
				JSONObject json = new JSONObject();
				json.put("length", ja.length());
				json.put("values", ja);
				
				writeJson(req, resp, json);
			}
			else{
				throw new RuntimeException("Unrecongnized path: "+ mode);
				
			}
			
		}
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		if ("POST".equalsIgnoreCase(req.getMethod())) {
			Scanner s = new Scanner(req.getInputStream(), "UTF-8")
					.useDelimiter("\\A");
			String body = s.hasNext() ? s.next() : "";
			if (body.equals("")) {
				throw new RuntimeException(
						"Empty obstacle data from post request");
			} else {
				JSONArray obstacleArray = new JSONArray(body);
				updateObstacle(req,resp,obstacleArray);
			}
		}
	}
	
	private void updateObstacle(HttpServletRequest req, HttpServletResponse resp, JSONArray obstacleArray) throws IOException{
		StopWatch sw = new StopWatch().start();
		boolean res = false;
		SnappingService snapper = new SnappingService(
				(MapaalGraphHopper) hopper);
		for (int i = 0; i < obstacleArray.length(); i++) {
			JSONObject jObstacle = obstacleArray.getJSONObject(0);
			JsonObstacleParser parser = new JsonObstacleParser();

			Obstacle obstacle = parser.ParseObstacle(jObstacle);
			double[] gpx = obstacle.getPoi().getCoordinates();
			long[] nodes = { -1, -1 };

			if (snapper.snapReportToSingleEdge(nodes, gpx)) {
				obstacle.getPoi().setCoordinates(gpx);
				obstacle.setNodes(nodes);
				logger.info("Correction done, new nodes: " + nodes[0]
						+ " - " + nodes[1] + " new coordinates: "
						+ +gpx[0] + " - " + gpx[1]);
			} else {
				logger.warn("Unable to snap to graph nodes and coordinates: "
						+ nodes[0]+ " - "+ nodes[1]+ " "+ gpx[0]+ " - "+ gpx[1]
						+ "the report will be saved but no correct re-routing is guarantee, try to re-send a report from a near area");
			}

			if (obstacle.getId() == 0) {
				res = obscalesHandler.addObstacle(obstacle);
			} else {
				res = obscalesHandler.updateObstacle(obstacle);
			}
			if (!res) {
				logger.error("Unable to save obstacle,  try to re-send a it");
			}
			
			float took = sw.stop().getSeconds();

			JSONObject jsonInfo = new JSONObject();
			jsonInfo.put("took", Math.round(took * 1000));
			JSONObject json = new JSONObject();
			json.put("info", jsonInfo);

			json.put("obstacleId", obstacle.getId());
			writeJson(req, resp, json);
			
		}
	}

}
