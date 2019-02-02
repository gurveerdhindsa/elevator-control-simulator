// Elevator.java
package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

public class Elevator implements Runnable{
	
	private DatagramSocket receiveSocket; //non primitive fields start as null
	private int currentfloor;
	private int destinationfloor;
	private boolean stationary;
	private int portNumber;
	private boolean doorsOpen;   //false = door closed 
	private Thread motorThread;
	private Thread messageThread;
	private LinkedList<Integer>pendingDestinations;
	private int direction; // 1 is up -1 is down
	private int initial;
	private int motorExit;

	
	public Elevator(int portNumber)
	{
		this.stationary = true;
		this.portNumber = portNumber;
		this.currentfloor = 0;
		this.destinationfloor = 0;
		this.messageThread = new Thread(this, "messageThread");
		this.pendingDestinations = new LinkedList<>();
		this.doorsOpen = true;
	
		try {
			receiveSocket = new DatagramSocket(portNumber);
		}catch (SocketException e)
		{
			e.printStackTrace();
			System.out.println("Elevator not created");
		}
	}
	
	private synchronized int peekPending()
	{
		return this.pendingDestinations.peekFirst();
	}
	private synchronized int getPendingDest()
	{
		return this.pendingDestinations.removeFirst();
	}
	
	private synchronized void addPendingDest(int destination)
	{
		if(this.pendingDestinations.isEmpty())
		{
			this.pendingDestinations.add(destination);
			return;
		}
		
		//if direction is up
		
		//if direction is down
		this.pendingDestinations.add(destination);
		if(this.getDirection() == 1)
		{
			Collections.sort(this.pendingDestinations);
		}
		else
		{
			Collections.sort(this.pendingDestinations, Collections.reverseOrder());
		}
		
	}
	
	private synchronized int motorExit(int read)
	{
		if(read == 1)
		{
			return this.motorExit;
		}
		
		this.motorExit = 1;
		return 0;
	}
	private synchronized int anyPendingDest()
	{
		if(this.pendingDestinations.isEmpty())
		{
			return 0;
		}
		
		return 1;
	}
	
	public void forever()
	{
		System.out.println(Thread.currentThread().getName());
		
		//don't know how we want to this yet
		//maybe create a schedulerElevator instance 
		//fill in the required of port number convert it to bytes
		//or looks like just port number might be enough for registration
		while(true)
		{
			
			byte data[] = new byte[100];
		    DatagramPacket receiveClientPacket = new DatagramPacket(data, data.length);
		    //System.out.println("IntermediateHost: Waiting for Packet.\n");
		    // Block until a datagram packet is received from receiveSocket.
	        try {
	        	System.out.printf("Elevatorwaiting for movement request\n");
	        	this.receiveSocket.receive(receiveClientPacket);
	        }
	        catch(IOException e)
	        {
	        	System.out.print("IO Exception: likely:");
	            e.printStackTrace();
	        }


			byte[] msg = receiveClientPacket.getData();
			
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
				
				//Message received format: [0 - floor - carButton]
				System.out.println("Got request with contents");
				System.out.println(Arrays.toString(msg));
				
				if(this.getCurrentFloor() == msg[1])
				{
					this.setDestFloor(msg[2]);
				}
				else 
				{
					this.setDestFloor(msg[1]);
					addPendingDest((int)msg[2]);
				}
				this.setDirection(); 
				this.stationary = false;
				this.motorThread = new Thread(this, "motorThread");
				this.motorThread.start();
				
				
				//send packet to scheduler that elevator moving(later iteration?)
				
			}
			//if scheduler is interrupting an elevator before its gets to it's final destination then 
			//message should include the currentFloor the elevator is on 
			else if(msg[0] == (byte)1)
			{
				//update this.currentFloor with current floor from message
				//should be if going up 
				this.currentfloor = (int)msg[1];
				addPendingDest((int)msg[2]);
				this.motorThread.interrupt();
				stationary = true;
				this.doorsOpen = true;
				 
			}
			
			//else if msg[0] == 2 is door close. so close door
			//send back door closed to the scheduler 
			//door close message received
			else if(msg[0]==(byte)2) {
				
				System.out.println("Closing doors. ");
				this.doorsOpen = false;
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

		}
		
	}
	public void start()
	{
		byte[] registerElev = new byte[] {0,0,0,1,0};
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
		
		this.messageThread.start();
	}
	
	public void run()
	{
		System.out.println(Thread.currentThread().getName());
		
		if(Thread.currentThread().getName().equals("messageThread"))
		{
			this.forever();
		}
		else if(Thread.currentThread().getName().equals("motorThread"))
		{
			this.handleMovement();
		}
		else
		{
			this.start();
		}

	}
	
	private Boolean mimicMovement()
	{
		//here function should calculate the number of floors it needs to move 
		int floorDiff = Math.abs(this.getCurrentFloor() - this.getDestFloor());
		//multiply the avg 10000milliseconds to 1 floor by the number of floors 
		try {
			System.out.println("Elevator moving");
			Thread.sleep(floorDiff*10000);
			//if thread wakes up on its own then it got to the final destination 
			//that was updated when the move request was received
			//so we update current floor as that floor
			//send packet to scheduler elevator has arrived
			
			this.setCurrentFloor(this.getDestFloor());;
			int pendingR = this.anyPendingDest();
			System.out.println("pendingR " + pendingR);
			int destination = pendingR == 1 ? this.peekPending() : -1;
			byte[] arrivalMessage = new byte[4];   //byte 4 is used to represent arrival to destination
			arrivalMessage[0] = 5;
			arrivalMessage[1] = (byte)pendingR;
			arrivalMessage[2] = (byte)destination;
			arrivalMessage[3] = (byte)this.getCurrentFloor();
			
			DatagramPacket arrivalMsgPkt = new DatagramPacket(arrivalMessage, arrivalMessage.length, InetAddress.getLocalHost(),69);
			DatagramSocket arrivalMsgSocket = new DatagramSocket();
			arrivalMsgSocket.send(arrivalMsgPkt);
			arrivalMsgSocket.close();
			
			System.out.printf("Elevator got to destination floor: %d\n",destinationfloor);
			
			return pendingR == 1;
			
			
		} catch (IOException e) {
			
			//some better handling here(later iteration)
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Throwable  s = e.getCause();
			System.out.println(s.getClass().getName());
			System.out.printf("Elevator stopped at floor %d to answer request\n",currentfloor);			
		}
		
		return false;
	}
	public void handleMovement()
	{
		while(mimicMovement())
		{
			this.setDestFloor(this.getPendingDest());
			this.setDirection();
		}
	}
	
	public synchronized int  getCurrentFloor()
	{
		return this.currentfloor;
	}
	
	public synchronized int getDestFloor()
	{
		return this.destinationfloor;
	}
	
	public synchronized void setDestFloor(int floor)
	{
		this.destinationfloor = floor;
	}
	
	public synchronized void setCurrentFloor(int floor)
	{
		this.currentfloor = floor;
	}
	
	public synchronized void setDirection()
	{
		this.direction = (this.destinationfloor - this.currentfloor)/
				Math.abs(this.destinationfloor - this.currentfloor);
	}
	
	public synchronized int getDirection()
	{
		return this.direction;
	}
	
	public static void main(String[] args)
	{
		Elevator e = new Elevator(70);
		e.start();
	}
}