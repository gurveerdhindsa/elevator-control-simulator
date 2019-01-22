// Elevator.java
package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

public class Elevator {
	
	private static DatagramSocket receiveSocket; //non primitive fields start as null
	private static DatagramSocket responseSocket;
	private static DatagramPacket receivePacket;
	private static DatagramPacket responsePacket;
	String responsePacketS;
	String receivePacketS;

	//Number of elevator buttons will depend on number of floors
	boolean button1 = false;
	boolean button2 = false;
	boolean button3 = false;
	boolean button4 = false;
	
	//Number of elevator lamps will depend on number of floors/buttons
	boolean lamp1 = false;
	boolean lamp2 = false;
	boolean lamp3 = false;
	boolean lamp4 = false;
	
	//Elevator motor?
	
	//Elevator door?
	
	
	public Elevator() {
		
		//Elevator (server) creates a DatagramSocket to use receive on port 69
		try{
			receiveSocket = new DatagramSocket(69);
		} catch (SocketException e) {
			e.printStackTrace();
			System.out.println("Cannot open socket on port 69.");
		}
	}
	
	//This method is used to process the data received from scheduler 
	public void sendandreceive() {
		
       for(;;){
			
			//Waiting to receive a request
			//Construct a DatagramPacket to receive packets upto 100bytes long from Intermediate Host
			byte data[] = new byte[100];
			receivePacket = new DatagramPacket(data, data.length);
			System.out.println("Waiting to receive packet from scheduler.");
			
			//Receiving and printing data received
			try{
				receiveSocket.receive(receivePacket);
				receivePacketS = new String(data,0,receivePacket.getLength());    //convert the received packet to a String
				System.out.println("Data received from scheduler (in bytes): " + receivePacket);
				System.out.println("Data received  from scheduler (in String): " + receivePacketS);	
			}
			catch (IOException e) {
				e.printStackTrace();
		        System.exit(1);
			} 
			
			//Parse the packet to confirm the format is valid 
			String[] parsedPacket = receivePacketS.split(" ");   //split the received packet at each "white space" from input file
			
			//Content after parsing: Time - Initial floor - Direction - Destination floor
			String time = parsedPacket[0];
			String initialFloor = parsedPacket[1];
			String direction = parsedPacket[2];
			String destFloor = parsedPacket[3];
		
			
			//Example of how elevator moves to initialFloor using the motor:
			if(initialFloor=="2"){
			   //if PREVIOUS destFloor was greater than this initialFloor, motor = move elevator down to floor 2, otherwise motor = move up
			   //open doors
			 }
			
			//After passenger gets on elevator
			//Example of passenger selecting button inside elevator for which floor he wants to go to -> turning on button and lamp
			if(destFloor=="1") {
				button1 = true;
				lamp1 = true;
			}
			if(destFloor=="2") {
				button2 = true;
				lamp2 = true;
			}
			
			//Close doors
			
			//Example of determining whether elevator is moving up or down
			if(direction=="Up") {
				//motor moves elevator up
			}
			else {
				//motor moves elevator down
			}
			
			//Once we reach the destination floor we turn the lamp off:
			   //if(current time = initial time + total time it takes for elevator to move from initial floor to destination floor){
			       //open doors
			       lamp1 = false;
			   //}
			//for total time measurement, we have to take into account time to open doors, move between floors (initial to dest) and close doors
				
			//Print out the response packet information
			System.out.println("Response being sent to scheduler: " + responsePacketS);
				
			
			//Creating DatagramSocket to send response to Intermediate Host
			try{
				responseSocket = new DatagramSocket();
			} catch (SocketException e) {
				responseSocket.close();
			} 
			
			//Sending request to host
		    try {
				responseSocket.send(responsePacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
	}

	
	public static void main( String args[] ) {
		
		Elevator server = new Elevator();
		server.sendandreceive();
			
	}
}