$(function () {
  var host, createDefaultRequest,createReroutingRequest, decodePath, formatCoord, formatInstruction, url, jsondata;

  host = 'http://localhost:8989/';
  
  
  createurl = function(pointALat,pointALon,pointBLat,pointBLon){
	  return[
       host + 'route?',
       'point='+pointALat+','+pointALon,
       'point='+pointBLat+','+pointBLon,
       'vehicle=mapaal',
       'debug=true'
     ].join('&');
  };

  createJsonData = function(nodeA,nodeB,lat,lon){
  jsonData=[{
	    "nodes": [nodeA,nodeB],
	    "poi": {
	      "type": "Feature",
	      "geometry": {
	        "type": "Point",
	        "coordinates": [lat,lon]
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
  return JSON.stringify(jsonData);
  };

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


  createReroutingRequest = function (url,data) {
    return $.ajax({
      type: 'POST',
      dataType: 'json',
      timeout: 30000,
      url: url,
      processData: false,
      contentType: "application/json;",
      data: data
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
	
	asyncTest("Repository Janitor", function () {
		    expect(2);
		    var req = createDefaultRequest( host + 'repoJanitor')
					.done(function (json) {
					  var info, cleanedRows;
					  info = json.info;
					  cleanedRows= json.cleanedRows;
					  console.log(json);
					  ok(info, 'request completed in: ' + info.took);
					  ok(cleanedRows>=0, 'rows cleaned : ' + cleanedRows);
					});
		  });
	

  asyncTest("default routing - via mamiani", function () {
    expect(6);
    var url = createurl(43.612966,13.50526,43.611856,13.504794);
    var expetedLenght=4
    
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
			  ok(instructions && instructions.length === expetedLenght, 'instructions: ' + _(instructions).map(formatInstruction).join(','));
			});
  });

 
  asyncTest("re-routing request - via mamiani", function () {
    expect(6);
    var url = createurl(43.612966,13.50526,43.611856,13.504794);
    var data = createJsonData(474842489,1873976113,43.61264,13.504987)
 	  var expetedLenght=6
    var req = createReroutingRequest(url,data)
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
  
  asyncTest("default routing - via santo stefano", function () {
	    expect(6);
	    var url = createurl(43.6125,13.515142,43.613634,13.513473);
	    var expetedLenght=5
	    
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
				  ok(instructions && instructions.length ===  expetedLenght, 'instructions: ' + _(instructions).map(formatInstruction).join(','));
				});
	    });
  
  asyncTest("re-routing request - via santo stefano", function () {
	    expect(6);
	    var url = createurl(43.613634,13.513473,43.6125,13.515142);
	    var data = createJsonData(107139073,107139069,43.612263,13.514149)
	    var expetedLenght=7
	    
	    var req = createReroutingRequest(url,data)
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
	        ok(instructions && instructions.length === expetedLenght, 'instructions: ' + _(instructions).map(formatInstruction).join(','));
	      });
	  });
  
  asyncTest("re-routing request - via panoramica - worst case scenario -- needs work", function () {
	    expect(6);
	    var url = createurl(43.619592,13.527297,43.618881,13.523998);
	    var data = createJsonData(471237247,137545680,43.61918,13.5255)
	 	  var expetedLenght=4
	    var req = createReroutingRequest(url,data)
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
	        ok(instructions && instructions.length === expetedLenght, 'instructions: ' + _(instructions).map(formatInstruction).join(','));
	      });
	  });

});
