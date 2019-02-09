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
	//private Thread elevator1Thread, elevator2Thread, elevator3Thread, elevator4Thread;
	private DatagramSocket receiveElevatorSocket;
	private DatagramSocket receiveFloorSocket;
	private ArrayList<SchedulerElevators> elevators;
	private ArrayList<FloorRequest>upRequests;
	private ArrayList<FloorRequest>downRequests;
	
	private boolean requestExists;
	
	/**
	 * Constructor for the Scheduler
	 * Creates 2 threads: one to listen/send
	 * messages/commands to and from floor, and the other 
	 * to listen/send messages to and from elevator
	 */
	
	public Scheduler()
	{
		this.floorMsgThread = new Thread(this,"floorThread");
		this.elevatorMsgThread = new Thread(this, "elevatorThread"); 
		this.elevators = new ArrayList<>();
		this.upRequests = new ArrayList<>();
		this.downRequests = new ArrayList<>();
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
	 * A synchronized method that 
	 * is used to ensure the scheduler waits until
	 * an elevator is registered
	 */
	
	private synchronized void isEmpty()
	{
		if(this.elevators.isEmpty())
		{
			try {
				wait();
				//not sure how is wait interrupted yet
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//if not empty do nothing and just allow for continued 
		//execution
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
			
			//think for register elevator 
			//jsut need to send portNumber and 
			//the msg[0] byte signifying register elevator msg
			if((byte)elevatorMsg[0] == (byte)0)
			{
				//register elevator 
				//SchedulerElevators elevator = new SchedulerElevators();
				//elevator.currentFloor = (int)elevatorMsg[1];
				//elevator.destinationFloor = (int)elevatorMsg[2];
				//elevator.isStationary = ((int)elevatorMsg[3]);
				//elevator.portNumber = (int)elevatorMsg[4];
				//this.addElevator(elevator);
				
				// Thread somethread = new Thread(new SchedulerElevators(
				//send response back to elevator telling it new port number
				//assigned to it.
				//have ArrayList<Integer>
				//add(our new portnumber)
				//when

				ArrayList<Integer> assignedPorts;
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
				
			}
			
			//else if door closed  
			else if((byte)elevatorMsg[0] == (byte)2) 
			{
				
				if(elevatorMsg[1] == 0 && !isFloorEmpty(0))  { //parameter for isFloorEmpty will be floor of currentfloor update
					                                           //sent by elevator every 8 seconds
				
					System.out.println("Received doors closed message from elevator and has no pending dest");
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					buffer.write((byte) 0);
					FloorRequest request = requests.remove(0);    //need to change this to remove current floor index instead of
					buffer.write((byte) request.floor);
					buffer.write((byte) request.carButton);
					
					byte[] data = buffer.toByteArray();
					System.out.println("Sent following to elevator: " + Arrays.toString(data));
					
					try
					{
						DatagramSocket sendElevatorMove = new DatagramSocket();
						SchedulerElevators selectedElevator = this.elevators.get(0);
						DatagramPacket elevatorPckt = 
								new DatagramPacket(data,data.length,
										InetAddress.getLocalHost(),selectedElevator.portNumber);
						//DatagramPacket floorPckt = new DatagramPacket(floorData,floorData.length,packet.getAddress(),packet.getPort());
						sendElevatorMove.send(elevatorPckt);
						System.out.println("Sent movement");
						sendElevatorMove.close();
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
			//else if destination arrival message
			else if((byte)elevatorMsg[0] == (byte)5) {
				System.out.println("Received destination arrival message.");
				System.out.println(elevatorMsg[1]);
				if(elevatorMsg[1]==0 && isRequestEmpty()) {              
					System.out.println("Elevator stopped finally");
				}
				else
				{
					System.out.println("Elevator got to floor " + elevatorMsg[3] + " sending close door");
					closeDoor();
				}
			}
				
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
				//removing first 0 byte from received packet
				byte[] actualMsg = Arrays.copyOfRange(floorMsg, 1, packet.getLength());
				
				isEmpty();
				//adding new request to front of requests linked list
				FloorRequest r = (FloorRequest) FloorRequest.getObjectFromBytes(actualMsg);
				this.addRequest(r.floor,r);

				SchedulerElevators elevator = this.elevators.get(0);
				if(this.elevators.get(0).isStationary == 1)
				{
					//send close door
					this.elevators.get(0).isStationary = 0;
					closeDoor();
				}
			}
			 
				
		}
			/*
			//receiving elevator approaching next floor message every 8 seconds from floor
			else if(floorMsg[0] == (byte)4)
			{
				SchedulerElevators elevator = this.elevators.get(0);
				System.out.println("Received elevator approaching floor from floor");
				elevator.currentFloor = elevator.currentFloor+1;
				
				//check list for request at that floor
				FloorRequest re  = new FloorRequest();
				if(requests.get(elevator.currentFloor).timestamp != null) {
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					buffer.write((byte) 1);
					buffer.write((byte) requests.get(elevator.currentFloor).floor);
					
					byte[] stopData = buffer.toByteArray();
					
					ByteArrayOutputStream request = new ByteArrayOutputStream();
					request.write((byte) 0);
					request.write((byte) requests.get(elevator.currentFloor).floor);
					request.write((byte) requests.get(elevator.currentFloor).carButton);
					//request.write((byte) requests.get(elevator.currentfloor).carButton);
					
					byte[] newRequest = request.toByteArray();
					
					try {
						DatagramSocket sendInterrupt = new DatagramSocket();
						SchedulerElevators selectedElevator = this.elevators.get(0);
						DatagramPacket elevatorStopPckt = 
								new DatagramPacket(stopData,stopData.length,
										InetAddress.getLocalHost(),selectedElevator.portNumber);
						DatagramPacket newRequestPckt = 
								new DatagramPacket(newRequest,newRequest.length,
										InetAddress.getLocalHost(),selectedElevator.portNumber);
						sendInterrupt.send(elevatorStopPckt);
						System.out.println("Sent stop");
						sendInterrupt.send(newRequestPckt);
						System.out.println("Sent movement");
						sendInterrupt.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				else {
					System.out.println("No requests found on next floor. Elevator can keep moving.");
					byte[] floorData = new  byte[] {3};
					DatagramPacket floorPckt = new DatagramPacket(floorData,floorData.length,
							packet.getAddress(),packet.getPort());
					try {
						DatagramSocket noInterrupt = new DatagramSocket();
						noInterrupt.send(floorPckt);
						noInterrupt.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
				}
				
				
			}
			*/

	}
	
	/**
	 * Synchronized method that returns a boolean
	 * to denote if the floor request array list is 
	 * empty or not
	 */
	public synchronized boolean isFloorEmpty(int index)
	{
		if(requests.get(index).timestamp==null) {
			return true;
		}
		return false;
	}
	
	public synchronized boolean isRequestEmpty() {
		return requests.isEmpty();
	}
	/**
	 * Synchronized method that synchronizes the adding
	 * of floor requests to the array list
	 * @param index the index at which the request is to be
	 * added onto the list
	 * @param r the floor request received
	 */
	
	public synchronized void addRequest(int index, FloorRequest r)
	{
		requests.set(index,r);
	}
	
	/**
	 * Method that sends a door close message each
	 * time before sending a new move request
	 */
	
	public void closeDoor() {
		
		System.out.println("Sending door close message. ");
		byte[] doorCloseMsg = new byte[] {2};
		try {
			DatagramSocket sendDoorClose = new DatagramSocket();
			SchedulerElevators selectedElevator = this.elevators.get(0);
			DatagramPacket doorClosePkt = new DatagramPacket(doorCloseMsg, doorCloseMsg.length, InetAddress.getLocalHost(),selectedElevator.portNumber);
			sendDoorClose.send(doorClosePkt);
			System.out.println("Sent door close message. ");
			sendDoorClose.close();
		} catch (IOException e) {
			e.printStackTrace();
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
	public synchronized void addElevator(SchedulerElevators elevator)
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

	    Scheduler s = new Scheduler();
	    s.start();
	    
	}
		
	
}