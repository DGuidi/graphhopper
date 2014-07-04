package com.graphhopper.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
    boolean includeElevation = getBooleanParam(req, "elevation", true);
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
