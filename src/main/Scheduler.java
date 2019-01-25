package main;
import java.io.IOException;
import java.net.*;
import java.util.*;



public class Scheduler implements Runnable{
	
	private Thread floorMsgThread, elevatorMsgThread;
	private DatagramSocket receiveElevatorSocket;
	private DatagramSocket receiveFloorSocket;
	private ArrayList<SchedulerElevators> elevators;
	private ArrayList<FloorRequest>requests;
	
	public Scheduler()
	{
		this.floorMsgThread = new Thread(this,"floorThread");
		this.elevatorMsgThread = new Thread(this, "elevatorThread");
		this.elevators = new ArrayList<>();
		this.requests = new ArrayList<>();
		try {
			this.receiveElevatorSocket = new DatagramSocket(69);
			this.receiveFloorSocket = new DatagramSocket(45);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
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
			
			System.out.println("Elevator message received");
			
			
			//think for register elevator 
			//jsut need to send portNumber and 
			//the msg[0] byte signifying register elevator msg
			if((byte)elevatorMsg[0] == (byte)0)
			{
				//register elevator 
				SchedulerElevators elevator = new SchedulerElevators();
				elevator.currentFloor = (int)elevatorMsg[1];
				elevator.destinationFloor = (int)elevatorMsg[2];
				elevator.isStationary = ((int)elevatorMsg[3]);
				elevator.portNumber = (int)elevatorMsg[4];
				this.elevators.add(elevator);
				
			}
			
			//else if door closed  
				
		}
	}
	
	public void listenForFloorMsg()
	{
		while(true)
		{
			//listen for elevator message
			byte []floorMsg = new byte[100];
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
				
				//call to isEmpty
				//checks if any elevators available
				//if none blocks the listen to messages
				//from floor thread till an elevator is 
				//registered
				isEmpty();

				//[Should have two arraylists for up and down requests for now]
				//single elevator for now so definitely selecting 
				//elevator at index 0, if elevator is moving 
				//check elevators current floor & direction, if 
				//same direction as request and difference between
				// currentFloor and requestFloor > 1 
				//else if stationary start new thread and name it 
				//closeDoor(easy because of single elevator, otherwise
				//don't know which elevator door to close)
				
				//(probably create a processing thread that should handle 
				//this. Basically have a linkedlist for now since 		
				//doing FIFO. if when processing thread is started 
				//linked list empty, it should get stuck on wait.
				//once this thread puts into the linked list
				//it should notifyall. [Another two new sync methods 
				// isRequestEmpty and addRequest that adds to linked
				//list or checks if empty]. if there are requests 
				//to handle get our single elevator.....
				//my brain got lost here so need help with this one 
				//as listenFloorMsg can't have too much processing 
				//otherwise would miss the floor arrival notification from
				//floor)
				System.out.println("Sending move command to elevator and notifying floor");
				byte[] data = new byte[] {0,5};
				byte[] floorData = new  byte[] {2};
				
				try {
					DatagramSocket sendElevatorMove = new DatagramSocket();
					SchedulerElevators selectedElevator = this.elevators.get(0);
					DatagramPacket elevatorPckt = 
							new DatagramPacket(data,data.length,
									InetAddress.getLocalHost(),selectedElevator.portNumber);
					DatagramPacket floorPckt = new DatagramPacket(floorData,floorData.length,
							packet.getAddress(),packet.getPort());
					sendElevatorMove.send(elevatorPckt);
					sendElevatorMove.send(floorPckt);
					System.out.println("Sent movement");
					sendElevatorMove.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			else if(floorMsg[0] == (byte)3)
			{
				System.out.println("Received elevator approaching floor from floor");
				//process if you need to stop elevator at this floor its approaching
			}
		}
	}
	
	public void start()
	{
		this.floorMsgThread.start();
		this.elevatorMsgThread.start();
	}
				

	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		if(Thread.currentThread().getName().equals("floorThread"))
		{
			this.listenForFloorMsg();
		}
		else if(Thread.currentThread().getName().equals("closeDoor"))
		{
			//createCloseDoor function to send close door 
			//to our one elevator
		}
		else
		{
			this.listenForElevatorMsg();
		}
		
	}
	public static void main(String[] args) {

	    Scheduler s = new Scheduler();
	    s.start();
	    
	}
		
	
}