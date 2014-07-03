$(function () {
  var host = 'http://localhost:8989/',
	createRequest = function (url) {
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
	},
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
	},
	formatCoord = function (coord) {
	  return coord[0].toFixed(6) + ' ' + coord[1].toFixed(6);
	},
	formatInstruction = function (instr) {
	  var nodes = instr.nodes || [0, 0];
	  return [instr.text, ': [', nodes[0] || 'null', '-', nodes[1] || 'null', ']'].join('');
	};

  asyncTest("default routing", function () {
    expect(6);
    var url = [
			host + 'route?',
			'point=43.165858,13.724729',
			'point=43.167963,13.726033',
			'type=json',
			'vehicle=mapaal',
			'locale=it',
			'points_encoded=false'
    ].join('&'),
		req = createRequest(url)
			.done(function (json) {
			  var info = json.info,
					paths = json.paths,
					path;
			  console.log(json);
			  ok(info, 'request completed in: ' + info.took);
			  ok(paths && paths.length && paths.length === 1, 'paths valid');
			  path = paths[0];
			  ok(path, 'path found');
			  console.log(path);

			  ok(!path.points_encoded, 'points are not encoded');
			  var points = path.points.coordinates;
			  ok(points, 'data: ' + _(points).map(formatCoord).join(','));

			  var instructions = path.instructions;
			  ok(instructions && instructions.length === 6, 'instructions: ' + _(instructions).map(formatInstruction).join(','));
			});
  });
});
