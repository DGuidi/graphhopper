$(function () {
  var host, createDefaultRequest,createReroutingRequest, decodePath, formatCoord, formatInstruction, url, jsondata;

  host = 'http://localhost:8989/';
  
  url = [
         host + 'route?',
         'point=43.612947,13.505276',
         'point=43.611887,13.50474',
         'vehicle=mapaal',
         'debug=true'
       ].join('&');
  
  jsonData=[{
	    "nodes": [127848207,1873976113],
	    "poi": {
	      "type": "Feature",
	      "geometry": {
	        "type": "Point",
	        "coordinates": [43.6125232,13.5048275]
	      },
	      "properties": {
	        "typename": "POI"
	      }
	    },
	    "details": {
	      "sensor": {
	        "type": "test automatico",
	        "info": "segnalazione da test automatico",
	        "details": "segnalazione da test automatico, more data"
	      }
	    }
	  }];

	createDefaultRequest = function (url) {
	  return $.ajax({
	    type: 'GET',
	    dataType: 'json',
	    timeout: 30000,
	    url: url
	  })
		.fail(function (x, text) {
		  ok(false, 'ajax request failed: ' + text);
		})
		.always(function () {
		  start();
		});
	};


  createReroutingRequest = function (url) {
    return $.ajax({
      type: 'POST',
      dataType: 'json',
      timeout: 30000,
      url: url,
      processData: false,
      contentType: "application/json;",
      data: JSON.stringify(jsonData)
    })
    .fail(function (x, text) {
      ok(false, 'ajax request failed: ' + text);
    })
    .always(function () {
      start();
    });
  };

	decodePath = function (path) {
	  var encoded = path.points,
			len = encoded.length,
			index = 0,
			arr = [],
			lat = 0,
			lng = 0;
	  while (index < len) {
	    var b,
			shift = 0,
			result = 0;
	    do {
	      b = encoded.charCodeAt(index++) - 63;
	      result |= (b & 0x1f) << shift;
	      shift += 5;
	    } while (b >= 0x20);
	    var deltaLat = ((result & 1) ? ~(result >> 1) : (result >> 1));
	    lat += deltaLat;

	    shift = 0;
	    result = 0;
	    do {
	      b = encoded.charCodeAt(index++) - 63;
	      result |= (b & 0x1f) << shift;
	      shift += 5;
	    } while (b >= 0x20);
	    var deltaLon = ((result & 1) ? ~(result >> 1) : (result >> 1));
	    lng += deltaLon;
	    arr.push([lng * 1e-5, lat * 1e-5]);
	  }
	  return arr;
	};

	formatCoord = function (coord) {
	  return coord[0].toFixed(6) + ' ' + coord[1].toFixed(6);
	};

	formatInstruction = function (instr) {
	  var nodes = instr.nodes || [0, 0];
	  return [instr.text, ': [', nodes[0] || 'null', '-', nodes[1] || 'null', ']'].join('');
	};

  asyncTest("default routing", function () {
    expect(6);
    var req = createDefaultRequest(url)
			.done(function (json) {
			  var info, paths, path;

        info = json.info;
				paths = json.paths;
				console.log(json);
			  ok(info, 'request completed in: ' + info.took);
			  ok(paths && paths.length && paths.length === 1, 'paths valid');
			  path = paths[0];
			  ok(path, 'path found');

			  ok(!path.points_encoded, 'points are not encoded');
			  var points = path.points.coordinates;
			  ok(points, 'data: ' + _(points).map(formatCoord).join(','));

			  var instructions = path.instructions;
			  ok(instructions && instructions.length === 4, 'instructions: ' + _(instructions).map(formatInstruction).join(','));
			});
  });

  asyncTest("re-routing request", function () {
    expect(6);
    var req = createReroutingRequest(url)
      .done(function (json) {
        var info, paths, path;

        info = json.info;
        paths = json.paths;
        console.log(json);
        ok(info, 'request completed in: ' + info.took);
        ok(paths && paths.length && paths.length === 1, 'paths valid');
        path = paths[0];
        ok(path, 'path found');

        ok(!path.points_encoded, 'points are not encoded');
        var points = path.points.coordinates;
        ok(points, 'data: ' + _(points).map(formatCoord).join(','));

        var instructions = path.instructions;
        ok(instructions && instructions.length === 6, 'instructions: ' + _(instructions).map(formatInstruction).join(','));
      });
  });

});
