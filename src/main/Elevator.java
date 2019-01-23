// Elevator.java
package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Elevator extends Thread {
	
	
	//private static DatagramSocket receiveSocket; //non primitive fields start as null
	private static DatagramSocket arrivalMessageSocket;
	private static DatagramPacket receivePacket;
	private static DatagramPacket arrivalMessagePacket;
	private String receivePacketS = "";
	private final int floorInterval = 13;
	
	boolean[] buttons;    //Number of elevator buttons will depend on number of floors
	boolean[] lamps;      //Number of elevator lamps will depend on number of floors/buttons
	
	//Elevator motor?
	//stationary=false; moving = true;
	boolean isMotorOn = false;
	//Elevator door?
	//closed = false; open = true;
	boolean doors = false;
	
	
	//variables to represent data received from scheduler
	private int currentFloor;
	private int destFloor;
	
	private DatagramSocket receiveSocket; //non primitive fields start as null
	private DatagramSocket SendSocket;
	private boolean stationary;        //using this variable, the scheduler will determine if the elevator car is currently stationary or moving
	private int portNumber;
	
	public Elevator(int portNumber)
	{
		this.stationary = true;
		this.portNumber = portNumber;
		try {
			receiveSocket = new DatagramSocket(portNumber);       //each elevator car will receive on different port
			arrivalMessageSocket = new DatagramSocket(5000);
		}catch (SocketException e)
		{
			e.printStackTrace();
			System.out.println("Elevator not created");
		}
	}
	
	private void getRequest()
	{
		byte data[] = new byte[100];
	    DatagramPacket receiveClientPacket = new DatagramPacket(data, data.length);
	    System.out.println("IntermediateHost: Waiting for Packet.\n");
	    // Block until a datagram packet is received from receiveSocket.
        try {
        	System.out.printf("Elevator %s waiting for movement request\n",this.getName());
        	receiveSocket.receive(receiveClientPacket);
        }
        catch(IOException e)
        {
        	System.out.print("IO Exception: likely:");
            System.out.println("Receive Socket Timed Out.\n" + e);
            e.printStackTrace();
        }
        
        //request gotten
        //fill in destination floor and assuming that we will just start moving for now  
        System.out.println("got request");        
        
        try {
        	Thread.sleep(10000);
        }
        catch(InterruptedException e)
        {
        	System.out.println("Stopping elevator");
        }
	}
	
	public int getPortNumber()
	{
		return this.portNumber;
	}
	public void run()
	{
		getRequest();
	}
	
	
	//This method is used to process the data received from scheduler 
	public void sendandreceive() {
		
       for(;;){
			
			//Waiting to receive a request
			//Construct a DatagramPacket to receive packets upto 100bytes long from Intermediate Host
    	   	System.out.println("Waiting to receive packet from scheduler.");
			byte floorData[] = new byte[100];
			receivePacket = new DatagramPacket(floorData, floorData.length);
		
			//Receiving and printing data received
			try{
				receiveSocket.receive(receivePacket);
				
				//  Checks for move command (motor = true/false)
				//Assuming first index of packet is motor ON/OFF 
				isMotorOn = (floorData[0]!=0); // converts byte into bool
				if (isMotorOn == true) {
					this.stationary = true;
				}
				else {
					this.stationary = false;
				}
				receivePacketS = new String(floorData,0,receivePacket.getLength());    //convert the received packet to a String
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
			String currentFloorS = parsedPacket[0];
			String destFloorS = parsedPacket[1];
			
			currentFloor = Integer.valueOf(currentFloorS);
			System.out.println("Currently at Floor: "+ currentFloor);
			destFloor = Integer.valueOf(destFloorS);
			System.out.println("Going to Floor " + destFloor);
			String arrivalMessageS = "The elevator car has arrived at floor number " + currentFloorS;
			byte arrivalMessageB[] = arrivalMessageS.getBytes();
			arrivalMessagePacket = new DatagramPacket(arrivalMessageB , arrivalMessageB.length, receivePacket.getAddress(), receivePacket.getPort());
			
			//Example of how elevator moves to initialFloor using the motor (moving from floor 1 to floor 2):
			if(currentFloor==2){
			   
				//Elevator starts moving towards currentFloor (floor 1 to floor 2) after receiving packet
			   
				doors = false;     //close doors
				this.stationary = true;      //elevator starts moving towards currentFloor
				
				
				try {
					TimeUnit.SECONDS.sleep(floorInterval);    //13 seconds to move from floor 1 to floor 2
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				//after arriving at currentFloor
				stationary = false;   //elevator stops moving
				doors = true;    //open doors
				
				//send arrival message to scheduler
				try {
					arrivalMessageSocket.send(arrivalMessagePacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				//Print out the arrival message packet information
				System.out.println("Response being sent to scheduler: " + arrivalMessagePacket);
				
			 }
			
			//After passenger gets on elevator
			//Example of passenger selecting button inside elevator for which floor he wants to go to -> turning on button and lamp
		    buttons[destFloor] = true;
			lamps[destFloor] = true;
			
			
			//Close doors
			doors = false;
			this.stationary = true;    //elevator starts moving again towards destination floor
			
			//time taken to move from current floor to destination floor
			//Example: 2nd floor to 5th floor:  (13*(5-2)=39seconds)
			elevatorMoving(destFloor, currentFloor, floorInterval);
			
			//After arriving at destination floor
			lamps[destFloor] = false;      //turn off destination floor lamp
			buttons[destFloor] = false;      //turn off destination floor button
			
			this.stationary = false;    //elevator stops moving
			doors = true;     //open doors	
			
		}
		
	}
	
	public void elevatorMoving(int destFloor, int currFloor, int interval) {
		
		try {
			TimeUnit.SECONDS.sleep(interval*Math.abs(destFloor-currFloor));   
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

	
	public static void main( String args[] ) {
		
		Elevator car1 = new Elevator(69);
		car1.sendandreceive();
		
		//Elevator car2 = new Elevator(70);
		//car2.sendandreceive();
		
		//Elevator car3 = new Elevator(71);
		//car2.sendandreceive();
		
		//Elevator car4 = new Elevator(72);
		//car2.sendandreceive();
	}
}