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

/**
 * Represents the Elevator Subsystem,
 * which when instantiated notifies the
 * Scheduler subsystem of its existence and 
 * sit idle till it receives a request. 
 * When Message received from Scheduler it is 
 * decoded and appropriate actions is taken such 
 * closing doors or moving the elevator
 */
public class Elevator implements Runnable{
	
	private DatagramSocket receiveSocket;
	private int currentfloor;
	private int destinationfloor;
	private boolean stationary;
	private int portNumber;
	private boolean doorsOpen;   //false = door closed 
	private Thread motorThread;
	private Thread messageThread;
	private LinkedList<Integer>pendingDestinations;
	private int direction; // 1 is up -1 is down

	
	/**
	 * Constructor for an Elevator
	 * @param portNumber The Elevator 
	 *  instance's portNumber on which to receive messages 
	 */
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
	
	/**
	 * A synchronized method that 
	 * returns the currentFloor an
	 * elevator instance currently on
	 * @return the currentfloor field
	 */
	public synchronized int  getCurrentFloor()
	{
		return this.currentfloor;
	}
	
	/**
	 * A synchronized access to the 
	 * destinationFloor field of an elevator
	 * instance
	 * @return the destinationfloor
	 * field representing which floor
	 * the elevator is currently headed to
	 */
	public synchronized int getDestFloor()
	{
		return this.destinationfloor;
	}
	
	/**
	 * Syncrhonized setter for the destinationFloor field
	 * @param floor the floor to which the elevator should
	 * move
	 */
	public synchronized void setDestFloor(int floor)
	{
		this.destinationfloor = floor;
	}
	
	/**
	 * Synchronized setter for the currentFloor field
	 * @param floor the floor the elevator is currently on
	 */
	public synchronized void setCurrentFloor(int floor)
	{
		this.currentfloor = floor;
	}
	
	/**
	 * Synchronizes the setting of the 
	 * direction field of an elevator
	 * instance
	 */
	public synchronized void setDirection()
	{
		this.direction = (this.destinationfloor - this.currentfloor)/
				Math.abs(this.destinationfloor - this.currentfloor);
	}
	
	/**
	 * Gets the value of the direction field 
	 * @return 1 if current direction is up 
	 * and -1 if down
	 */
	public synchronized int getDirection()
	{
		return this.direction;
	}
	
	/**
	 * Checks if there are pending destination 
	 * the elevator instance still has to visit
	 * @return the floor number at the top of 
	 * the pending destinations list
	 */
	private synchronized int peekPending()
	{
		return this.pendingDestinations.peekFirst();
	}
	
	/**
	 * removes and return the head of the 
	 * pending destination list
	 * @return the floor number at the head
	 * of the pending destination list
	 */
	private synchronized int getPendingDest()
	{
		return this.pendingDestinations.removeFirst();
	}
	
	/**
	 * Adds a floor number to the list of pending
	 * destinations the elevator instance as to visit
	 * @param destination The new floor number to add
	 */
	private synchronized void addPendingDest(int destination)
	{
		if(this.pendingDestinations.isEmpty())
		{
			this.pendingDestinations.add(destination);
			return;
		}
		
		this.pendingDestinations.add(destination);
		
		//if elevator going up then floors sorted in ascending order
		if(this.getDirection() == 1)
		{
			Collections.sort(this.pendingDestinations);
		}
		//elevator going down then floors sorted in descending order
		else
		{
			Collections.sort(this.pendingDestinations, Collections.reverseOrder());
		}
		
	}
	
	/**
	 * Checks if there are any destinations in 
	 * the pending destinations list of an 
	 * elevator instance
	 * @return 0 if no pending destinations and 1
	 * if there is.
	 */
	private synchronized int anyPendingDest()
	{
		if(this.pendingDestinations.isEmpty())
		{
			return 0;
		}
		
		return 1;
	}
	
	/**
	 * The messageThread runs continuously listening for
	 * messages sent to this elevator instance.
	 */
	public void forever()
	{	
		//don't know how we want to this yet
		//maybe create a schedulerElevator instance 
		//fill in the required of port number convert it to bytes
		//or looks like just port number might be enough for registration
		while(true)
		{
			if(Thread.currentThread().isInterrupted())
			{
				if(this.motorThread == null)
				{
					return;
				}
				try {
					this.motorThread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			}
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
	
	/**
	 * Method executed by initial thread assigned to an
	 * instance of this runnable. It registers the instance
	 * with the scheduler and then starts another Thread 
	 * to listen for messages on the instances portNumber
	 */
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
	
	/**
	 * The run method of this runnable in which all threads
	 * start
	 */
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
	
	/**
	 * Mimics the movement of an elevator between floors
	 * @return True if there are pending destinations still to
	 * visit. False if no pending destinations
	 */
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
	
	/**
	 * Controls the movement of an elevator instance
	 * 
	 */
	public void handleMovement()
	{
		while(mimicMovement())
		{
			this.setDestFloor(this.getPendingDest());
			this.setDirection();
		}
	}
	
	public static void main(String[] args)
	{
		Elevator e = new Elevator(70);
		e.start();
	}
}