package main;
import java.io.IOException;
import java.net.*;
import java.util.*;



public class Scheduler implements Runnable{
	
	private Thread floorMsgThread;
	private Thread elevatorMsgThread;
	private DatagramSocket receiveElevatorSocket;
	private DatagramSocket receiveFloorSocket;
	private ArrayList<SchedulerElevators> elevators ;
	
	public Scheduler()
	{
		this.floorMsgThread = new Thread(this,"floorThread");
		this.elevatorMsgThread = new Thread(this, "elevatorThread");
		this.elevators = new ArrayList<>();
		try {
			this.receiveElevatorSocket = new DatagramSocket(69);
			this.receiveFloorSocket = new DatagramSocket(45);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
			
			//else if handle other messages and on 
				
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
				
				//check if any elevators available
				/*if(this.elevators.isEmpty())
				{
					//add floor request to queue of waiting request
				}*/
				
				while(this.elevators.isEmpty())
				{
					//do nothing
				}
				//simulating move command to floor 5
				//and notifying floor that elevator is moving
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
	/*public void schedule()
	{
		byte[] msg = new byte[]{ 0, 4, 5, 6,7};
		
		System.out.println("Sending move request");
		try {
			DatagramPacket packet = new DatagramPacket(msg,msg.length,InetAddress.getLocalHost(),23);
			DatagramSocket sendSocket = new DatagramSocket();
			sendSocket.send(packet);
			sendSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		pause();
		
		System.out.println("Paused for 9s");
		byte[] secMsg = new byte[] {1,3,4,6};
		try
		{
			DatagramPacket pckt =  new DatagramPacket(secMsg, secMsg.length, InetAddress.getLocalHost(),23);
			DatagramSocket sendSocket = new DatagramSocket();
			sendSocket.send(pckt);
			System.out.println("Sent stop rquest");
			sendSocket.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

	}*/
	
	//just checking
	
	
	/*public void sendReceive() {
		for (;;) {
			
			//receive button request from floor (up/down)
			System.out.println("Scheduler - Waiting for Packet from Elevator....");
			try {
				receiveSocket.receive(receivePacketFloor); 
				System.out.println("Scheduler - Packet Received from Elevator");
				printPacketDetails(receivePacketFloor);
				
				//send packet to Elevator
				sendPacketElevator = new DatagramPacket(receivePacketFloor.getData(), receivePacketFloor.getLength(),receivePacketFloor.getAddress(), sendPort);
				sendSocket.send(sendPacketElevator);
				System.out.println("Scheduler - Packet Sent to Elevator");
				
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			//send packet to Elevator
			
			try {
				byte dataReceived[] = new byte[100];
				receivePacketElevator = new DatagramPacket(dataReceived, dataReceived.length);
				sendSocket.receive(receivePacketElevator); // packet received from the server
				System.out.println("Scheduler- Packet Received from Elevator");
				printPacketDetails(receivePacketElevator);
				
				sendPacketFloor = new DatagramPacket(receivePacketElevator.getData(),receivePacketElevator.getLength(), receivePacketElevator.getAddress(),receivePacketFloor.getPort());
				printPacketDetails(sendPacketFloor);
				sendSocket.send(sendPacketFloor);
				System.out.println("Scheduler - Packet Sent to Floor");
				
			}
			catch(IOException e1) {
				e1.printStackTrace();
			}
			
			
		}
		
	}

	private void printPacketDetails(DatagramPacket packet) {
		System.out.println();
		System.out.println("The data inside the packet (String): " + new String(packet.getData(), 0, packet.getLength()));
		System.out.println("The length of the packet: " + packet.getLength());
		System.out.println("Address of packet: " + packet.getAddress());
		System.out.println("The destination port is: " + packet.getPort());
		System.out.println();
		
	}*/
	
	@Override
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
		
	}
	public static void main(String[] args) {

	    Scheduler s = new Scheduler();
	    s.start();
	    
	}
		
	
}