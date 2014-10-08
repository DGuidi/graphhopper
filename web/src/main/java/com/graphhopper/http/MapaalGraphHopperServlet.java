package com.graphhopper.http;


import it.esalab.mapaal.http.parsers.JsonReportParser;
import it.esalab.mapaal.http.repository.ForbiddenEdgesRepository;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
				JSONArray reportArray = new JSONArray(body);
				for (int i = 0; i < reportArray.length(); i++) {
					JSONObject jreport = reportArray.getJSONObject(0);
					Report report = new Report("Feature", "Point", "POI");
					JsonReportParser parser = new JsonReportParser(report);
					report = parser.ParseReport(jreport);
					snapReportToSingleEdge(report);
					boolean res = nodesHandler.receiveReport(report);
					if (!res) {
						// TODO: standardizzare l'errore di inserimento ma
						// continuare il ciclo e fornire comunque un routing
					}
				}
			}
		}
		doGet(req, resp);
	}

	private void snapReportToSingleEdge(Report report) throws IOException {
		long stantnode = report.getNodes()[0];
		long endnode = report.getNodes()[1];
		double[] gpx = report.getCoordinates();

		NodeList nl;
		List<String> matchingnodes = new ArrayList<String>();

		try {

			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder;
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(hopper.getOSMFile());
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			XPathExpression expr = xpath.compile("//way[nd/@ref=\"" + stantnode
					+ "\" or nd/@ref=\"" + endnode + "\"]");
			nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

			/*
			 * ricavo dall'osm gli elementi via che comprendono i nodi della
			 * segnalazione
			 */
			for (int j = 0; j < nl.getLength(); j++) {
				if (nl.item(j).getNodeType() == Node.ELEMENT_NODE) {
					Element via = (Element) nl.item(j);
					NodeList childNodes = via.getElementsByTagName("nd");
					for (int j1 = 0; j1 < childNodes.getLength(); j1++) {
						Element child = (Element) childNodes.item(j1);
						matchingnodes.add(child.getAttribute("ref"));
					}
				}
			}

			/*
			 * recupero dall osm le coordinate di tutti i nodi trovati
			 */
			String[][] matchingnodes_coords = new String[3][matchingnodes
					.size()];
			NodeList edgeNodes = doc.getElementsByTagName("node");
			for (int j = 0; j < edgeNodes.getLength(); j++) {
				Element node = (Element) edgeNodes.item(j);
				String id = node.getAttribute("id");
				if (matchingnodes.contains(id)) {
					int index = matchingnodes.indexOf(id);
					matchingnodes_coords[0][index] = id;
					matchingnodes_coords[1][index] = node.getAttribute("lat");
					matchingnodes_coords[2][index] = node.getAttribute("lon");
				}
			}

			/*
			 * ottengo la coppia di nodi più vicini alle coordinate della
			 * segnalazione
			 */
			double nearestMAXlat, nearestMINlat, nearestMAXlon, nearestMINlon;
			nearestMAXlat = nearestMAXlon = Double.MIN_VALUE;
			nearestMINlat = nearestMINlon = Double.MAX_VALUE;
			long nearestMINlatIndex, nearestMAXlatIndex, nearestMAXlonIndex, nearestMINlonIndex;
			nearestMINlatIndex = nearestMAXlatIndex = nearestMAXlonIndex = nearestMINlonIndex = 0;

			for (int j = 0; j < matchingnodes_coords[1].length; j++) {
				if (matchingnodes_coords[1][j] != null
						&& matchingnodes_coords[2][j] != null) {
					double actualLat = Double
							.parseDouble(matchingnodes_coords[1][j]);
					double actualLon = Double
							.parseDouble(matchingnodes_coords[2][j]);
					if (actualLat >= gpx[0] && actualLat <= nearestMINlat) {
						nearestMINlat = actualLat;
						nearestMINlatIndex = Long
								.parseLong(matchingnodes_coords[0][j]);
					}
					if (actualLat <= gpx[0] && actualLat >= nearestMAXlat) {
						nearestMAXlat = actualLat;
						nearestMAXlatIndex = Long
								.parseLong(matchingnodes_coords[0][j]);
					}
					if (actualLon >= gpx[1] && actualLon <= nearestMINlon) {
						nearestMINlon = actualLon;
						nearestMINlonIndex = Long
								.parseLong(matchingnodes_coords[0][j]);
					}
					if (actualLon <= gpx[1] && actualLon >= nearestMAXlon) {
						nearestMAXlon = actualLon;
						nearestMAXlonIndex = Long
								.parseLong(matchingnodes_coords[0][j]);
					}
				}
			}

			if ((nearestMAXlatIndex == nearestMAXlonIndex && nearestMINlatIndex == nearestMINlonIndex)||nearestMAXlatIndex == nearestMINlonIndex && nearestMINlatIndex == nearestMAXlonIndex){
				long[] normnodes = { nearestMINlatIndex, nearestMAXlatIndex };
				report.setNodes(normnodes);
				}
			else{
				System.out.println("nodi problematici");
				// TODO: errore da gestire, non ho ide;ntificato correttamente i
				// nodi più vicini continuo con la segnalazione che mi è arrivata
			}
		} catch (ParserConfigurationException e) {
			System.out.println("error while parsing osm document");
			e.printStackTrace();
		} catch (SAXException e) {
			System.out.println("error while parsing osm document");
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			System.out.println("error while nodes collection");
			e.printStackTrace();
		}
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
