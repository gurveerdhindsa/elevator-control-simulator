// FloorRequest.java

package main;

import java.io.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * The Class FloorRequest used to instantiate a request to move the elevator
 */
public class FloorRequest implements Serializable {

	public Timestamp timestamp; 		// timestamp of request
	public int floor, 				// origin of request
			   carButton; 			// destination of request
	public String floorButton; 		// direction of request

	/**
	 * Constructor for class FloorRequest
	 */
	public FloorRequest(Timestamp timestamp, int floor, int carButton, String floorButton) {
		this.timestamp = timestamp;
		this.floor = floor;
		this.carButton = carButton;
		this.floorButton = floorButton;

		System.out.println("Creating a floor request with:\n\t" + "Timestamp: " + new SimpleDateFormat("hh:mm:ss.SSS").format(timestamp) + "\n\tFloor: " + floor + "\n\tCar Button: " + carButton + "\n\tFloor Button: " + floorButton);
	}

	/**
	 * Constructor for class FloorRequest
	 */
	public FloorRequest() {
	}

	/**
	 * Converts the current instance of FloorRequest into a byte array
	 * 
	 * @returns the byte array message
	 */
	public byte[] getBytes() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] result = null;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(this);
			out.flush();
			result = bos.toByteArray();

			if (bos != null)
				// Close the stream
				bos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Converts a byte array into a FloorRequest object
	 * 
	 * @param input
	 *            - a byte array
	 * @returns a FloorRequest instance
	 */
	public static Object getObjectFromBytes(byte[] input) {
		ByteArrayInputStream bis = new ByteArrayInputStream(input);
		ObjectInput in = null;
		Object result = null;
		try {
			in = new ObjectInputStream(bis);
			result = in.readObject();

			if (in != null)
				// Close the stream
				in.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return result;
	}
}
