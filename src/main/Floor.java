// Floor.java

package main;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.sql.Timestamp;

public class Floor {
	
	private DatagramPacket sendPacket,			// Packet to send to Scheduler
	   					   receivePacket;		// Packet to receive from Scheduler
	private DatagramSocket sendReceiveSocket;	// Socket to send and receive packets
	
	// Constants
	private static final int DESTINATION_PORT = 23;
	
	public Floor() {
		
		System.out.println("Importing configuration...");
		
		// Import from configuration.txt
		importConfiguration();
		
		try {
			// Create datagram socket and bind to any available port
			// This socket will be used to send/receive UDP Datagram packets
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException err) {   // Unable to create the socket
			err.printStackTrace();
			System.exit(1);
		}
	}
	
	
	public void importConfiguration() {
		try (BufferedReader br = new BufferedReader(new FileReader("src/configuration/configuration.txt"))) {
			String input = null;
			
			Timestamp timestamp = null;
			int floor = 0, carButton = 0;
			String floorButton = null;
			
			while ((input = br.readLine()) != null) {
				String [] inputFields = input.split(":",2);				
				if (inputFields[0].equals("Time")) {
					try {
					    SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss.SSS");
					    Date parsedDate = dateFormat.parse(inputFields[1]);
					    timestamp = new Timestamp(parsedDate.getTime());
					} catch (ParseException e) {
						e.printStackTrace();
					}
				} else if (inputFields[0].equals("Floor")) {
					floor = Integer.parseInt(inputFields[1].trim());
				} else if (inputFields[0].equals("Car Button")) {
					carButton = Integer.parseInt(inputFields[1].trim());
				} else if (inputFields[0].equals("Floor Button")){
					floorButton = inputFields[1].trim();
				}
			}
			
			FloorRequest floorRequest = new FloorRequest(timestamp, floor, carButton, floorButton);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Main method
	 * @param args
	 */
	public static void main (String [] args) {
		Floor floor = new Floor();
	}
	
}