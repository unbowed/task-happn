package com.semako.taskhappn;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

@RestController
public class WebService {

	@PostMapping("/count")
	public long count(@RequestParam("lat") double lat, @RequestParam("lon") double lon,
	                 @RequestParam("file") MultipartFile tsvFile ) throws IOException {
		// Everything is done from the stream, no intermediary space is wasted, the file could be very big :)
		return getWorld(tsvFile.getInputStream()).getCount(new World.Zone(lat,lon));
	}

	@PostMapping("/top")
	public List<World.Zone> top(@RequestParam("n") int n, @RequestParam("file") MultipartFile tsvFile ) throws IOException {
		// Everything is done from the stream, no intermediary space is wasted, the file could be very big :)
		return getWorld(tsvFile.getInputStream()).getTop(n);
	}

	public static World getWorld(InputStream is) throws IOException {
		final Reader r = new InputStreamReader(is, StandardCharsets.UTF_8);
		final BufferedReader br = new BufferedReader(r);
		br.readLine(); // Skip header
		final Stream<World.Point> pointStream = br.lines()
				.filter(s -> !s.trim().isEmpty())
				.map(s -> {
					try {
						final String[] part = s.split("\\s+");
						return new World.Point(Double.parseDouble(part[1]), Double.parseDouble(part[2]));
					} catch (Exception e) {
						throw new IllegalStateException("Cannot parse line : \"" + s + "\" error : " + e);
					}
				});
		return new World(pointStream);
	}

	@ExceptionHandler
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public String handleException(Exception e){
		return e.toString();
	}

}
