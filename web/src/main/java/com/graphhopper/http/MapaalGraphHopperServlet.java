package com.graphhopper.http;


import it.esalab.mapaal.http.MapaalGraphHopper;
import it.esalab.mapaal.http.mapservices.SnappingService;
import it.esalab.mapaal.http.parsers.JsonReportParser;
import it.esalab.mapaal.http.repository.ForbiddenEdgesRepository;
import it.esalab.mapaal.http.repository.Poi;
import it.esalab.mapaal.http.repository.Report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.graphhopper.GHResponse;
import com.graphhopper.util.Helper;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.BBox;

/**
 * NOTE: cannot move this class to mapaal package :(
 */
public class MapaalGraphHopperServlet extends GraphHopperServlet {

	private static final long serialVersionUID = 5682815885200619893L;
	private ForbiddenEdgesRepository nodesHandler = new ForbiddenEdgesRepository();
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		if ("POST".equalsIgnoreCase(req.getMethod())) {
			Scanner s = new Scanner(req.getInputStream(), "UTF-8")
					.useDelimiter("\\A");
			String body = s.hasNext() ? s.next() : "";
			if (body.equals("")) {
				throw new RuntimeException(
						"Empty report data from post request");
			} else {
				// SnappingService snapper = new SnappingService(hopper.getOSMFile()); //old version based on osm file, deprecated
				SnappingService snapper = new SnappingService((MapaalGraphHopper) hopper);

				JSONArray reportArray = new JSONArray(body);
				for (int i = 0; i < reportArray.length(); i++) {
					JSONObject jreport = reportArray.getJSONObject(0);
					Poi poi = new Poi("Feature", "Point", "POI");
					Report report = new Report(poi);
					JsonReportParser parser = new JsonReportParser(report);
					report = parser.ParseReport(jreport);
					long[] nodes = report.getNodes();
					double[] gpx = report.getPoi().getCoordinates();
					if (snapper.snapReportToSingleEdge(nodes, gpx)) {
						report.getPoi().setCoordinates(gpx);
						report.setNodes(nodes);
						logger.info("Correction done, new nodes: " + nodes[0]
								+ " - " + nodes[1] + " new coordinates: "
								+ +gpx[0] + " - " + gpx[1]);
					} else {
						logger.warn("Unable to snap to graph nodes and coordinates: "
								+ nodes[0]+ " - "+ nodes[1]+ " "
								+ gpx[0]+ " - "+ gpx[1]
								+ "the report will be saved but no correct rerouting is guarantee, try to re-send a report from a near area");
					}
					boolean res = nodesHandler.receiveReport(report);
					if (!res) {
						logger.error("Unable to save report,  try to re-send a it");
					}
				}
			}
		}
		doGet(req, resp);
	}
	
  
  @Override
  protected String routingType(HttpServletRequest req) {
    String param = getParam(req, "routingType", null);
    return param != null ? param : getParam(req, "vehicle", "CAR");
  }
  
  @Override
  protected String defaultLocale() {
    return "it";
  }
  
  @Override
  protected void writeJson(HttpServletRequest req, HttpServletResponse res, GHResponse rsp,
      float took) throws JSONException, IOException {
    JSONObject jsonInfo = new JSONObject();
    jsonInfo.put("took", Math.round(took * 1000));
    JSONObject json = new JSONObject();
    json.put("info", jsonInfo);
    if (rsp.hasErrors()) {
      errors(rsp, jsonInfo);
    } else if (!rsp.isFound()) {
      notfound(jsonInfo);
    } else {
      JSONObject jsonPath = def(req, rsp);
      json.put("paths", Collections.singletonList(jsonPath));
    }
    writeJson(req, res, json);
  }

  private void errors(GHResponse rsp, JSONObject jsonInfo) throws JSONException {
    List<Map<String, String>> list = new ArrayList<Map<String, String>>();
    for (Throwable t : rsp.getErrors()) {
      Map<String, String> map = new HashMap<String, String>();
      map.put("message", t.getMessage());
      map.put("details", t.getClass().getName());
      list.add(map);
    }
    jsonInfo.put("errors", list);
  }

  private void notfound(JSONObject jsonInfo) throws JSONException {
    Map<String, String> map = new HashMap<String, String>();
    map.put("message", "Not found");
    map.put("details", "");
    jsonInfo.put("errors", Collections.singletonList(map));
  }

  private JSONObject def(HttpServletRequest req, GHResponse rsp) throws JSONException {
    boolean enableInstructions = getBooleanParam(req, "instructions", true);
    boolean pointsEncoded = getBooleanParam(req, "points_encoded", false);
    boolean calcPoints = getBooleanParam(req, "calc_points", true);
    boolean includeElevation = getBooleanParam(req, "elevation", false);
    JSONObject jsonPath = new JSONObject();
    jsonPath.put("distance", Helper.round(rsp.getDistance(), 3));
    jsonPath.put("time", rsp.getMillis());
    if (calcPoints) {
      PointList points = rsp.getPoints();
      if (points.getSize() >= 2) {
        BBox bounds = hopper.getGraph().getBounds();
        jsonPath.put("bbox", rsp.calcRouteBBox(bounds).toGeoJson());
      }
      Object createPoints = createPoints(points, pointsEncoded, includeElevation);
      jsonPath.put("points", createPoints);
      if (enableInstructions) {
        InstructionList instructions = rsp.getInstructions();
        jsonPath.put("instructions", instructions.createJson());
      }
    }
    return jsonPath;
  }

}
