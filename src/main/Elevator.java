// Elevator.java
package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
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
	
	private DatagramSocket receiveSckt;
	private Thread motorThread, sensorThread, messageThread;
	private LinkedList<Integer>pendingDestinations;
	private int assignedSchedulerPort, currentFloor, destinationFloor, portNumber,
	            direction; // 1 is up -1 is down
	private boolean doorsOpen;   //false = door closed ;
	private int sensorCount, //sensorCount how many times to do 8s notification
	                    specialCase; //special movement with no 8s notification
	private long currentFloorTime, currentDoorTime;
	private Thread closeDoor;
	                                     
	
	/**
	 * Constructor for an Elevator
	 * @param portNumber The Elevator 
	 *  instance's portNumber on which to receive messages 
	 */
	public Elevator(int portNumber)
	{
		this.portNumber = portNumber;
		this.currentFloor = 0;
		this.destinationFloor = 0;
		this.messageThread = new Thread(this, "messageThread");
		this.pendingDestinations = new LinkedList<>();
		this.doorsOpen = true;
		this.sensorCount = 0;
	
		try {
			receiveSckt = new DatagramSocket(portNumber);
		}catch (SocketException e)
		{
			e.printStackTrace();
			System.out.println("Elevator not created");
		}
	}
	
	/**
	 * Synchronizes the setting of the 
	 * direction field of an elevator
	 * instance
	 */
	public void setDirection()
	{
		this.direction = (this.destinationFloor - this.currentFloor)/
				Math.abs(this.destinationFloor - this.currentFloor);
	}
	
	/**
	 * Adds a floor number to the list of pending
	 * destinations the elevator instance as to visit
	 * @param destination The new floor number to add
	 */
	private void addPendingDest(int destination)
	{
		if(this.pendingDestinations.contains(destination))
		{
			return;
		}
		
		this.pendingDestinations.add(destination);
		//if elevator going up then floors sorted in ascending order
		if(this.direction == 1)
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
	 * The messageThread runs continuously listening for
	 * messages sent to this elevator instance.
	 */
	private void processorFunction()
	{	
		boolean breakOut = false;
		while(true)
		{
			if(breakOut)
			{
				break;
			}
			byte data[] = new byte[100];
		    DatagramPacket receivePckt = new DatagramPacket(data, data.length);
		    // Block until a datagram packet is received from receiveSocket.
	        try {
	        		this.receiveSckt.receive(receivePckt);	
	        } catch(IOException e) {
	            //e.printStackTrace();
	            break;
	        }
	        
	        switch(data[0])
	        {
	        case 1:
	        	handleRegistrationConfirmed(data);
	        	break;
	        
	        case 2:
	        	this.closeDoor = new Thread(this,"closeDoor");
	        	this.currentDoorTime = data[1];
	        	this.closeDoor.start();
	        	break;
	        
	        case 4:
	        	//Message received format: [4 - floor - carButton - direction(1 is up -1 is down)
				handleRequest(data);
				break;
			
	        case 6: // receives byte 6, elevator can move
				this.motorThread = new Thread(this, "motorThread");
				this.sensorThread = new Thread(this, "sensorThread");
				System.out.println("Elevator with port:" + this.portNumber + " moving from:"
						+ this.currentFloor + " to:" + this.destinationFloor);
				this.sensorThread.start();
				this.motorThread.start();
				break;
			
	        case 8:
	        	//just stop motorThread and sensor thread
				handleStop();
				break;
			
	        case 11: //update pending destination
	        	updateDestination();
	        	sendReady();
	        	break;
	        
	        case 14:
	        	this.motorThread.interrupt();
	        	this.sensorThread.interrupt();
	        	this.closeDoor.interrupt();
	        	breakOut = true;
	        	break;
	        
	        case 15:
	        	this.closeDoor.interrupt();
	        	this.closeDoor = new Thread(this, "closeDoor");
	        	this.currentDoorTime = data[1];
	        	this.closeDoor.start();
	        	break;
	        	
			default:
				System.out.println("Got unrecognized message");
	        }
	        
		}
		
	}
	
	/**
	 * Registering an elevator to a the specified port number , specifying the 
	 * direction of the elevator -1 is down and 1 is up
	 * Third index being the direction key
	 * @param data byte[]
	 */
	private void handleRegistrationConfirmed(byte[] data)
	{
		this.assignedSchedulerPort = data[1];
		this.direction = data[2] == 1 ? 1 : -1;
		System.out.println("Elevator with port:" + this.portNumber
				+ " received registration confirmed msg:[assignedSchedulerPort:" + this.assignedSchedulerPort
				+ ", direction:" + (this.direction == 1 ? "Up" : "Down") + "]");
	}
	
	/**
	 * Sends packet to scheduler to tell the elevator has stopped
	 */
	private void handleStop()
	{
		if(this.sensorThread != null) // interrupt the sensor thread to open doors to service a request
		{
			this.sensorThread.interrupt();// interrupt sensor thread
			this.motorThread.interrupt(); // interrupt elevator
			this.doorsOpen = true; // open doors
		}

		byte[] stoppedMsg = new byte[] {9};
		
		try {
			DatagramPacket stoppedPckt = new DatagramPacket(stoppedMsg, stoppedMsg.length,
					InetAddress.getLocalHost(), this.assignedSchedulerPort);
			DatagramSocket stoppedMsgSckt = new DatagramSocket();
			stoppedMsgSckt.send(stoppedPckt);
			stoppedMsgSckt.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * The scheduler sends byte 2 indicating it to close the elevator doors
	 */
	private void handleDoorClose()
	{
		this.doorsOpen = false;
		byte[] doorClosedMsg = new byte[] {3}; // sends a byte 3 back to the scheduler, letting it know doors are closed
		try {
			
			long start = System.nanoTime();
			long time = 500 + (this.currentDoorTime * 25);
			System.out.println("elevator before sleep sleep time is" + this.currentDoorTime + " and time " + time);
			Thread.sleep(time);
			System.out.println("after sleep " + (System.nanoTime() - start));
			DatagramPacket doorClosedPckt = new DatagramPacket(doorClosedMsg, 
					doorClosedMsg.length, InetAddress.getLocalHost(),this.assignedSchedulerPort);
			DatagramSocket doorClosedMsgSckt = new DatagramSocket();
			doorClosedMsgSckt.send(doorClosedPckt);
			System.out.println("Elevator with port:" + this.portNumber +
					" closed its doors.");
			doorClosedMsgSckt.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("Intrrupted close doors");
		}
	}
	
	/**
	 * Update its new floor number, returns the next floor to be service from linked list
	 * meaning that request has been serviced
	 */
	public void updateDestination()
	{
		this.destinationFloor = this.pendingDestinations.removeFirst();
		System.out.println("destination is " + this.destinationFloor);
		this.setDirection();
		this.sensorCount = Math.abs(this.currentFloor - this.destinationFloor);
	}
	
	/**
	 * A request comes in with the floor number, destination floor and direction
	 * @param data
	 */
	private void handleRequest(byte[] data)
	{
		//Message received format: [0 - floor - carButton - direction(1 is up -1 is down)]
		System.out.print("Elevator with port:" + this.portNumber + 
				" received request with contents:[");
		System.out.println("FloorNum:" + data[1] + ", carButton:" + data[2]
				+ ", direction:" + (data[3] == 1 ? "Up" : "Down") + "]");
		if(this.currentFloor != data[1] ) 
		{
			this.destinationFloor = data[1];
			this.addPendingDest((int)data[2]);
		}
		else if(this.destinationFloor == data[1] || this.destinationFloor == data[2])
		{
			this.destinationFloor = data[2];
		}
		
		else if(direction == 1) // elevator request is in upwards direction
		{
			this.addPendingDest(this.destinationFloor < data[2] ? (int)data[2] : this.destinationFloor); // adds request to the linked list
			this.destinationFloor = this.destinationFloor < data[2] ? this.destinationFloor : data[2];	
		}
		else if(direction == -1) //elevator request in downwards direction
		{
			this.addPendingDest(this.destinationFloor < data[2] ? this.destinationFloor : data[2]);
			this.destinationFloor = this.destinationFloor < data[2] ? (int)data[2] : this.destinationFloor;
		}
		
		this.setDirection();
		this.sensorCount = Math.abs(this.currentFloor - this.destinationFloor); // calculate the difference between the curr floor and destination floor
		this.specialCase = this.direction == data[3] ? this.specialCase : (this.specialCase | 0x00000001);
		this.currentFloorTime = (long)data[4];
		System.out.println("currentfloortime is now " + data[4]);
		sendReady(); // send packet to scheduler ready to move
	}
	
	/**
	 * Sends the request to the scheduler, it is ready to move
	 */
	private void sendReady()
	{
		byte[] elevatorReadyMsg = new byte[] {5, (byte)this.currentFloor,
				(byte)this.destinationFloor, (byte)this.direction};
		
		try {
			DatagramPacket elevatorReadyPckt = new DatagramPacket(elevatorReadyMsg, elevatorReadyMsg.length
					,InetAddress.getLocalHost(), this.assignedSchedulerPort);
			DatagramSocket elevatorReadySckt = new DatagramSocket();
			/*
			System.out.println("Elevator wtih port:" + this.portNumber + " sending ReadyMsg with:["
					+ "currentFloor:" + this.currentFloor + ", destinationFloor:" + this.destinationFloor
					+ ", direction:" + (this.direction == 1 ? "Up" : "Down") + "]");*/
			elevatorReadySckt.send(elevatorReadyPckt);
			elevatorReadySckt.close();
		}catch (IOException e)
		{
			e.printStackTrace();
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
			//later on need elevator to know host of and port of scheduler when being instantiated
			DatagramPacket pck = new DatagramPacket(registerElev, registerElev.length, 
					InetAddress.getLocalHost(),69);
			DatagramSocket soc = new DatagramSocket();
			soc.send(pck);
			soc.close();
			System.out.println("Elevator with port:" + this.portNumber + " sent registration msg:" + 
			Arrays.toString(registerElev));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.messageThread.start();
	}
	
	/**
	 * Sending a packet for every floor that is passed in between the current floor and destination floor
	 * to the scheduler, if the special case is triggered the elevator will not stop to take requests
	 */
	public void sendSensorMsg()
	{
		if(Thread.currentThread().isInterrupted())
		{
			return;
		}
		this.currentFloor = this.direction == 1 ? (this.currentFloor + 1) : (this.currentFloor - 1); // incrementing/decrementing floor based on direction
		byte[] sensorMsg = new byte[] {7, (byte)this.direction, (byte)this.specialCase,
				(byte)this.currentFloor}; // creates packet to 
		try {	
			DatagramPacket sensorPckt = new DatagramPacket(sensorMsg, sensorMsg.length,
					InetAddress.getLocalHost(), this.assignedSchedulerPort);
			DatagramSocket sensorSckt = new DatagramSocket();
			sensorSckt.send(sensorPckt);
			System.out.println("Elevator with port:" + this.portNumber + " sending sensorMsg with["
					+ "approachingFloor:" + this.currentFloor + ", Don't_Stop(Special case):"
					+ this.specialCase + "]");
			sensorSckt.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Iterates for the floors between the current and destination floors
	 * Sleeping the thread for each floor and sending the message to the scheduler 
	 */
	public void sensorFunction()
	{
		
		while(this.sensorCount > 0)
		{
			long time = 2800 + (this.currentFloorTime * 70);
			try {
				Thread.sleep(time); // sleep for two seconds
				sendSensorMsg(); // send packet to let scheduler know
				this.sensorCount --;
				//Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				break;
			}
		}
		
	}
	
	/**
	 * Mimics the movement of an elevator between floors
	 * @return True if there are pending destinations still to
	 * visit. False if no pending destinations
	 */
	private void mimicMovement()
	{
		//calculate the number of floors it needs to move 
		int floorDiff = Math.abs(this.currentFloor - this.destinationFloor);
		try {
			Thread.sleep(floorDiff*3000);
			//if thread wakes up on its own then it got to the destination floor
			int anyPendingDest = this.pendingDestinations.isEmpty() ? 0 : 1;
			int destination = anyPendingDest == 1 ? this.pendingDestinations.peekFirst() : -1;
			byte[] arrivalMessage = new byte[5];
			arrivalMessage[0] = 10;//byte 10 is used to represent arrival msg
			arrivalMessage[1] = (byte)anyPendingDest;// 1 if there is 0 if not
			arrivalMessage[2] = (byte)destination;//-1 if previous index is 0 otherwise floor No.
			arrivalMessage[3] = (byte)this.direction;
			
			DatagramPacket arrivalMsgPkt = new DatagramPacket(arrivalMessage, arrivalMessage.length,
					InetAddress.getLocalHost(),this.assignedSchedulerPort);
			DatagramSocket arrivalMsgSocket = new DatagramSocket();
			arrivalMsgSocket.send(arrivalMsgPkt);
			arrivalMsgSocket.close();
			
			this.specialCase = this.specialCase & 0x00000000;
			this.doorsOpen = true;
			
			System.out.println("Elevator with port:" + this.portNumber + 
					" arrived at floor:" + this.currentFloor + " and opening doors");
			/*
			System.out.println("Elevator with port:" + this.portNumber + 
					" sending ArrivalMsg with:[pendingDestinationListEmpty:" + 
					(anyPendingDest == 1 ? "True" : "False") + ", destinationAtHeadOfList:"
					+ destination + "]");*/
			
		} catch (IOException e) {
			//some better handling here(later iteration)
			//e.printStackTrace();
			return;
		} catch (InterruptedException e) {
			System.out.printf("Elevator stopped at floor %d to answer request\n",this.currentFloor);	
			return;
		}
		
	}
	
	/**
	 * The run method of this runnable in which all threads
	 * start
	 */
	public void run()
	{
		if (Thread.currentThread().getName().equals("messageThread")) {
			this.processorFunction();
			System.out.println("Elevator crashed");
			
		} else if (Thread.currentThread().getName().equals("motorThread")) {
			this.mimicMovement();
		}
		else if(Thread.currentThread().getName().equals("sensorThread"))
		{
			this.sensorFunction();
		}
		else if(Thread.currentThread().getName().equals("closeDoor"))
		{
			this.handleDoorClose();
		}
		else {
			this.start();
		}

	}
	
	public void stop()
	{
		this.messageThread.interrupt();
		this.receiveSckt.close();
	}
	
	/**
	 * Method not to be used outside Unit testing
	 * @return
	 */
	public synchronized int getDirection() {
		return this.direction;
	}
	
	/**
	 * Method not to be used outside unit testing
	 * @return
	 */
	public synchronized int getCurrentFloor() {
		return this.currentFloor;
	}
	
	/**
	 * Method not to be used outside unit testing
	 * @return
	 */
	public synchronized int getDestinationFloor() {
		return this.destinationFloor;
	}
	
	/**
	 * Method not to be used outside unit testing 
	 * @return
	 */
	public synchronized int getPort() {
		return this.portNumber;
	}
	
	/**
	 * Method not to be used outside unit testing
	 * @return
	 */
	public synchronized int getAssignedPort() {
		return this.assignedSchedulerPort;
	}
	
	/**
	 * Method not to be used outside unit testing
	 * @return
	 */
	public synchronized boolean getDoorsOpen() {
		return this.doorsOpen;
	}
	
	/**
	 * Method not to be used outside unit testing
	 * @return
	 */
	public synchronized int getSensorCount()
	{
		return this.sensorCount;
	}
	
	/**
	 * Method not to be used outside unit testing
	 * @return
	 */
	public Boolean isSensorThreadExecuting()
	{
		return this.sensorThread.isAlive();
	}
	
	/**
	 * Method not to be used outside unit testing
	 * @return
	 */
	public Boolean isMotorThreadExecuting()
	{
		return this.motorThread.isAlive();
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		Elevator e = new Elevator(70);
		e.start();
		
		Elevator e1 = new Elevator(71);
		e1.start();
		
		Elevator e2 = new Elevator(72);
		e2.start();
		
		Elevator e3 = new Elevator(73);
		e3.start();
	}
}