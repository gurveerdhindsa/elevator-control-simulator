package main;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;

public class SchedulerElevators implements Runnable{
	
	int currentFloor;
	int destinationFloor;
	int assignedPort;
	int elevPortNumber;
	int initialList;
	List<FloorRequest> up;
	List<FloorRequest> down;
	FloorRequest currentRequest;
	int topFloor;
	int direction; // 1 is up -1 is down
	DatagramSocket receiveElevatorSocket;
	DatagramSocket sendElevatorSocket;
	
	public SchedulerElevators(List<FloorRequest>up, List<FloorRequest>down, int elevPort, int assignedPort, int initList)
	{
		this.up = up;
		this.down = down;
		this.elevPortNumber = elevPort;
		this.assignedPort = assignedPort;
		this.initialList = initList;
		try {
			this.receiveElevatorSocket = new DatagramSocket(assignedPort);
			this.sendElevatorSocket = new DatagramSocket(elevPort);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public FloorRequest checkDownRequests(int floor)
	{
		synchronized(this.down)
		{
			for(int i = floor; i >= 0; i--)
			{
				FloorRequest req = this.down.get(i);
				if(req.timestamp != null)
				{
					return req;
				}
			}
			return null;
		}
	}
	
	//is there an up request in the current floor?
	public FloorRequest getUpCurrentFloorRequest(int floor)
	  {
		synchronized(this.up)
		{
			if(this.up.get(floor).timestamp != null)
			{
				FloorRequest req = this.up.get(floor);
				this.up.set(floor, new FloorRequest());
				return req;
			}
			else {
				return new FloorRequest();
			}
		}
	  }
	
	//is there an down request in the current floor?
	public FloorRequest getDownCurrentFloorRequest(int floor)
	  {
		synchronized(this.down)
		{
			if(this.down.get(floor).timestamp != null)
			{
				FloorRequest req = this.down.get(floor);
				this.down.set(floor, new FloorRequest());      //USE THIS EVERYTIME YOU WANT TO REMOVE A REQUEST FROM AN INDEX IN THE LIST
				return req;
			}
			else {
				return new FloorRequest();
			}
		}
	  }
	
	//At initialization - check all up requests starting from parameter floor to 19 
	private FloorRequest checkInitialUp(int floor)
	{
		boolean empty = true;
		int reqIndex = 0;
		
		synchronized(this.up)
		{
			for(int i = floor; i < 20; i++)
			{
				if(this.up.get(i).timestamp != null)
				{
					reqIndex = i;
					empty = false;
				}
			}
			
			while(empty)
			{
				try {
					this.up.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				for(int i = floor; i < this.up.size(); i++)
				{
					if(this.up.get(i).timestamp != null)
					{
						reqIndex = i;
						empty = false;
					}
				}
				
			}
			
			FloorRequest req = this.up.get(reqIndex);
			this.up.set(reqIndex, new FloorRequest());      //USE THIS EVERYTIME YOU WANT TO REMOVE A REQUEST FROM AN INDEX	IN THE LIST
			return req;
		}
	}
	
	//At initialization - check all down requests starting from floor 19 to 0 
	private FloorRequest checkInitialDown(int floor)
	{
		boolean empty = true;
		int reqIndex = 0;
		
		synchronized(this.down)
		{
			for(int i = floor; i >= 0; i--)
			{
				if(this.down.get(i).timestamp != null)
				{
					reqIndex = i;
					empty = false;
				}
			}
			
			while(empty)
			{
				try {
					this.down.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				for(int i = floor; i < this.down.size(); i++)
				{
					if(this.down.get(i).timestamp != null)
					{
						reqIndex = i;
						empty = false;
					}
				}
				
			}
			
			FloorRequest req = this.down.get(reqIndex);
			this.down.set(reqIndex, new FloorRequest());      //USE THIS EVERYTIME YOU WANT TO REMOVE A REQUEST FROM AN INDEX IN THE LIST	
			return req;
		}
	}
	
	
	public void workerFunction()
	{
		while(true)
		{
			byte[] msg = new byte[100];
			//listen for elevator message
				
				
			try {
				DatagramPacket packet = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), assignedPort);
				System.out.println("Waiting for message from elevator");
				this.receiveElevatorSocket.receive(packet);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue; //@TODO get rid of and do better handling
			}
			
			System.out.println("Elevator message received for register elevator");
			System.out.println(Arrays.toString(msg));
			
			
			// 8s notification
			if(msg[0] == 9)
			{
				//updateCurrentFloor();
				
				//if special case (if elevator was going Up but then gets a down Request it will set a bit till it gets to that floor)
				   //do nothing
				//else if up
				  //handleUp8s()
				//else
				  //handleDown8s()
			}
			
			//Arrival notification
			else if(msg[0] == 8)
			{
				/**
				 * FloorRequest req;
				 * if currentFloor == topMostFloor
				 * {
				 *     req = checkDownRequest()
				 *     if req == null 
				 *         checkInitialDown(currentFloor)
				 * }
				 * else if(direction == 1 && msg[pendingIndicator] != 1)
				 * {
				 *    req = checkUpRequests(currentFloor);
				 *    if req == null
				 *    {
				 *       req = checkDownRequest(topfloor)
				 *        if req == null:
				 *             checkInitialUp(currentFloor);
				 *    }
				 *    
				 *    if there is somekind of request do process below
				 *    close door 
				 *    this.currentRequest = req;
				 *        
				 * }
				 * 
				 * else if(direction == 1 && msg[pendingIndicator] == 1)
				 * {
				 *     this.currentRequest = getUpRequest(this.currentFloor);
				 *     closedoor
				 *     wait for door closed 
				 *      
				 * }
				 */
				
			}
			
			//door closed
			else if(msg[0] == 3)
			{
				/**
				 * if this.currentRequest != null
				 * {
				 *   sendRequest() ->  
				 *   
				 * }
				 * else
				 * {sendMove()}
				 */
			}
			
			//elevatorReady
			else if(msg[0] == 1)
			{
				//update direction, destination, specialCase, currentfloor
				//sendMove()
				
			}
			//elevator stopped message 
			else
			{
				sendDoorClose();
			}
		}
	}
	
	
	public void waitUp()
	{
		synchronized(this.up)
		{
			
		}
	}
	public void handleUp8s()
	{
		FloorRequest req = null;
		
		if(this.currentFloor != this.destinationFloor)
		{
			req = getUpCurrentFloorRequest(this.currentFloor);
		}
		
		if(req != null)
		{
			sendStop();	
		}
		
	}
	
	public void handleDown8s()
	{
		FloorRequest req = null;
		
		if(this.currentFloor != this.destinationFloor)
		{
			req = getDownCurrentFloorRequest(this.currentFloor);
		}
		
		if(req != null)
		{
			sendStop();
		}
		
	}
	public void updateCurrentFloor()
	{
		if (direction == 1) {
		     currentFloor++;
		}
		else {
		   currentFloor--;
		}
	}
	public void start()
	{

		//FloorRequest req = initialList == 1 ? checkInitialUp() : checkInitialDown();
		if(this.initialList==1) {
			checkInitialUp(0);
		}
		else {
			checkInitialDown(19);
		}
		
		//Send registration confirmation message to elevator
		sendRegistrationConfirmation();
		
		//send door close
		sendDoorClose();
	
		//listen for message from elevator
		

		// get it 
		//send req
	
		
		// call workerFunction()
		workerFunction();
		
		
	}
	
	public void sendRegistrationConfirmation() {
		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		buffer.write((byte) 1);
		buffer.write((byte) this.assignedPort);
		buffer.write((byte) this.direction);
		byte[] data = buffer.toByteArray();
		System.out.println("Sent regristration confirmation with followig data: " + Arrays.toString(data));
	}
	
	public void sendStop() {
		
		byte[] stopData = new byte[] {8};
		
		try {
			//DatagramSocket sendInterrupt = new DatagramSocket();
			DatagramPacket elevatorStopPckt = new DatagramPacket(stopData, stopData.length, InetAddress.getLocalHost(),elevPortNumber);
			sendElevatorSocket.send(elevatorStopPckt);
			System.out.println("Sent stop");
			//sendElevatorSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void sendDoorClose()
	{
		System.out.println("Sending door close message. ");
		byte[] doorCloseMsg = new byte[] {2};
		try {
			DatagramSocket sendDoorClose = new DatagramSocket();
			DatagramPacket doorClosePkt = new DatagramPacket(doorCloseMsg, doorCloseMsg.length, InetAddress.getLocalHost(),elevPortNumber);
			sendDoorClose.send(doorClosePkt);
			System.out.println("Sent door close message. ");
			sendDoorClose.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}
