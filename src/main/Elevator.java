// Elevator.java
package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Elevator implements Runnable{
	
	private DatagramSocket receiveSocket; //non primitive fields start as null
	private DatagramSocket SendSocket;
	private int currentfloor;
	private int destinationfloor;
	private boolean stationary;
	private int portNumber;
	private boolean doorsOpen;   //false = door closed 
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
	
	
	//function can maybe be properly named
	//basically just listens for packets from
	//scheduler.
	// can be moved into the while loop of 
	//forever and cleaned up 
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
		
		//don't know how we want to this yet
		//maybe create a schedulerElevator instance 
		//fill in the required of port number convert it to bytes
		//or looks like just port number might be enough for registration
				
		byte[] registerElev = new byte[] {0,0,0,0,0};
		byte port = (byte) this.portNumber;
		registerElev[4] = port;
		
		try {
			System.out.println("Sending register elevator");
			//later on need elevator to know host of and port of scheduler when being instantiated
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
			
			//for message sequence here, need destination floor and 
			//what else?
			if(msg[0] == (byte)0)
			{
				//this is assuming that schdeuler never sends move request
				//to an elevator already moving
				//message should contain destination floor (and whether up or down) 
				//or up or down can be calculated from destination floor - current flooor
				//update destination floor field before starting move thread
				//can do fancy console printing if like
				
				//Message received format: [0 - Current Floor - Direction - Destination Floor]
				currentfloor = msg[1];
				destinationfloor = msg[3];
				//direction = destinationfloor - currentfloor
				 
				System.out.println("Elevator moving");
				stationary = false;
				this.motorThread.start();
				//send packet to scheduler that elevator moving(later iteration?)
				System.out.println("Listening for possible new message");
				
			}
			//if scheduler is interrupting an elevator before its gets to it's final destination then 
			//message should include the currentFloor the elevator is on 
			else if(msg[0] == (byte)1)
			{
				//update this.currentFloor with current floor from message
				//then stop elevator
				this.motorThread.interrupt();
				stationary = true;
				 
			}
			
			//else if msg[0] == 2 is door close. so close door
			//send back door closed to the scheduler 
			//door close message received
			else if(msg[0]==(byte)2 && msg.length==1) {
				
				System.out.println("Closing doors. ");
				doorsOpen = false;
				byte[] doorCloseMsg = new byte[] {2};
				try {
					System.out.println("Sending door close message. ");
					DatagramPacket doorClosePkt = new DatagramPacket(doorCloseMsg, doorCloseMsg.length, InetAddress.getLocalHost(),69);
					DatagramSocket doorMsgSocket = new DatagramSocket();
					doorMsgSocket.send(doorClosePkt);
					doorMsgSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			//door open message received
			else if(msg[0]==(byte)3 && msg.length==1) {
				
				System.out.println("Opening doors. ");
				doorsOpen = true;
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
		
		//here function should calculate the number of floors it needs to move 
		int floorDiff = Math.abs(currentfloor-destinationfloor);
		//multiply the avg 10000milliseconds to 1 floor by the number of floors 
		try {
			Thread.sleep(floorDiff*100000);
			//if thread wakes up on its own then it got to the final destination 
			//that was updated when the move request was received
			//so we update current floor as that floor
			//send packet to scheduler elevator has arrived
			currentfloor = destinationfloor;
			
			byte[] arrivalMessage = new byte[] {4};   //byte 4 is used to represent arrival to destination
			DatagramPacket arrivalMsgPkt = new DatagramPacket(arrivalMessage, arrivalMessage.length, InetAddress.getLocalHost(),69);
			DatagramSocket arrivalMsgSocket = new DatagramSocket();
			arrivalMsgSocket.send(arrivalMsgPkt);
			arrivalMsgSocket.close();
			
			System.out.println("Elevator got to final destination");
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
			System.out.println("Elevator stopped before final destination");
			//if elevator was stopped can just print or something
			//sendPacket to scheduler that elevator has stopped(maybe later iteration)
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	public static void main( String args[] ) {
		
		Elevator server = new Elevator(23);
		server.start();
		//send port number to scheduler first
	}
}