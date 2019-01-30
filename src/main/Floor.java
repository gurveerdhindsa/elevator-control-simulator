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
				String[] inputFields = input.trim().split(":", 2);
				if (inputFields[0].equals("Time")) {
					try {
						SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss.SSS");
						Date parsedDate = dateFormat.parse(inputFields[1]);
						timestamp = new Timestamp(parsedDate.getTime());
					} catch (ParseException e) {
						e.printStackTrace();
					}
				} else if (inputFields[0].equals("Floor")) {
					floor = Integer.parseInt(inputFields[1]);
				} else if (inputFields[0].equals("Car Button")) {
					carButton = Integer.parseInt(inputFields[1]);
				} else if (inputFields[0].equals("Floor Button")) {
					floorButton = inputFields[1].trim();
				} else if (input.isEmpty() && timestamp != null && !floorButton.isEmpty()) {
					floorRequests.add(new FloorRequest(timestamp, floor, carButton, floorButton));
				}
			}
			if (timestamp != null) {
				floorRequests.add(new FloorRequest(timestamp, floor, carButton, floorButton));
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void send() {
		//edit to send all requests with random 
		// time in between requests
		DatagramPacket packet;
		try {
			System.out.println(floorRequests.size());
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			
			for (FloorRequest floorRequest : floorRequests) {
				// Initial 0 byte to signify a floor request
				buffer.write((byte) 0);
				// For now, we will just send one floor request
				buffer.write(floorRequests.get(0).getBytes());
				byte[] request = buffer.toByteArray();
				// Create datagram packet to send
				packet = new DatagramPacket(request, request.length, InetAddress.getLocalHost(), 45);
				System.out.println("Sending floor request...");
				sendReceiveSocket.send(packet);
				// Reset the buffer for the next floor request
				buffer.reset();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Floor requests sent!");
		}
	}

	public void receiveElevatorMovement() throws InterruptedException, UnknownHostException {
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

//		if (waitforMove[0] == (byte)3) {
//			notifyScheduler();
//		}
		
		// wait till elevator almost reaching floor 2
		//remove pause() after
		pause(); // or sleep to be consistent whilst getting rid of function overhead
		         // sleep for 8000 milli seconds and then call stopElevator
		
		
	}

	private void notifyScheduler() throws InterruptedException, UnknownHostException {
		

		Thread.sleep(8000);
		
		try {
			byte[] floortofloor = new byte[]{4};
			System.out.println (Arrays.toString(floortofloor));
			//DatagramSocket sendToScheduler = new DatagramSocket();
			DatagramPacket packet = new DatagramPacket(floortofloor, floortofloor.length, InetAddress.getLocalHost(), 45 );
			sendReceiveSocket.send(packet);
			sendReceiveSocket.close();
			
		}	catch (IOException e) {
				e.printStackTrace();
		}
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
			try {
				this.receiveElevatorMovement();
			} catch (InterruptedException | UnknownHostException e) {
				e.printStackTrace();
			}
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