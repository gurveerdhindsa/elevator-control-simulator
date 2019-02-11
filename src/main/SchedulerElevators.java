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
	
	public SchedulerElevators(List<FloorRequest>up, List<FloorRequest>down, 
			int elevPort, int assignedPort, int initList)
	{
		this.up = up;
		this.down = down;
		this.elevPortNumber = elevPort;
		this.assignedPort = assignedPort;
		this.initialList = initList;
		try {
			this.receiveElevatorSocket = new DatagramSocket(assignedPort);
			this.sendElevatorSocket = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public FloorRequest checkUpRequests(int floor)
	{
		synchronized(this.up)
		{
			for(int i = floor; i >= 0; i--)
			{
				FloorRequest req = this.up.get(i);
				if(req.timestamp != null)
				{
					return req;
				}
			}
			return null;
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
		}
		return null;
	  }
	
	//is there an down request in the current floor?
	public FloorRequest getDownCurrentFloorRequest(int floor)
	  {
		synchronized(this.down)
		{
			if(this.down.get(floor).timestamp != null)
			{
				FloorRequest req = this.down.get(floor);
				this.down.set(floor, new FloorRequest()); //USE THIS EVERYTIME YOU WANT TO REMOVE A REQUEST FROM AN INDEX IN THE LIST
				return req;
			}
		}
		return null;
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
			this.up.set(reqIndex, new FloorRequest()); //USE THIS EVERYTIME YOU WANT TO REMOVE A REQUEST FROM AN INDEX	IN THE LIST
			return req;
		}
	}
	
	//At initialization or when stationary and no requests
	//in curr direction
	//- check all down requests starting from floor 19 to 0 
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
			this.down.set(reqIndex, new FloorRequest()); //USE THIS EVERYTIME YOU WANT TO REMOVE A REQUEST FROM AN INDEX IN THE LIST	
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
			
			
			switch(msg[0])
			{
			case 3: //door close
				if(this.currentRequest != null)
				{
					sendRequest();
				}
				else
				{
					sendMove();
				}
				break;
			
			case 5: // ready
				
				sendMove();
				break;
			
			case 7: //arrival sensor
				this.updateCurrentFloor();
				//not special case & direction up
				if(msg[1] != 1 && msg[2] == 1)
				{
					handleUp8s();
				}
				//not special case & direction down
				else if(msg[1] != 1 &&  msg[2] == -1)
				{
					handleDown8s();
				}
				break;
			
			case 9: //stopped
				sendDoorClose();
				break;
			
			case 10: //arrival message
				if(msg[3] == 1)
				{
					this.handleUpArrival(msg);
				}
				else if(msg[3] == -1)
				{
					this.handleDownArrival(msg);
				}
				sendDoorClose();
				break;
			
			default:
				break;
			}
			
			/*
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
				/** breakdown into two fucntions,
				 *  up direction Arrival and down direction arrival
				 * FloorRequest req;
				 * if currentFloor == topMostFloor
				 * {
				 *     checkInitialDown(currentFloor)
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
				 *    sendDoorClose();
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
			}*/
		}
	}
	
	private void handleDownArrival(byte[] msg)
	{
		if(this.currentFloor == 0)
		{
			this.currentRequest = this.checkInitialUp(0);
			return;
		}
		
		switch(msg[1])
		{
		case -1:
			this.currentRequest = this.checkDownRequests(this.currentFloor);
			if(this.currentRequest == null)
			{
				this.currentRequest = this.checkUpRequests(0);
				
				if(this.currentRequest == null)
				{
					this.checkInitialDown(this.currentFloor);
				}
			}
			break;
		
		case 1:
			this.currentRequest = this.getDownCurrentFloorRequest(this.currentFloor);
			break;
		}
	}
	
	private void handleUpArrival(byte[] msg)
	{
		if(this.currentFloor == this.topFloor)
		{
			this.currentRequest = this.checkInitialDown(this.currentFloor);
			return;
		}
		
		switch(msg[1])
		{
		case -1:
			this.currentRequest = this.checkUpRequests(this.currentFloor);
			if(this.currentRequest == null)
			{
				this.currentRequest = this.checkDownRequests(this.topFloor);
				
				if(this.currentRequest == null)
				{
					this.checkInitialUp(this.currentFloor);
				}
			}
			break;
		case 1:
			this.currentRequest = this.getUpCurrentFloorRequest(this.currentFloor);
			break;
		}
	}
	public void handleUp8s()
	{
		FloorRequest req = null;
		this.updateCurrentFloor();
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
		this.updateCurrentFloor();
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

		sendRegistrationConfirmation();
		this.currentRequest = initialList == 1 ? checkInitialUp(0) : checkInitialDown(19);
		//send door close
		sendDoorClose();	
		// call workerFunction()
		workerFunction();
	}
	
	
	private void sendMove()
	{
		byte[] msg = new byte[] {6};
		try
		{
			DatagramPacket request = new DatagramPacket(msg,msg.length,
					InetAddress.getLocalHost(), this.elevPortNumber);
			this.sendElevatorSocket.send(request);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
	}
	private void sendRequest()
	{
		//[4 - floor - carButton - direction(1 is up -1 is down)
		byte[] msg = new byte[4];
		
		msg[0] = (byte)4;
		msg[1] = (byte)this.currentRequest.floor;
		msg[2] = (byte)this.currentRequest.carButton;
		msg[3] = this.currentRequest.floorButton.equals("up") ? (byte)1 : (byte)-1;
		try
		{
			DatagramPacket request = new DatagramPacket(msg,msg.length,
					InetAddress.getLocalHost(),this.elevPortNumber);
			this.sendElevatorSocket.send(request);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
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
