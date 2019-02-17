// Floor.java

package main;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.sql.Timestamp;

/**
 * Floor sends a floor request to the scheduler
 * Floor receives when the elevator is moving
 * Every 8 secs it sends a packet to the Scheduler
 * letting it know it is moving to the next floor up/down
 *
 */

public class Floor implements Runnable {
	private Thread sendThread, sendReceiveThread;
	private DatagramSocket sendReceiveSocket; 	// Socket to send and receive packets
	private List<FloorRequest> floorRequests;	// List of requests to be made

	/**
	 * Creates 2 threads, one to receive and one to send packets to/from scheduler
	 * 
	 */
	public Floor() {
		sendThread = new Thread(this, "sendThread");
		sendReceiveThread = new Thread(this, "sendReceiveThread");
		
		floorRequests = new ArrayList<FloorRequest>();

		importConfiguration();

		try {
			this.sendReceiveSocket = new DatagramSocket();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		sendThread.start();
		sendReceiveThread.start();
	}
	
	/**
	 * Reads the input file configuration.txt, reads in the time stamp , floor, floor button and car button
	 * Creates an arrayList of floor request from the file and adds to the list
	 * @param
	 * @return 
	 */
	public void importConfiguration() {
		try (BufferedReader br = new BufferedReader(new FileReader("src/configuration/configuration.txt"))) {
			String input = null;
			Timestamp timestamp = null;
			int floor = 0, carButton = 0;
			String floorButton = null;

			while ((input = br.readLine()) != null) {
				String[] inputFields = input.trim().split(":", 2);
				if (inputFields[0].equals("Time")) {
					try {
						SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss.SSS");
						Date parsedDate = dateFormat.parse(inputFields[1]);
						timestamp = new Timestamp(parsedDate.getTime());
					} catch (ParseException e) {
						e.printStackTrace();
					}
				} else if (inputFields[0].equals("Floor")) {
					floor = Integer.parseInt(inputFields[1]);
				} else if (inputFields[0].equals("Car Button")) {
					carButton = Integer.parseInt(inputFields[1]);
				} else if (inputFields[0].equals("Floor Button")) {
					floorButton = inputFields[1].trim();
					floorButton = floorButton.toLowerCase();
				} else if (input.isEmpty() && timestamp != null && !floorButton.isEmpty()) {
					floorRequests.add(new FloorRequest(timestamp, floor, carButton, floorButton));
				}
			}
			if (timestamp != null) {
				floorRequests.add(new FloorRequest(timestamp, floor, carButton, floorButton));
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends the floor request to the scheduler on port 45, the first byte being a 0,
	 * meaning it is a floor request. Each request will result in a packet being sent 
	 * to scheduler
	 * @param
	 * @return 
	 */
	public void send() {
		// edit to send all requests with random 
		// time in between requests
		DatagramPacket packet;
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			
			for (FloorRequest floorRequest : floorRequests) {
				// Initial 0 byte to signify a floor request
				buffer.write((byte) 0);
				// For now, we will just send one floor request
				buffer.write(floorRequest.getBytes());
				byte[] request = buffer.toByteArray();
				// Create datagram packet to send
				packet = new DatagramPacket(request, request.length, InetAddress.getLocalHost(), 45);
				System.out.println(new SimpleDateFormat("hh:mm:ss.SSS").format(floorRequest.timestamp) + ": Passenger request to go " + floorRequest.floorButton.toLowerCase() + " from floor " + floorRequest.floor + " to " + floorRequest.carButton);

				sendReceiveSocket.send(packet);
				try {
					Thread.sleep(1700);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// Reset the buffer for the next floor request
				buffer.reset();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Floor requests sent!");
		}
	}
	
	/**
	 * Executes the both send and receive threads
	 * @param
	 * @return 
	 */
	@Override
	public void run() {
		if (Thread.currentThread().getName().equals("sendThread"))
			this.send();
	}

	/**
	 * Main method
	 * Creates instance of floor method and runs the 
	 * @param args
	 */
	public static void main(String[] args) {
		new Floor();
	}
}