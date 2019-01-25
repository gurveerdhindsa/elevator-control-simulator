package main;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FloorRequest implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Timestamp timestamp;
	private int floor, 
		        carButton;
	private String floorButton;
	
	public FloorRequest(Timestamp timestamp, int floor, int carButton, String floorButton) {
		this.timestamp = timestamp;
		this.floor = floor;
		this.carButton = carButton;
		this.floorButton = floorButton;

		System.out.println("Creating a floor request with:\n\t" + "Timestamp: " + timestamp + "\n\tFloor: " + floor + "\n\tCar Button: " + carButton + "\n\tFloor Button: " + floorButton);
	}
	
	public byte [] getBytes() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte [] result = null;
		try {
		  out = new ObjectOutputStream(bos);   
		  out.writeObject(this);
		  out.flush();
		  result = bos.toByteArray();
		  
		  if (bos != null)
			  bos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static Object getObjectFromBytes(byte [] input) {
		ByteArrayInputStream bis = new ByteArrayInputStream(input);
		ObjectInput in = null;
		Object result = null;
		try {
		  in = new ObjectInputStream(bis);
		  result = in.readObject();
		  
		  if (in != null)
			  in.close();
		} catch(IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public Timestamp getTimestamp() {
		return this.timestamp;
	}
	
	public int getFloor() {
		return this.floor;
	}
	
	public int getCarButton() {
		return this.carButton;
	}
	
	public String getFloorButton() {
		return this.floorButton;
	}
}
