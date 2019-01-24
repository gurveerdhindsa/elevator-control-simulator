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
	
	public Floor() {
		
		try {
			this.sendReceiveSocket = new DatagramSocket();
		}
		catch(Exception e)
		{
			
		}
		
		//read line, verify that line follows the 
		//following pattern
		
		/*config = new HashMap<String,Object>();
		
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
		}*/
	}
	
	
	/*public void importConfiguration() {
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
	}*/
	
	public void sendReceive()
	{
		byte[] request = new byte[] {0,5};
		DatagramPacket packet;
		try {
			packet = new DatagramPacket(request,request.length,
					InetAddress.getLocalHost(),45);
			System.out.println("Sending floor request");
			sendReceiveSocket.send(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		byte[] waitforMove = new byte[100];
		
		DatagramPacket movement = new DatagramPacket(waitforMove, waitforMove.length);
		try {
			sendReceiveSocket.receive(movement);
			System.out.println("Receive that elevator is moving to floor");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//wait till elevator almost reaching floor 2
		pause();
		
		byte[] stopelevator = new byte[] {3};
		
		try {
			DatagramPacket stop = new DatagramPacket(stopelevator,stopelevator.length,InetAddress.getLocalHost(),
					45);
			System.out.println("Stopping elevator");
			this.sendReceiveSocket.send(stop);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	static void pause(){
	    long Time0 = System.currentTimeMillis();
	    long Time1;
	    long runTime = 0;
	    while(runTime<9000){
	        Time1 = System.currentTimeMillis();
	        runTime = Time1 - Time0;
	    }
	}
	
	
	/**
	 * Main method
	 * @param args
	 */
	public static void main (String [] args) {
		Floor floor = new Floor();
		floor.sendReceive();
	}
	
}