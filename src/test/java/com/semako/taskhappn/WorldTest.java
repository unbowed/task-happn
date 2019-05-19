package com.semako.taskhappn;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
public class WorldTest {

	@Test
	public void basicUsage(){
		World.Point[] points = new World.Point[]{
				new World.Point(-48.6, -37.7),
				new World.Point(-27.1, 8.4),
				new World.Point(6.6, -6.9),
				new World.Point(-2.3, 38.3),
				new World.Point(6.8, -6.9),
				new World.Point(-2.5, 38.3),
				new World.Point(0.1, -0.1),
				new World.Point(-2.1, 38.1)
		};

		World.Zone[] top2Zone = new World.Zone[]{
				new World.Zone(-2.5,38),
				new World.Zone(6.5,-7)
		};

		World w = new World(Arrays.stream(points));
		Assert.assertEquals("It can count POI per zone",2, w.getCount(new World.Zone(6.5,-7)));
		Assert.assertArrayEquals("It can show top zone",top2Zone, w.getTop(2).toArray());
	}

	final String tsv = "@id @lat @lon\n" +
			"id1 -48.6 -37.7\n" +
			"\n" +
			"id2 -27.1 8.4\n" +
			"\n" +
			"id3 6.6 -6.9\n" +
			"\n" +
			"id4 -2.3 38.3\n" +
			"\n" +
			"id5 6.8 -6.9\n" +
			"\n" +
			"id6 -2.5 38.3\n" +
			"\n" +
			"id7 0.1 -0.1\n" +
			"\n" +
			"id8 -2.1 38.1";

	@Test
	public void TsvUsage() throws IOException {

		World.Zone[] top2Zone = new World.Zone[]{
				new World.Zone(-2.5,38),
				new World.Zone(6.5,-7)
		};

		World w = WebService.getWorld(new ByteArrayInputStream(tsv.getBytes(StandardCharsets.UTF_8)));
		Assert.assertEquals("It can count POI per zone",2, w.getCount(new World.Zone(6.5,-7)));
		Assert.assertArrayEquals("It can show top zone",top2Zone, w.getTop(2).toArray());
	}
}
