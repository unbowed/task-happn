package com.semako.taskhappn;

import com.fasterxml.jackson.annotation.JsonView;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class World {

	private static final int LAT_SIZE = 90;
	private static final int LON_SIZE = 180;

	static class Point {
		private final double lat;
		private final double lon;
		public Point(double lat, double lon) {
			this.lat = lat;
			this.lon = lon;
		}
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Point point = (Point) o;
			return (point.lat == lat) && (point.lon == lon);
		}
		@Override
		public int hashCode() {
			return Objects.hash(lat, lon);
		}
	}

	/**
	 * A Zone is defined as a point representing a lower lat and lon bound
	 */
	static class Zone {

		public interface ShowCoord {}

		private final Point topLeft;
		public Zone(double minLat, double minLon){
			topLeft = new Point(minLat, minLon);
		}
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Zone zone = (Zone) o;
			return topLeft.equals(zone.topLeft);
		}
		@Override
		public int hashCode() {
			return Objects.hash(topLeft);
		}

		@JsonView(ShowCoord.class)
		public double minLat() {
			return topLeft.lat;
		}
		@JsonView(ShowCoord.class)
		public double minLon() {
			return topLeft.lon;
		}
		@JsonView(ShowCoord.class)
		public double maxLat() {
			return topLeft.lat+0.5;
		}
		@JsonView(ShowCoord.class)
		public double maxLon() {
			return topLeft.lon+0.5;
		}

	}

	/**
	 * Example for .3 return 0, for .69 return .5
	 */
	private static double halfCeil(double coord){
		return (Math.floor(coord*2)) / 2.0;
	}

	/**
	 *  Example True for 0 or -90 or 5.5
	 *  False for 5.20
	 */
	private static boolean isBoundary(double coord){
		return Math.floor(coord*2) == (coord*2);
	}

	// A ConcurrentHashMap can be used as scalable frequency map (a form of histogram or multiset) by using LongAdder values and initializing via computeIfAbsent.
	// For example, to add a count to a ConcurrentHashMap<String,LongAdder> freqs, you can use freqs.computeIfAbsent(k -> new LongAdder()).increment();
	private final ConcurrentHashMap<Zone, LongAdder> pointCountPerZone = new ConcurrentHashMap<>();
	private final List<Zone> zonesSortedDesc;

	public World(Stream<Point> pointsStream){
		// Let count points in each zone
		pointsStream
				/*
					Going parallel...
					In case we have enough core it should be faster.
					If not thread safety in pointCountPerZone is a penalty and a simple HashMap would do the work.
					TBH I will have to test both sequential and parallel to know what is better.
				*/
				.parallel()
				.forEach(p -> {
					// Most point belong at least inside one zone  [~]
					pointCountPerZone.computeIfAbsent(
							new Zone(halfCeil(p.lat), halfCeil(p.lon)),
							k -> new LongAdder()
					).increment();

					// Some point are on the lat line ~|~ so they belong to two zones unless lat == (-/+)LAT_SIZE
					if(isBoundary(p.lat) && Math.abs(p.lat) != LAT_SIZE){
						// The second zone is the zone they are max lat to (by definition)
						pointCountPerZone.computeIfAbsent(
								new Zone(p.lat-0.5f, halfCeil(p.lon)),
								k -> new LongAdder()
						).increment();
					}

					// Same for the lon line
					if(isBoundary(p.lon) && Math.abs(p.lon) != LON_SIZE){
						// The second zone is the zone they are max lon to (by definition)
						pointCountPerZone.computeIfAbsent(
								new Zone(halfCeil(p.lat), p.lon),
								k -> new LongAdder()
						).increment();
					}
					// Now an edge case, if they are both
					if( isBoundary(p.lat) && Math.abs(p.lat) != LAT_SIZE  && isBoundary(p.lon) && Math.abs(p.lon) != LON_SIZE ){
						pointCountPerZone.computeIfAbsent(
								new Zone(p.lat, p.lon),
								k -> new LongAdder()
						).increment();
					}
					// Why do we have to handle so much case ?
					// Well point on crossroad -+- belong to up to four zones
					// Example (0,0) belongs to
					// [(-0.5;-0.5) , (0;0)]
					// [(0;-0.5) , (0.5;0)]
					// [(-0.5;0) , (0;0.5)]
					// [(0;0) , (0.5;0.5)]

					// Finally I have to note that Point are expected to occur only ONCE, if not I will count them again
				});
		// Now we just have to order them O(nlogn) worst case, but we can query it as much as we want without cost
		zonesSortedDesc = pointCountPerZone
				.entrySet()
				.stream()
				.sorted(Comparator.<Map.Entry<Zone, LongAdder>, Long>comparing(e -> e.getValue().sum()).reversed() )
				.map(e -> e.getKey())
				.collect(Collectors.toList());

	}

	public long getCount(Zone z){
		return pointCountPerZone.computeIfAbsent(z, k -> new LongAdder()).sum();
	}
	public List<Zone> getTop(int n){
		return zonesSortedDesc.subList(0,Math.min(zonesSortedDesc.size(),n));
	}

}
