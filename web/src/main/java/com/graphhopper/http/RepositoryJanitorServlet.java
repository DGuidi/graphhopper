package com.graphhopper.http;

import it.esalab.mapaal.http.repository.EdgesRepository;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import org.json.JSONObject;
import com.graphhopper.util.StopWatch;

public class RepositoryJanitorServlet extends GHBaseServlet {
	private static final long serialVersionUID = 7673542886380987613L;
	private EdgesRepository nodesHandler = new EdgesRepository();

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		try {
			cleanRepository(req, res);
		} catch (IllegalArgumentException ex) {
			writeError(res, SC_BAD_REQUEST, ex.getMessage());
		} catch (Exception ex) {
			logger.error(
					"Error while executing request: " + req.getQueryString(),
					ex);
			writeError(res, SC_INTERNAL_SERVER_ERROR,
					"Problem occured:" + ex.getMessage());
		}
	}

	private void cleanRepository(HttpServletRequest req, HttpServletResponse res)
			throws Exception {
		StopWatch sw = new StopWatch().start();
		int affectedRows = nodesHandler.cleanEdgesDb();
		float took = sw.stop().getSeconds();

		JSONObject jsonInfo = new JSONObject();
		jsonInfo.put("took", Math.round(took * 1000));
		JSONObject json = new JSONObject();
		json.put("info", jsonInfo);

		json.put("cleanedRows", affectedRows);
		writeJson(req, res, json);
	}
}
