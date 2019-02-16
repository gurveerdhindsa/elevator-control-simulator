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
	
	private int currentFloor;
	private int destinationFloor;
	private int assignedPort;
	private int elevPortNumber;
	private int initialList;
	private List<FloorRequest> up;
	private List<FloorRequest> down;
	private FloorRequest currentRequest;
	private int topFloor;
	private int direction; // 1 is up -1 is down
	private DatagramSocket receiveElevatorSocket;
	private DatagramSocket sendElevatorSocket;
	
	public SchedulerElevators(List<FloorRequest>up, List<FloorRequest>down, 
			int elevPort, int assignedPort, int initList)
	{
		this.up = up;
		this.down = down;
		this.elevPortNumber = elevPort;
		this.assignedPort = assignedPort;
		this.initialList = initList;
		this.direction = this.initialList == 1 ? 1 : -1;
		this.topFloor = 19; // remove hard-coded value
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
			for(int i = floor; i <= this.topFloor; i++)
			{
				FloorRequest req = this.up.get(i);
				if(req.timestamp != null)
				{
					this.up.set(i, new FloorRequest());
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
				if(this.down.get(i).timestamp != null)
				{
					this.down.set(i, new FloorRequest());
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
					System.out.println("Scheduler: " + Thread.currentThread().getName() + 
							" Waiting on up list");
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
			System.out.println("Scheduler: " + Thread.currentThread().getName() + 
					" Got up request");
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
					break;
				}
				
				for(int i = floor; i >= 0; i--)
				{
					if(this.down.get(i).timestamp != null)
					{
						reqIndex = i;
						empty = false;
					}
				}
				
			}
			
			FloorRequest req = this.down.get(reqIndex);
			this.down.set(reqIndex, new FloorRequest()); //USE THIS EVERYTIME YOU WANT 
			                                             //TO REMOVE A REQUEST FROM AN INDEX IN THE LIST	
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
				DatagramPacket packet = new DatagramPacket(msg, msg.length);
				this.receiveElevatorSocket.receive(packet);
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("closing");
				break; //@TODO get rid of and do better handling
			} 
			
			switch(msg[0])
			{
			case 3: //door closed
				if(this.currentRequest != null)
				{
					sendRequest();
				}
				else
				{
					msg = new byte[] {11};
					try
					{
						DatagramPacket updateDestination = new DatagramPacket(msg,msg.length,
								InetAddress.getLocalHost(), this.elevPortNumber);
						this.sendElevatorSocket.send(updateDestination);
					}
					catch(IOException e)
					{
						e.printStackTrace();
					}
					
				}
				break;
			
			case 5: // ready
				this.direction = msg[3];
				sendMove();
				break;
			
			case 7: //arrival sensor
				this.updateCurrentFloor();
				//not special case & direction up
				if(msg[2] != 1 && msg[1] == 1)
				{
					handleUp8s();
				}
				//not special case & direction down
				else if(msg[2] != 1 &&  msg[1] == -1)
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
		case 0:
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
		System.out.println("Scheduler: " + Thread.currentThread().getName() + "handled down arrival: " + this.currentRequest
				+ " for scheduler with port " + this.assignedPort);
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
		case 0:
			this.currentRequest = this.checkUpRequests(this.currentFloor);
			if(this.currentRequest == null)
			{
				System.out.println("Scheduler: " + Thread.currentThread().getName()+ " checking down req");
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
		//this.updateCurrentFloor();
		if(this.currentFloor != this.destinationFloor)
		{
			this.currentRequest = getUpCurrentFloorRequest(this.currentFloor);
		}
		
		System.out.println("Scheduler: " + Thread.currentThread().getName() + "updatedfloor: " + this.currentFloor + " req:" + this.currentRequest
				+ " for scheduler with port " + this.assignedPort);
		if(this.currentRequest != null)
		{
			sendStop();	
		}
		
	}
	
	public void handleDown8s()
	{
		//this.updateCurrentFloor();
		if(this.currentFloor != this.destinationFloor)
		{
			this.currentRequest = getDownCurrentFloorRequest(this.currentFloor);
		}
		
		System.out.println("Scheduler: " + Thread.currentThread().getName() + " updatedfloor: " + this.currentFloor + " req:" + this.currentRequest
				+ " for scheduler with port " + this.assignedPort);
		if(this.currentRequest != null)
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
		System.out.println("Scheduler: " + Thread.currentThread().getName() + " updated current floor  " + this.currentFloor
				+" for scheduler with port " + this.assignedPort);
	}
	public void start()
	{

		sendRegistrationConfirmation();
		//1 up 0 down
		this.currentRequest = this.initialList == 1 ? checkInitialUp(0) : checkInitialDown(19);
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
			this.currentRequest = null;
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
		buffer.write((byte) this.initialList);
		byte[] data = buffer.toByteArray();
		//System.out.println("Scheduler: sending confrimation to Elevator with port:" + this.elevPortNumber);
		try
		{
			DatagramPacket registration = new DatagramPacket(data,data.length,
					InetAddress.getLocalHost(),this.elevPortNumber);
			this.sendElevatorSocket.send(registration);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void sendStop() {
		
		byte[] stopData = new byte[] {8};
		
		try {
			DatagramPacket elevatorStopPckt = new DatagramPacket(stopData, stopData.length, 
					InetAddress.getLocalHost(),this.elevPortNumber);
			this.sendElevatorSocket.send(elevatorStopPckt);
			System.out.println(Thread.currentThread().getName() + 
					" Sent stop");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void sendDoorClose()
	{
		byte[] doorCloseMsg = new byte[] {2};
		try {
			DatagramSocket sendDoorClose = new DatagramSocket();
			DatagramPacket doorClosePkt = new DatagramPacket(doorCloseMsg, doorCloseMsg.length, InetAddress.getLocalHost(),elevPortNumber);
			sendDoorClose.send(doorClosePkt);
			sendDoorClose.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	@Override
	public void run() {
		// TODO Auto-generated method stub
		this.start();
		
	}
	
	public void stop()
	{
		this.sendElevatorSocket.close();
		
	}
}
