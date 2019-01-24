// Elevator.java
package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

public class Elevator implements Runnable{
	
	private DatagramSocket receiveSocket; //non primitive fields start as null
	private DatagramSocket SendSocket;
	private int currentfloor;
	private int destinationfloor;
	private boolean stationary;
	private int portNumber;
	Thread motorThread;
	Thread messageThread;

	
	public Elevator(int portNumber)
	{
		this.stationary = true;
		this.portNumber = portNumber;
		this.currentfloor = 0;
		this.destinationfloor = 0;
		this.motorThread = new Thread(this, "motorThread");
		this.messageThread = new Thread(this, "messageThread");
	
		try {
			receiveSocket = new DatagramSocket(portNumber);
		}catch (SocketException e)
		{
			e.printStackTrace();
			System.out.println("Elevator not created");
		}
	}
	
	
	//first thing it receive after being created is move from floor to floor
	
	public byte[]  getRequest()
	{
		byte data[] = new byte[100];
	    DatagramPacket receiveClientPacket = new DatagramPacket(data, data.length);
	    System.out.println("IntermediateHost: Waiting for Packet.\n");
	    // Block until a datagram packet is received from receiveSocket.
        try {
        	System.out.printf("Elevatorwaiting for movement request\n");
        	this.receiveSocket.receive(receiveClientPacket);
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
        
        byte[] pcktmsg = receiveClientPacket.getData();
        
        byte[] msgRcd = new byte[1];
        msgRcd[0] = pcktmsg[0];
        System.out.println(Arrays.toString(msgRcd));
        
        return msgRcd;

	}
	
	public void forever()
	{
		System.out.println(this.motorThread.getName());
		System.out.println(Thread.currentThread().getName());
		
		byte[] registerElev = new byte[] {0,0,0,0,0};
		byte port = (byte) this.portNumber;
		registerElev[4] = port;
		
		try {
			System.out.println("Sending register elevator");
			DatagramPacket pck = new DatagramPacket(registerElev, registerElev.length, 
					InetAddress.getLocalHost(),69);
			DatagramSocket soc = new DatagramSocket();
			soc.send(pck);
			soc.close();
			System.out.println("Sent register elevator");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		while(true)
		{
			byte[] msg;
			msg = getRequest();
			if(msg[0] == (byte)0)
			{
				//move motor or move elevator
				if(this.motorThread.getState() == Thread.State.TIMED_WAITING)
				{
				  System.out.println("Elevator already moving");
				}
				
				else
				{
					System.out.println("Elevator moving");
					this.motorThread.start();
					
					System.out.println("Listening for possible new message");
				}
				
			}
			else if(msg[0] == (byte)1)
			{
				this.motorThread.interrupt();
			}
		}
		
	}
	public void start()
	{
		this.messageThread.start();
		System.out.printf("Main thread: %s done its job\n",Thread.currentThread().getName());
	}
	
	public void run()
	{
		System.out.println(Thread.currentThread().getName());
		if(Thread.currentThread().getName().equals("messageThread"))
		{
			this.forever();
		}
		else 
		{
			handleMovement();
		}

	}
	public void handleMovement()
	{
		try {
			Thread.sleep(100000);
			System.out.println("Elevator got to final destination");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println(e.getMessage());
			System.out.println("Elevator stopped before final destination");
			//hanlde that
		}
		
		//sendPacket to scheduler that elevator at final destination
	}
	
	public int getPortNumber()
	{
		return this.portNumber;
	}
	//This method is used to process the data received from scheduler 
	/*public void sendandreceive() {
		
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

	*/
	public static void main( String args[] ) {
		
		Elevator server = new Elevator(23);
		server.start();
		//send port number to scheduler first
	}
}