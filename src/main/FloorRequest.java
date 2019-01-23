package main;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Timestamp;

public class FloorRequest implements Serializable {
	Timestamp timestamp;
	int floor, 
		carButton;
	String floorButton;
	
	public FloorRequest(Timestamp timestamp, int floor, int carButton, String floorButton) {
		this.timestamp = timestamp;
		this.floor = floor;
		this.carButton = carButton;
		this.floorButton = floorButton;

		System.out.println("Creating a floor request with:\n\t" + "Timestamp: " + timestamp + "\n\tFloor: " + floor + "\n\tCar Button: " + carButton + "\n\tFloor Button: " + floorButton);
	}
	
	public FloorRequest(byte [] data) {
		// TODO
	}
	
	public byte [] getBytes() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte [] result;
		try {
		  out = new ObjectOutputStream(bos);   
		  out.writeObject(this);
		  out.flush();
		  result = bos.toByteArray();
		} finally {
			try {
		    bos.close();
		  } catch (IOException e) {
			  e.printStackTrace();
		  }
		}
		return result;
	}
}
