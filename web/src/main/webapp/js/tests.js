$(function () {
	var host = 'http://localhost:8989/',
	createRequest = function (url) {
		return $.ajax({
			type : 'GET',
			dataType : 'json',
			timeout : 30000,
			url : url
		})
		.fail(function (x, text, thrown) {
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
		array = [],
		lat = 0,
		lng = 0,
		ele = 0;
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

			if (path.points_dimension === 3) {
				// elevation
				shift = 0;
				result = 0;
				do {
					b = encoded.charCodeAt(index++) - 63;
					result |= (b & 0x1f) << shift;
					shift += 5;
				} while (b >= 0x20);
				var deltaEle = ((result & 1) ? ~(result >> 1) : (result >> 1));
				ele += deltaEle;
				array.push([lng * 1e-5, lat * 1e-5, ele / 100]);
			} else {
				array.push([lng * 1e-5, lat * 1e-5]);
			}
		}
		return array;
	};

	asyncTest("routing without points encoding", function () {
		expect(4);
		var url = [
			host + 'route?',
			'point=43.164832,13.72317',
			'point=43.170209,13.735056',
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
				ok(info, 'request completed in: ' + info.took);
				ok(paths && paths.length && paths.length === 1, 'paths valid');
				path = paths[0];
				ok(path, 'path found');
				console.log(path);

				ok(!path.points_encoded, 'points are not encoded');
			});
	});

	asyncTest("routing with points encoding", function () {
		expect(4);
		var url = [
			host + 'route?',
			'point=43.164832,13.72317',
			'point=43.170209,13.735056',
			'type=json',
			'vehicle=mapaal',
			'locale=it'
		].join('&'),
		req = createRequest(url)
			.done(function (json) {
				var info = json.info,
				paths = json.paths,
				path;
				ok(info, 'request completed in: ' + info.took);
				ok(paths && paths.length && paths.length === 1, 'paths valid');
				path = paths[0];
				ok(path, 'path found');
				console.log(path);

				ok(path.points_encoded, 'points are encoded');
				var points = decodePath(path);
				console.log(points);
			});
	});
});
