$(function () {
  var host, createGetRequest,createPostRequest, decodePath, formatCoord, formatInstruction, url, jsondata, createObstaclesJsonData, createPoiJsonData, createJsonData, obstacleId;

  host = '/';
 
  createurl = function(pointALat,pointALon,pointBLat,pointBLon){
	  return[
       host + 'route?',
       'point='+pointALat+','+pointALon,
       'point='+pointBLat+','+pointBLon,
       'vehicle=mapaal',
       'debug=true'
     ].join('&');
  };

  createPoiJsonData = function(lat, lon){
	  var poi = {
				"type" : "Feature",
				"geometry" : {
					"type" : "Point",
					"coordinates" : [ lat, lon ]
				},
				"properties" : {
					"typename" : "POI"
				}
			};
	  return poi;
  }
  
  createObstaclesJsonData = function(lat, lon) {
		jsonData = [ {
			"poi" : createPoiJsonData(lat, lon),
			"info" : "obstacle info",
			"notes" : "more info about obstacle",
			"warning" : false,
			"disability_type": 1,
			"permanent":false
		} ];
		return JSON.stringify(jsonData);
	};
  
  	createJsonData = function(nodeA, nodeB, lat, lon, duration) {
		jsonData = [ {
			"nodes" : [ nodeA, nodeB ],
			"poi" : createPoiJsonData(lat, lon),
			"details" : {
				"sensor" : {
					"type" : "test automatico",
					"info" : "segnalazione da test automatico",
					"details" : "segnalazione da test automatico, more data"
				}
			}
		} ];
		if (duration != null) {
			jsonData[0].duration = duration
		}
		return JSON.stringify(jsonData);
	};

	createGetRequest = function (url) {
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


  createPostRequest = function (url,data) {
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

//	decodePath = function (path) {
//	  var encoded = path.points,
//			len = encoded.length,
//			index = 0,
//			arr = [],
//			lat = 0,
//			lng = 0;
//	  while (index < len) {
//	    var b,
//			shift = 0,
//			result = 0;
//	    do {
//	      b = encoded.charCodeAt(index++) - 63;
//	      result |= (b & 0x1f) << shift;
//	      shift += 5;
//	    } while (b >= 0x20);
//	    var deltaLat = ((result & 1) ? ~(result >> 1) : (result >> 1));
//	    lat += deltaLat;
//
//	    shift = 0;
//	    result = 0;
//	    do {
//	      b = encoded.charCodeAt(index++) - 63;
//	      result |= (b & 0x1f) << shift;
//	      shift += 5;
//	    } while (b >= 0x20);
//	    var deltaLon = ((result & 1) ? ~(result >> 1) : (result >> 1));
//	    lng += deltaLon;
//	    arr.push([lng * 1e-5, lat * 1e-5]);
//	  }
//	  return arr;
//	};

	formatCoord = function (coord) {
	  return coord[0].toFixed(6) + ' ' + coord[1].toFixed(6);
	};

	formatInstruction = function(instr) {
		var nodes = instr.nodes || [ 0, 0 ];
		return [ instr.text, ': [', nodes[0] || 'null', '-',
				nodes[1] || 'null', ']' ].join('');
	};
	
	
	
	/* ADMIN TESTS-------------------------------------------------------------------------------------------------- */
	
	asyncTest("ADMIN - Insert and update Obstacle", function() {
		expect(2);
		var url =  host + 'obstacles/update';
		var data = createObstaclesJsonData(43.619064,13.525956);		
		var req =  createPostRequest(url, data).done(
				function(json) {
					var info, cleanedRows;
					info = json.info;
					obstacleId = json.obstacleId;
					console.log(json);
					ok(info, 'obstacle inserted with id: '+ obstacleId);
					ok(info.took, 'request completed in: ' + info.took+' ms');
				});
	});	
	
	asyncTest("ADMIN - Get Obstacle", function() {
		expect(2);
		if(!obstacleId){
			//NOTA: se i test non vengono eseguiti in fila faccio la select di un nodo noto
			//se va in errore modificare l'id con l'ultimo generato dal test precedente
			obstacleId = 1340;
			}
		var url =  host + 'obstacles/get?id='+obstacleId;
		var req =  createGetRequest(url).done(
				function(json) {	
					var info, cleanedRows;
					obstacleId = json.id;
					coordinates = json.poi.geometry.coordinates;
					debugger;
					console.log(json);
					ok(obstacleId, 'obstacle inserted with id: '+ obstacleId);
					ok(coordinates , 'obstacle coordinatese: ' + coordinates);
				});
		
	});	
	
//	asyncTest("ADMIN - Get List", function() {
//		expect(1);
//		var url =  host + 'obstacles/list?permanent=false&date=2005-10-1,2015-12-30';
//		var req =  createGetRequest(url).done(
//				function(json) {	
//					var info, cleanedRows;
//					length = json.length;
//					debugger;
//					console.log(json);
//					ok(length , 'selected: '+ length +" obstacles");
//				});
//		
//	});	
	
	
	asyncTest("MANTAINANCE - Repository Janitor", function() {
		expect(2);
		var req = createGetRequest(host + 'repoJanitor').done(
			function(json) {
				var info, cleanedRows;
				info = json.info;
				cleanedRows = json.cleanedRows;
				console.log(json);
				ok(info, 'request completed in: ' + info.took+' ms');
				ok(cleanedRows >= 0, 'rows cleaned : ' + cleanedRows);
			});
	});

	/* ROUTING TESTS-------------------------------------------------------------------------------------------------- */
	
	asyncTest("default routing - via mamiani", function() {
		expect(6);
		var url = createurl(43.612966, 13.50526, 43.611856, 13.504794);
		var expetedLenght = 4
		var req = createGetRequest(url).done(
			function(json) {
				var info, paths, path;
				info = json.info;
				paths = json.paths;
				ok(info, 'request completed in: ' + info.took+' ms');
				ok(paths && paths.length && paths.length === 1,'paths valid');
				path = paths[0];
				ok(path, 'path found');
				ok(!path.points_encoded, 'points are not encoded');
				var points = path.points.coordinates;
				ok(points, 'data: '+ _(points).map(formatCoord).join(','));
				var instructions = path.instructions;
				ok(instructions&& instructions.length === expetedLenght,'instructions: '+ _(instructions).map(formatInstruction).join(','));
			});
	});

	
	asyncTest("re-routing request - via mamiani", function() {
		expect(6);
		var url = createurl(43.612966, 13.50526, 43.611856, 13.504794);
		var data = createJsonData(1873976112, 1873976113, 43.61264, 13.504987)
		var expetedLenght = 6
		var req = createPostRequest(url, data).done(
			function(json) {
				var info, paths, path;
				info = json.info;
				paths = json.paths;
				ok(info, 'request completed in: ' + info.took+' ms');
				ok(paths && paths.length && paths.length === 1,'paths valid');
				path = paths[0];
				ok(path, 'path found');
				ok(!path.points_encoded, 'points are not encoded');
				var points = path.points.coordinates;
				ok(points, 'data: '+ _(points).map(formatCoord).join(','));
				var instructions = path.instructions;
				ok(instructions && instructions.length === 6,'instructions: '+ _(instructions).map(formatInstruction).join(','));
			});
	});
  

	asyncTest("default routing - via santo stefano", function() {
		expect(6);
		var url = createurl(43.6125, 13.515142, 43.613634, 13.513473);
		var expetedLenght = 5
		var req = createGetRequest(url).done(
			function(json) {
				var info, paths, path;
				info = json.info;
				paths = json.paths;
				ok(info, 'request completed in: ' + info.took+' ms');
				ok(paths && paths.length && paths.length === 1,'paths valid');
				path = paths[0];
				ok(path, 'path found');
				ok(!path.points_encoded, 'points are not encoded');
				var points = path.points.coordinates;
				ok(points, 'data: '+ _(points).map(formatCoord).join(','));
				var instructions = path.instructions;
				ok(instructions&& instructions.length === expetedLenght,'instructions: '+ _(instructions).map(formatInstruction).join(','));
			});
	});
	
	asyncTest("re-routing request - via santo stefano - test scadenza",function() {
		expect(6);
		var url = createurl(43.613634, 13.513473, 43.6125, 13.515142);
		var data = createJsonData(470083847, 470093743, 43.612263,13.514149, -10)
		var expetedLenght = 5
		var req = createPostRequest(url, data).done(
			function(json) {
				var info, paths, path;
				info = json.info;
				paths = json.paths;
				ok(info, 'request completed in: ' + info.took+' ms');
				ok(paths && paths.length && paths.length === 1,'paths valid');
				path = paths[0];
				ok(path, 'path found');
				ok(!path.points_encoded, 'points are not encoded');
				var points = path.points.coordinates;
				ok(points, 'data: '+ _(points).map(formatCoord).join(','));
				var instructions = path.instructions;
				ok(instructions&& instructions.length === expetedLenght,'instructions: '+ _(instructions).map(formatInstruction).join(','));
			});
	});
	
  asyncTest("re-routing request - via santo stefano", function() {
		expect(6);
		var url = createurl(43.613634, 13.513473, 43.6125, 13.515142);
		var data = createJsonData(470083847, 470093743, 43.612263, 13.514149)
		var expetedLenght = 7
		var req = createPostRequest(url, data).done(
			function(json) {
				var info, paths, path;
				info = json.info;
				paths = json.paths;
				ok(info, 'request completed in: ' + info.took+' ms');
				ok(paths && paths.length && paths.length === 1,'paths valid');
				path = paths[0];
				ok(path, 'path found');
				ok(!path.points_encoded, 'points are not encoded');
				var points = path.points.coordinates;
				ok(points, 'data: '+ _(points).map(formatCoord).join(','));
				var instructions = path.instructions;
				ok(instructions&& instructions.length === expetedLenght,'instructions: '+ _(instructions).map(formatInstruction).join(','));
			});
	});
  

	asyncTest("re-routing request - via panoramica - worst case scenario", function() {
		expect(6);
		var url = createurl(43.619592, 13.527297, 43.618881, 13.523998);
		var data = createJsonData(471237247, 137545680, 43.619064,13.525956)
		var expetedLenght = 2
		var req = createPostRequest(url, data).done(
			function(json) {
				var info, paths, path;
				info = json.info;
				paths = json.paths;
				ok(info, 'request completed in: ' + info.took+' ms');
				ok(paths && paths.length && paths.length === 1,'paths valid');
				path = paths[0];
				ok(path, 'path found');
				ok(!path.points_encoded, 'points are not encoded');
				var points = path.points.coordinates;
				ok(points, 'data: '+ _(points).map(formatCoord).join(','));
				var instructions = path.instructions;
				ok(instructions&& instructions.length === expetedLenght,'instructions: '+ _(instructions).map(formatInstruction).join(','));
			});
	});
	

	
});

