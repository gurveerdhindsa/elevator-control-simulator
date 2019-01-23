package main;
import java.io.IOException;
import java.net.*;
import java.util.*;



public class Scheduler {
	
	private ArrayList<Elevator> elevators;
	private DatagramSocket receiveSocket;
	private DatagramPacket receivePacketFloor;
	private static HashMap<Elevator, Integer> eleMap;
	private Elevator ele;
	public Scheduler()
	{
		eleMap = new HashMap<Elevator, Integer>();
		elevators = new ArrayList<>();
	}

	
	public void addElevator(Elevator car)
	{
		this.elevators.add(car);
	}
	// This code Just be where initalize the number of elevators 
	public boolean availablePort(int port) {
		try {
			DatagramSocket ds = new DatagramSocket(port);
			return true;
		}
		catch(IOException unavailable) {
			return false;
		}
	}
	
	public Elevator iterateElevators() throws SocketException {
		
		for (Elevator ele : eleMap.keySet()) {
				if (availablePort(eleMap.get(ele))) {
					return ele;
				}
				else {
					System.out.print("Port Unavilable");
				}
		}
		return null; // No elevators unavailable!
		
	}
	
	
	public void schedule()
	{
		
		receiveSocket.receive(receivePacketFloor); 	
		byte[] msg = new byte[]{ 0, 4, 5, 6,7};
		
		try {
			ele = iterateElevators();
			DatagramPacket packet = new DatagramPacket(msg,msg.length,InetAddress.getLocalHost(),eleMap.get(ele));
			DatagramSocket sendSocket = new DatagramSocket();
			sendSocket.send(packet);
			sendSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		pause();
		System.out.println("Pause done");
		
		Elevator f = this.elevators.get(0);
		boolean g = f.isInterrupted();
		Thread.State state = f.getState();
		f.interrupt();
	}
	
	//just checking
	static void pause(){
	    long Time0 = System.currentTimeMillis();
	    long Time1;
	    long runTime = 0;
	    while(runTime<9000){
	        Time1 = System.currentTimeMillis();
	        runTime = Time1 - Time0;
	    }
	}
	
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
	public static void main(String[] args) {
		
		Elevator carA = new Elevator(23);
		eleMap.put(carA, 6000);
		
		carA.start();
	    Scheduler s = new Scheduler();
	    s.addElevator(carA);
	    s.schedule();
	    
	}
		
	
}