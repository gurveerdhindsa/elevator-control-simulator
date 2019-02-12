package main;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 * Represents the Scheduler Subsystem,
 * which recieves a floor request from the floor
 * subsystem, processes it through a data structure
 * to prioritize requests,
 * and sends messages/commands to the elevators
 */

public class Scheduler implements Runnable{
	
	private Thread floorMsgThread, elevatorMsgThread;
	private DatagramSocket receiveElevatorSocket;
	private DatagramSocket receiveFloorSocket;
	private ArrayList<SchedulerElevators> elevators;
	private ArrayList<FloorRequest>upRequests;
	private ArrayList<FloorRequest>downRequests;
	private ArrayList<Integer>portNumbers;
	private int initialRequestList;
	
	/**
	 * Constructor for the Scheduler
	 * Creates 2 threads: one to listen/send
	 * messages/commands to and from floor, and the other 
	 * to listen/send messages to and from elevator
	 */
	
	public Scheduler(int numberOfElevators, int basePortNumber)
	{
		this.floorMsgThread = new Thread(this,"floorThread");
		this.elevatorMsgThread = new Thread(this, "elevatorThread"); 
		this.elevators = new ArrayList<>();
		this.upRequests = new ArrayList<>();
		this.downRequests = new ArrayList<>();
		this.portNumbers = new ArrayList<>();
		this.initialRequestList = 1;
		for(int i = 0; i < numberOfElevators; i++)
		{
			this.portNumbers.add(basePortNumber + i);
		}
		for (int i=0; i<20; i++) {
		    upRequests.add(i, new FloorRequest());
		}
		for (int i=0; i<20; i++) {
		    downRequests.add(i, new FloorRequest());
		}
		try {
			this.receiveElevatorSocket = new DatagramSocket(69);
			this.receiveFloorSocket = new DatagramSocket(45);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * The elevatorThread runs continuously listening for
	 * messages sent to the scheduler by the elevator 
	 * subsystem such as destination arrival, and sends
	 * messages to the floor and elevator
	 * subsystems according to the messages received
	 */
	
	public void listenForElevatorMsg()
	{
		while(true)
		{
			//listen for elevator message
			byte [] elevatorMsg = new byte[100];
			DatagramPacket packet = new DatagramPacket(elevatorMsg, elevatorMsg.length);
			
			try {
				System.out.println("Waiting for message from elevator");
				this.receiveElevatorSocket.receive(packet);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue; //@TODO get rid of and do better handling
			}
			
			System.out.println("Elevator message received for register elevator");
			System.out.println(Arrays.toString(elevatorMsg));
			
			switch(elevatorMsg[0])
			{
			case 0:
				SchedulerElevators elevator = new SchedulerElevators(this.upRequests,
						this.downRequests, elevatorMsg[4],this.portNumbers.remove(0),this.initialRequestList);
				this.initialRequestList = (this.initialRequestList + 1)%2;
				this.elevators.add(elevator);
				Thread elevThread = new Thread(elevator, "SchedulerElevator: " + this.elevators.size());
				elevThread.start();
				break;
			
			default:
				break;
			}
			
			/*
			if((byte)elevatorMsg[0] == (byte)0)
			{
				//register elevator 
				//
				
				// Thread somethread = new Thread(new SchedulerElevators(
				//send response back to elevator telling it new port number
				//assigned to it.
				//have ArrayList<Integer>
				//add(our new portnumber)
				//when

				ArrayList<Integer> assignedPorts = new;
				for(int p=40; p<45; p++) {
					assignedPorts.add(p);
				}
				
				int elevatorPort = (int)elevatorMsg[4];
				
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				int port = assignedPorts.remove(0);
				int initList;
				if(port%2==0) {
					initList = 1;
				}
				else {
					initList = -1;
				}
				
				Thread newElevator = new Thread(new SchedulerElevators(upRequests, downRequests, elevatorPort, port, initList));
				
			}*/
		}
	}
	
	/**
	 * The floorThread runs continuously listening for
	 * floor requests sent to the scheduler by the floor 
	 * subsystem, and sends messages to the floor and elevator
	 * subsystems according to the messages received
	 */
	
	public void listenForFloorMsg()
	{
		while(true)
		{
			//listen for message
			byte []floorMsg = new byte[300];
			DatagramPacket packet = new DatagramPacket(floorMsg, floorMsg.length);
			
			try {
				System.out.println("Waiting for message from floor");
				this.receiveFloorSocket.receive(packet);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue; //@TODO get rid of and do better handling
			}
			
			System.out.println("Floor message received");
			
			
			//floor request
			//proper code should extract floor number and other info
			if((byte)floorMsg[0] == (byte)0)
			{
				System.out.println("Got request msg");
				//removing first 0 byte from received packet
				byte[] actualMsg = Arrays.copyOfRange(floorMsg, 1, packet.getLength());
				//adding new request to front of requests linked list
				FloorRequest r = (FloorRequest) FloorRequest.getObjectFromBytes(actualMsg);
				System.out.println(r.floorButton);
				if(r.floorButton.equals("up"))
				{
					this.addUpRequest(r);
				}
				else
				{
					this.addDownRequest(r);
				}
			}	 	
		}		
	}
	
	
	public void addDownRequest(FloorRequest request)
	{
		synchronized(this.downRequests)
		{
			System.out.println("adding to down request");
			this.downRequests.set(request.floor,request);
			this.downRequests.notifyAll();
		}
	}
	
	/**
	 * Synchronized method that synchronizes the adding
	 * of floor requests to the array list
	 * @param index the index at which the request is to be
	 * added onto the list
	 * @param r the floor request received
	 */
	
	public void addUpRequest(FloorRequest request)
	{
		synchronized(this.upRequests)
		{
			this.upRequests.set(request.floor,request);
			System.out.println("Added to up list");
			this.upRequests.notifyAll();
		}
	}	
	
	
	/**
	 * Method starts both the thread listening for floor 
	 * messages and the thread listening for elevator messages
	 */
	public void start()
	{
		this.floorMsgThread.start();
		this.elevatorMsgThread.start();
	}
	
	/**
	 * Synchronized method that synchronizes the adding
	 * of elevators to the array list of elevators
	 * @param elevator the elevator to be added to the list
	 */
	public void addElevator(SchedulerElevators elevator)
	{
		this.elevators.add(elevator);
		notifyAll();
	}
	
	/**
	 * The run method of this runnable in which all threads
	 * start
	 */
	public void run() {
		// TODO Auto-generated method stub
		
		if(Thread.currentThread().getName().equals("floorThread"))
		{
			this.listenForFloorMsg();
		}
		else
		{
			this.listenForElevatorMsg();
		}
		/*
		 * else if(Thread.currentThread().getName().equals("elevator1Thread")){
		 *     this.listenForElevatorMsg();
		 * }
		 * else if(Thread.currentThread().getName().equals("elevator2Thread")){
		 *     this.listenForElevatorMsg();
		 * }
		 * else if(Thread.currentThread().getName().equals("elevator3Thread")){
		 *     this.listenForElevatorMsg();
		 * }
		 * else if(Thread.currentThread().getName().equals("elevator4Thread")){
		 *     this.listenForElevatorMsg();
		 * }
		 */
		
	}
	
	/**
	 * Main method
	 * Creates instance of the scheduler and runs it 
	 * @param args
	 */
	public static void main(String[] args) {

	    Scheduler s = new Scheduler(2,46);
	    s.start();
	    
	}
		
	
}