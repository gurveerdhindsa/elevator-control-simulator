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
	
	
	@Before
	public void initialize() {
		// Mock data
		Timestamp timestamp = null;
		int floor = 2, 
			carButton = 4;
		String floorButton = "Up";

		try {
		    SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss.SSS");
		    Date parsedDate = dateFormat.parse("14:05:15.0");
		    timestamp = new Timestamp(parsedDate.getTime());
		} catch (ParseException e) {
			e.printStackTrace();
		}
	    
	    fr = new FloorRequest(timestamp, floor, carButton, floorButton);
	}

	@Test
	public void Should_Convert_Object_To_Byte_Array() throws ParseException, IOException {
		assertTrue(fr.getBytes() instanceof byte[]);
	}

}
