package test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import main.FloorRequest;

public class FloorRequestTest {
	FloorRequest fr;
	
	Timestamp timestamp = null;
	int floor = 2,
	    carButton = 4;
	String floorButton = "Up";
	
	public Timestamp floorTime;
	public Timestamp doorTime;
	
	
	@Before
	public void initialize() {
		try {
		    SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss.SSS");
		    Date parsedDate = dateFormat.parse("14:05:15.0");
		    timestamp = new Timestamp(parsedDate.getTime());
		    
		    Date floor = dateFormat.parse("00:00:08.0");
		    floorTime = new Timestamp(floor.getTime());
		    
		    Date door = dateFormat.parse("00:00:02.0");
		    doorTime = new Timestamp(door.getTime());
		    
		} catch (ParseException e) {
			e.printStackTrace();
		}
	    
	    fr = new FloorRequest(timestamp, floor, carButton, floorButton, floorTime, doorTime);
	}

	@Test
	public void Should_Convert_Object_To_Byte_Array() throws ParseException, IOException {
		assertTrue(fr.getBytes() instanceof byte[]);
	}
	
	@Test
	public void Should_Convert_Byte_Array_To_Object() {
		// Convert our initialized object to a byte array then back to an object
		FloorRequest result = (FloorRequest) fr.getObjectFromBytes(fr.getBytes());
		
		// Verify the timestamp, floor, car button and floor button match
		assertTrue(result.timestamp.equals(this.timestamp));
		assertTrue(result.floor == this.floor);
		assertTrue(result.carButton == this.carButton);
		assertTrue(result.floorButton.equals(this.floorButton));		
	}

}
