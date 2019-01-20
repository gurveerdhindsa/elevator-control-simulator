package main;
import java.io.IOException;
import java.net.*;
import java.util.*;



public class Scheduler {
	private DatagramSocket receiveSocket;
	private DatagramSocket sendSocket;
	private DatagramPacket receivePacketFloor;
	private DatagramPacket sendPacketElevator;
	private DatagramPacket receivePacketElevator;
	private DatagramPacket sendPacketFloor;
	private final int receivePort = 23;
	private final int sendPort = 69;
	public Scheduler() {
		try {
			receiveSocket = new DatagramSocket(receivePort);
			sendSocket = new DatagramSocket(); 
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	
	//Person makes a request for an elevator
	//Closest elevator is brought to the floor
	//The person enters the elevator
	//Makes a request for the floor Number
	//The elevator goes to the floor depending on the floor requests
	public void sendReceive() {
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
				receiveSocket.receive(receivePacketElevator); // packet received from the server
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
		
	}
	public static void main(String[] args) {
		Scheduler proxy = new Scheduler();
		proxy.sendReceive();
	}
		
	
}