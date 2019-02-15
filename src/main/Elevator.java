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
	private boolean firstRequest, doorsOpen;   //false = door closed ;
	private int sensorCount, //sensorCount how many times to do 8s notification
	                    specialCase; //special movement with no 8s notification
	                                     
	
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
		this.firstRequest = true;
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
		while(true)
		{
			if(Thread.currentThread().isInterrupted())
			{
				return;
			}
			byte data[] = new byte[100];
		    DatagramPacket receivePckt = new DatagramPacket(data, data.length);
		    // Block until a datagram packet is received from receiveSocket.
	        try {
	        		System.out.printf("Elevator waiting for movement request\n");
	        		this.receiveSckt.receive(receivePckt);	
	        } catch(IOException e) {
	        	//System.out.print("IO Exception: likely:");
	            //e.printStackTrace();
	            break;
	        }
	        
	        switch(data[0])
	        {
	        case 1:
	        	System.out.println("Registration confirmed");
	        	handleRegistrationConfirmed(data);
	        	break;
	        
	        case 2:
	        	handleDoorClose();
	        	break;
	        
	        case 4:
	        	//Message received format: [4 - floor - carButton - direction(1 is up -1 is down)
				System.out.println("Received request with contents:");
				System.out.println(Arrays.toString(data));
				handleRequest(data);
				break;
			
	        case 6:
	        	System.out.println("Got move");
				this.motorThread = new Thread(this, "motorThread");
				this.sensorThread = new Thread(this, "sensorThread");
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
	        	
			default:
				System.out.println("Got unrecognized message");
	        }
	        /*
	        //registration confirmed
			if(data[0] == (byte)1)
			{
				handleRegistrationConfirmed(data);
			}
			
			else if(data[0] == (byte)2) //closedoor
			{
				//close door and send door closed
					 
			}
			//floor request
			else if(data[0]==(byte)4) 
			{
				//Message received format: [4 - floor - carButton - direction(1 is up -1 is down)
				System.out.println("Received request with contents:");
				System.out.println(Arrays.toString(data));
				handleRequest(data);
			}
			//move
			else if(data[0] == 6)
			{
				System.out.println("Got move");
				this.motorThread = new Thread(this, "motorThread");
				this.sensorThread = new Thread(this, "sensorThread");
				this.sensorThread.start();
				this.motorThread.start();
				
			}
			else if(data[0] == 8)
			{
				//just stop motorThread and sensor thread
				handleStop();
			}*/

		}
		
	}
	
	/**
	 * 
	 * @param data
	 */
	private void handleRegistrationConfirmed(byte[] data)
	{
		this.assignedSchedulerPort = data[1];
		this.direction = data[2] == 1 ? 1 : -1;
		System.out.println("Registration confirmation received with port: " + this.assignedSchedulerPort
				+ " and direction " + this.direction);
	}
	
	/**
	 * 
	 */
	private void handleStop()
	{
		this.sensorThread.interrupt();
		this.motorThread.interrupt();
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
	 * 
	 */
	private void handleDoorClose()
	{
		this.doorsOpen = false;
		System.out.println("Closing doors");
		byte[] doorClosedMsg = new byte[] {3};
		try {
			System.out.println("Sending door close message. ");
			DatagramPacket doorClosedPckt = new DatagramPacket(doorClosedMsg, 
					doorClosedMsg.length, InetAddress.getLocalHost(),this.assignedSchedulerPort);
			DatagramSocket doorClosedMsgSckt = new DatagramSocket();
			doorClosedMsgSckt.send(doorClosedPckt);
			doorClosedMsgSckt.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void updateDestination()
	{
		this.destinationFloor = this.pendingDestinations.removeFirst();
		this.setDirection();
		this.sensorCount = Math.abs(this.currentFloor - this.destinationFloor);
	}
	
	/**
	 * 
	 * @param data
	 */
	private void handleRequest(byte[] data)
	{
		//Message received format: [0 - floor - carButton - direction(1 is up -1 is down)]
		//System.out.println("Got request with " + Arrays.toString(data));
		if(this.currentFloor != data[1] || this.firstRequest) 
		{
			this.destinationFloor = data[1];
			this.addPendingDest(data[2]);
			this.firstRequest = false;
		}
		
		else if(direction == 1)
		{
			this.addPendingDest(this.destinationFloor < data[2] ? data[2] : this.destinationFloor);
			this.destinationFloor = this.destinationFloor < data[2] ? this.destinationFloor : data[2];	
		}
		else if(direction == -1)
		{
			this.addPendingDest(this.destinationFloor < data[2] ? this.destinationFloor : data[2]);
			this.destinationFloor = this.destinationFloor < data[2] ? data[2] : this.destinationFloor;
		}
		
		this.setDirection();
		this.sensorCount = Math.abs(this.currentFloor - this.destinationFloor);
		this.specialCase = this.direction == data[3] ? this.specialCase : (this.specialCase | 0x00000001);
		sendReady();
	}
	
	/**
	 * 
	 */
	private void sendReady()
	{
		byte[] elevatorReadyMsg = new byte[] {5, (byte)this.currentFloor,
				(byte)this.destinationFloor, (byte)this.direction};
		
		try {
			DatagramPacket elevatorReadyPckt = new DatagramPacket(elevatorReadyMsg, elevatorReadyMsg.length
					,InetAddress.getLocalHost(), this.assignedSchedulerPort);
			DatagramSocket elevatorReadySckt = new DatagramSocket();
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
	 * 
	 */
	public void sendSensorMsg()
	{
		if(Thread.currentThread().isInterrupted())
		{
			return;
		}
		this.currentFloor = this.direction == 1 ? (this.currentFloor + 1) : (this.currentFloor - 1);
		byte[] sensorMsg = new byte[] {7, (byte)this.direction, (byte)this.specialCase,
				(byte)this.currentFloor};
		try {	
			DatagramPacket sensorPckt = new DatagramPacket(sensorMsg, sensorMsg.length,
					InetAddress.getLocalHost(), this.assignedSchedulerPort);
			DatagramSocket sensorSckt = new DatagramSocket();
			sensorSckt.send(sensorPckt);
			sensorSckt.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 
	 */
	public void sensorFunction()
	{
		System.out.println("SensorThread");
		while(this.sensorCount > 0)
		{
			System.out.println("SensorCount: " + this.sensorCount);
			try {
				Thread.sleep(2000);
				sendSensorMsg();
				this.sensorCount --;
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Interrupted sensor");
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
		//here function should calculate the number of floors it needs to move 
		int floorDiff = Math.abs(this.currentFloor - this.destinationFloor);
		System.out.println("floordiff is " + floorDiff);
		//multiply the avg 10000milliseconds to 1 floor by the number of floors 
		try {
			System.out.println("Elevator moving");
			Thread.sleep(floorDiff*3000);
			//if thread wakes up on its own then it got to the final destination 
			//that was updated when the move request was received
			//so we update current floor as that floor
			//send packet to scheduler elevator has arrived
			int anyPendingDest = this.pendingDestinations.isEmpty() ? 0 : 1;
			int destination = anyPendingDest == 1 ? this.pendingDestinations.peekFirst() : -1;
			byte[] arrivalMessage = new byte[5];   //byte 5 is used to represent arrival to destination
			arrivalMessage[0] = 10;
			arrivalMessage[1] = (byte)anyPendingDest;
			arrivalMessage[2] = (byte)destination;
			arrivalMessage[3] = (byte)this.direction;
			
			DatagramPacket arrivalMsgPkt = new DatagramPacket(arrivalMessage, arrivalMessage.length,
					InetAddress.getLocalHost(),this.assignedSchedulerPort);
			DatagramSocket arrivalMsgSocket = new DatagramSocket();
			arrivalMsgSocket.send(arrivalMsgPkt);
			arrivalMsgSocket.close();
			
			this.specialCase = this.specialCase & 0x00000000;
			this.doorsOpen = true;
			System.out.printf("Elevator got to destination floor: %d and doors open\n",destinationFloor);
			
			
		} catch (IOException e) {
			
			//some better handling here(later iteration)
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			//Throwable  s = e.getCause();
			//System.out.println(s);
			System.out.printf("Elevator stopped at floor %d to answer request\n",currentFloor);			
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
			System.out.println("Exiting");
		} else if (Thread.currentThread().getName().equals("motorThread")) {
			this.mimicMovement();
		}
		else if(Thread.currentThread().getName().equals("sensorThread"))
		{
			this.sensorFunction();
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
	
	public int getDirection() {
		return this.direction;
	}
	public int getCurrentFloor() {
		return this.currentFloor;
	}
	public int getDestinationFloor() {
		return this.destinationFloor;
	}
	public int getPort() {
		return this.portNumber;
	}
	public int getAssignedPort() {
		return this.assignedSchedulerPort;
	}
	public boolean getDoorsOpen() {
		return this.doorsOpen;
	}
	public boolean isMotorInterrupted() {
        return this.motorThread.isInterrupted();   //returns true is motorThread is interrupted
    }
	public boolean isSensorInterrupted() {
        return this.sensorThread.isInterrupted();   //returns true is sensorThread is interrupted
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
	}
}