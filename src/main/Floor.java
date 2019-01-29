// Floor.java

package main;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.sql.Timestamp;

public class Floor implements Runnable {

	private Thread sendThread,
				   sendReceiveThread;
	private DatagramSocket sendReceiveSocket; 	// Socket to send and receive packets

	private List<FloorRequest> floorRequests;	// List of requests to be made

	
	
	public Floor() {
		sendThread = new Thread(this, "sendThread");
		sendReceiveThread = new Thread(this, "sendReceiveThread");
	
		floorRequests = new ArrayList<FloorRequest>();

		importConfiguration();

		try {
			this.sendReceiveSocket = new DatagramSocket();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void importConfiguration() {
		try (BufferedReader br = new BufferedReader(new FileReader("src/configuration/configuration.txt"))) {
			String input = null;
			Timestamp timestamp = null;
			int floor = 0, carButton = 0;
			String floorButton = null;

			while ((input = br.readLine()) != null) {
				String[] inputFields = input.split(":", 2);
				if (inputFields[0].equals("Time")) {
					try {
						SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss.SSS");
						Date parsedDate = dateFormat.parse(inputFields[1]);
						timestamp = new Timestamp(parsedDate.getTime());
					} catch (ParseException e) {
						e.printStackTrace();
					}
				} else if (inputFields[0].equals("Floor")) {
					floor = Integer.parseInt(inputFields[1].trim());
				} else if (inputFields[0].equals("Car Button")) {
					carButton = Integer.parseInt(inputFields[1].trim());
				} else if (inputFields[0].equals("Floor Button")) {
					floorButton = inputFields[1].trim();
				}
			}

			floorRequests.add(new FloorRequest(timestamp, floor, carButton, floorButton));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void send() {
		//edit to send all requests with random 
		// time in between requests
		DatagramPacket packet;
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			// Initial 0 byte to signify a floor request
			buffer.write((byte) 0);
			// For now, we will just send one floor request
			buffer.write(floorRequests.get(0).getBytes());

			byte[] request = buffer.toByteArray();
			
			packet = new DatagramPacket(request, request.length, InetAddress.getLocalHost(), 45);

			System.out.println(Arrays.toString(request));
			System.out.println(request.length);
			System.out.println("Sending floor request");

			sendReceiveSocket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void receiveElevatorMovement() {
		byte[] waitforMove = new byte[100];

		DatagramPacket movement = new DatagramPacket(waitforMove, waitforMove.length);

		try {
			sendReceiveSocket.receive(movement);
			System.out.println("Receive that elevator is moving to floor");
		} catch (IOException e) {
			e.printStackTrace();
		}

		//for now assumption is only message sent from
		//scheduler to floor is elevator on the move 
		//might need more later so might need 
		//to check first byte of message msg[0]
		//to decide what actions to take 
		
		// wait till elevator almost reaching floor 2
		pause(); // or sleep to be consistent whilst getting rid of function overhead
		         // sleep for 8000 milli seconds and then call stopElevator
		
		
	}

	public void stopElevator() {
		byte[] stopelevator = new byte[] { 3 };

		try {
			DatagramPacket stop = new DatagramPacket(stopelevator, stopelevator.length, InetAddress.getLocalHost(), 45);
			System.out.println("Stopping elevator");
			this.sendReceiveSocket.send(stop);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void pause() {
		long Time0 = System.currentTimeMillis();
		long Time1;
		long runTime = 0;
		while (runTime < 9000) {
			Time1 = System.currentTimeMillis();
			runTime = Time1 - Time0;
		}
	}
	
	public void start()
	{
		this.sendThread.start();
		this.sendReceiveThread.start();
	}
	
	@Override
	public void run() {
		if(Thread.currentThread().getName().equals("sendThread"))
		{
			this.send();
		}
		else
		{
			this.receiveElevatorMovement();
		}
		
		
	}

	/**
	 * Main method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Floor floor = new Floor();
		floor.start();
	}
}