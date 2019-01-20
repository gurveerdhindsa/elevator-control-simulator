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
	
	// For now, we'll use a HashMap to store the configuration & assume one set of input
	// Data Types: Time: Timestamp, Floor: Integer, Floor Button: String, Car Button: Integer
	private HashMap<String, Object> config;
	
	// Constants
	private static final int DESTINATION_PORT = 23;
	
	public Floor() {
		config = new HashMap<String,Object>();
		
		// Import from configuration.txt
		importConfiguration();
		
		
		// *Optional* Log the configuration we've imported
		System.out.println("Imported configuration: ");
		for (Map.Entry<String, Object> entry : config.entrySet()) {
			System.out.println("\t" + entry.getKey() + " : " + entry.getValue());
		}
		
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
			while ((input = br.readLine()) != null) {
				String [] inputFields = input.split(":",2);				
				if (inputFields[0].equals("Time")) {
					try {
					    SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss.SSS");
					    Date parsedDate = dateFormat.parse(inputFields[1]);
					    Timestamp timestamp = new Timestamp(parsedDate.getTime());

						config.put(inputFields[0], timestamp);
					} catch (ParseException e) {
						e.printStackTrace();
					}
				} else if (inputFields[0].equals("Floor") || inputFields[0].equals("Car Button")) {
					config.put(inputFields[0], Integer.parseInt(inputFields[1].trim()));
				} else if (inputFields[0].equals("Floor Button")){
					config.put(inputFields[0], inputFields[1].trim());
				}
			}
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