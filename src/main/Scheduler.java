package main;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;



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
			System.out.println(Arrays.toString(elevatorMsg));
			
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
				this.addElevator(elevator);
			}
			
			//else if door closed  
			else if((byte)elevatorMsg[0] == (byte)2) 
			{
				if(elevatorMsg[1] == 0 && !isRequestEmpty())
				{
					System.out.println("Received doors closed message from elevator and has no pending dest");
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					buffer.write((byte) 0);
					FloorRequest request = requests.remove(0);
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
				this.addRequest(0,r);
				
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
	
	public synchronized boolean isRequestEmpty()
	{
		return requests.isEmpty();
	}
	public synchronized void addRequest(int index, FloorRequest r)
	{
		requests.add(r);
	}
	
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
	
	public void start()
	{
		this.floorMsgThread.start();
		this.elevatorMsgThread.start();
	}
	
	public synchronized void addElevator(SchedulerElevators elevator)
	{
		this.elevators.add(elevator);
		notifyAll();
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
			this.closeDoor();
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