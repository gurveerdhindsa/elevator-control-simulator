package main;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SchedulerElevators implements Runnable{
	
	private int currentFloor;
	private int destinationFloor;
	private int assignedPort;
	private int elevPortNumber;
	private int initialList;
	private List<FloorRequest> up;
	long startTime;
	private List<FloorRequest> down;
	private FloorRequest currentRequest;
	private int topFloor;
	private int direction; // 1 is up -1 is down
	private DatagramSocket receiveElevatorSocket;
	private DatagramSocket sendElevatorSocket;
	private FloorRequest lastRequest;
	private InetAddress addr;
	
	private boolean useDoorTime;
    private Thread timer;
    public static long startDoorTime;
    public static long totalDoorTime;
    public static long startRequestTime;
    public static long totalRequestTime;
    public static long startMoveTime;
    public static long totalFloorToFloorTime;
	
	/**
	 * Creates a runnable instance with a single thread
	 * which will handle all communications the Elevator 
	 * with elevPort has to do with scheduler from now on
	 * @param up: the list in which all requests in the 'up' 
	 *        direction are queued
	 * @param down: the list in which all requests in the 'down' 
	 *        direction are queued
	 * @param elevPort: the port number of the Elevator client 
	 *                  that this runnable instance will be 
	 *                  responsible for
	 * @param assignedPort: the port number on the scheduler side
	 *                    to which the client Elevator instance
	 *                    will send all further messages to
	 * @param initList:  the initial direction which the
	 *                 client Elevator instance is headed to
	 */
	public SchedulerElevators(List<FloorRequest>up, List<FloorRequest>down, 
			int elevPort, int assignedPort, int initList, InetAddress addr)
	{
		this.up = up;
		this.down = down;
		this.elevPortNumber = elevPort;
		this.assignedPort = assignedPort;
		this.initialList = initList;
		this.direction = this.initialList == 1 ? 1 : -1;
		this.topFloor = 19; // remove hard-coded value
		this.addr = addr;
		try {
			this.receiveElevatorSocket = new DatagramSocket(assignedPort);
			this.sendElevatorSocket = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * goes through all floors from the param floor to
	 * the topmost floor checking if there is a request
	 * (direction button pressed) on any of these floors
	 * it returns a FloorRequest instance otherwise null
	 * @param floor:the base floor at which to start checking
	 *              for requests
	 * @return: a FloorRequest instance or null
	 */
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
	
	/**
	 *goes through all floors from the param floor to
	 * the bottom floor checking if there is a request
	 * (direction button pressed) on any of these floors
	 * it returns a FloorRequest instance otherwise null
	 * @param floor:the base floor at which to start checking
	 *              for requests
	 * @return: a FloorRequest instance or null
	 */
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
	
	
	/**
	 * Checks if there is a pending request in the
	 * 'up' direction on a certain floor
	 * @param floor:floor on which to check for 
	 *              pending request
	 * @return:FloorRequest instance if there is, null
	 *         if no pending request. 
	 */
	public FloorRequest getUpCurrentFloorRequest(int floor)
	  {
		synchronized(this.up)
		{
			if(this.up.get(floor).timestamp != null)
			{
				FloorRequest req = this.up.get(floor);
				this.up.set(floor, new FloorRequest());
				System.out.println("got a request for floor" + floor);
				return req;
			}
		}
		System.out.println("Returning null");
		return null;
	  }
	
	/**
	 * Checks if there is a pending request in the
	 * 'down' direction on a certain floor
	 * @param floor:floor on which to check for 
	 *              pending request
	 * @return:FloorRequest instance if there is, null
	 *         if no pending request. 
	 */
	public FloorRequest getDownCurrentFloorRequest(int floor)
	  {
		synchronized(this.down)
		{
			if(this.down.get(floor).timestamp != null)
			{
				FloorRequest req = this.down.get(floor);
				this.down.set(floor, new FloorRequest()); //USE THIS EVERYTIME YOU WANT TO REMOVE
				                                          //A REQUEST FROM AN INDEX IN THE LIST
				return req;
			}
		}
		return null;
	  }
	
	/**
	 * Checks all floors from param to topmost floor
	 * for pending requests in the 'up' direction.
	 * If none, block waiting in the 'up' direction
	 * requests queue 
	 * @param floor: floor from which to start checking
	 * @return: FloorRequest instance representing
	 *         the request pending in the 'up'
	 *         direction of a floor
	 */
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
			this.up.set(reqIndex, new FloorRequest()); //USE THIS EVERYTIME YOU WANT TO 
			                                          //REMOVE A REQUEST FROM AN INDEX	IN THE LIST
			return req;
		}
	}
	
	/** Checks all floors from param to bottom floor
	 * for pending requests in the 'down' direction.
	 * If none, block waiting in the 'down' direction
	 * requests queue 
	 * @param floor: floor from which to start checking
	 * @return: FloorRequest instance representing
	 *         the request pending in the 'down'
	 *         direction of a floor
	 */
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
	
	/**
	 * control function that listens
	 * for messages from this instance's
	 * single client Elevator instance and 
	 * take appropriate actions based on
	 * message received
	 */
	public void workerFunction()
	{
		boolean breakOut = false;
		Random generator = new Random();
		while(true)
		{
			if(breakOut)
			{
				break;
			}
			byte[] msg = new byte[100];
			//listen for elevator message	
			try {
				DatagramPacket packet = new DatagramPacket(msg, msg.length);
				this.receiveElevatorSocket.receive(packet);
			}
			catch (IOException e) {
				//e.printStackTrace();
				break; //@TODO get rid of and do better handling
			} 
			
			switch(msg[0])
			{
			case 3: //door closed
				
				if(this.currentRequest != null)
				{	
					totalDoorTime = System.nanoTime() - startDoorTime;
					//interrupt timer
					timer.interrupt();
					sendRequest();
					
				}
				else
				{
					//interrupt timer
					timer.interrupt();
					msg = new byte[] {11};
					try
					{
						DatagramPacket updateDestination = new DatagramPacket(msg,msg.length,
								addr, this.elevPortNumber);
						this.sendElevatorSocket.send(updateDestination);
					}
					catch(IOException e)
					{
						e.printStackTrace();
					}	
				}
				break;
			
			case 5: // ready
				totalRequestTime = System.nanoTime() - startRequestTime;
				this.direction = msg[3];
				this.destinationFloor = msg[2];
				sendMove();
				break;
			
			case 7: //arrival sensor
				totalFloorToFloorTime = System.nanoTime() - startMoveTime;
				//not special case & direction up
				this.timer.interrupt();
				this.updateCurrentFloor();
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
				System.out.println("Scheduler-> Elevator with port:" + this.elevPortNumber
						+ " is now stopped with doors Open");
				System.out.println("Scheduler-> Instructing Elevator with port:" + 
						this.elevPortNumber + " to close it's doors");
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
			
			case 12: // resend door close
				//set discard next receive doorclosed
				//sendDoorclose
				System.out.println("Resending door close");
				//this.discardNextReceived = true;
				if(this.currentRequest != null)
				{
					this.currentRequest.doorTime = generator.nextInt(2);
				}
				else
					{
					this.lastRequest.doorTime = generator.nextInt(2);
					}
				reSendDoorClose();
				break;
			
			case 13: //send shutdown to elevator
				System.out.println("Shutting down elevator with port " + this.elevPortNumber);
				byte [] crash = new byte [] {14};
				breakOut = true;
				try {
					DatagramPacket request = new DatagramPacket(crash, crash.length, addr, this.elevPortNumber);
					DatagramSocket timerSocket = new DatagramSocket();
					timerSocket.send(request); 
					timerSocket.close();
				}
				catch(IOException e)
				{
					
				}
				break;
			
			default:
				break;
			}
		}
	}
	
	
	/**
	 * Handles the arrival of an elevator to a
	 * floor if the elevator was moving in the
	 * 'down' direction. If the elevator has 
	 * no more passengers inside a new request is 
	 * searched for it. However if elevator still
	 * has passengers it is commanded to close it's
	 * doors and resume moving
	 * @param msg: byte array containing message
	 *           sent from the client Elevator 
	 *           instance indicating it's arrival
	 *           and doors opening at a floor
	 */
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
	}
	
	/**
	  * Handles the arrival of an elevator to a
	 * floor if the elevator was moving in the
	 * 'up' direction. If the elevator has 
	 * no more passengers inside a new request is 
	 * searched for it. However if elevator still
	 * has passengers it is commanded to close it's
	 * doors and resume moving
	 * @param msg: byte array containing message
	 *           sent from the client Elevator 
	 *           instance indicating it's arrival
	 *           and doors opening at a floor
	 */
	private void handleUpArrival(byte[] msg)
	{
		//System.out.println(x);
		if(this.currentFloor == this.topFloor)
		{
			this.currentRequest = this.checkInitialDown(this.currentFloor);
			return;
		}
		
		switch(msg[1])
		{
		case 0:
			System.out.println("Scheduler-> Elevator with port:" + this.elevPortNumber
					+ " has arrived at floor:" + this.currentFloor
					+ " and is now stationary. Finding pending request to assign to it");
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
			System.out.println("Scheduler-> Elevator with port:" + this.elevPortNumber
					+ " has arrived at floor:" + this.currentFloor
					+ " and needs to keep moving to drop passenger "
					+ "at floor:" + msg[2]);
			this.currentRequest = this.getUpCurrentFloorRequest(this.currentFloor);
			System.out.println("Request should be empty now " + this.currentRequest);
			break;
		}
	}
	
	/**
	 * Handles the sensor message sent from
	 * an Elevator client instance when it
	 * approaches a floor whilst going 'up'.
	 * Method checks if there is a pending 
	 * request in the 'up' direction for the
	 * floor the elevator is approaching.
	 * If there is, elevator is commanded
	 * to stop, otherwise do nothing
	 */
	public void handleUp8s()
	{
		if(this.currentFloor != this.destinationFloor)
		{
			this.currentRequest = getUpCurrentFloorRequest(this.currentFloor);
		}
	
		if(this.currentRequest != null)
		{
			sendStop();	
		}
	}
	
	/**
	 Handles the sensor message sent from
	 * an Elevator client instance when it
	 * approaches a floor whilst going 'down'.
	 * Method checks if there is a pending 
	 * request in the 'down' direction for the
	 * floor the elevator is approaching.
	 * If there is, elevator is commanded
	 * to stop, otherwise do nothing
	 */
	public void handleDown8s()
	{
		if(this.currentFloor != this.destinationFloor)
		{
			this.currentRequest = getDownCurrentFloorRequest(this.currentFloor);
		}
		
		if(this.currentRequest != null)
		{
			sendStop();
		}
		
	}
	
	/**
	 *Updates the currentFloor a client
	 *Elevator instance is currently on
	 */
	public void updateCurrentFloor()
	{
		if (direction == 1) {
		     currentFloor++;
		}
		else {
		   currentFloor--;
		}
		
		System.out.println("Scheduler-> Elevator with port:" + this.elevPortNumber
				+ " now approaching floor:" + this.currentFloor);
	}
	
	/**
	 * Function from which an instance notifies
	 * it's client Elevator instance of it's
	 * port and searches for first request to handle
	 */
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
	
	/**
	 * Sends a message to the client Elevator instance
	 * commanding it to start moving
	 */
	private void sendMove()
	{
		byte[] msg = new byte[] {6};
		try
		{
			DatagramPacket request = new DatagramPacket(msg,msg.length,
					addr, this.elevPortNumber);
			System.out.println("Scheduler-> Instructing Elevator with port:" + this.elevPortNumber
					+ " to start moving");
			this.timer = new Thread(this,"timerThread");
			this.sendElevatorSocket.send(request);
			startMoveTime = System.nanoTime();
			this.useDoorTime = false;
			this.startTime = System.nanoTime();
			this.timer.start();
			
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 
	 */
	public void timer()
	{
		FloorRequest floorRequest = this.currentRequest == null ? this.lastRequest : this.currentRequest;
		try
		{
			Thread.sleep(this.useDoorTime == true ? 535 : 3005);
			byte msg [] = new byte [] { this.useDoorTime == true ? (byte)12 : (byte)13};
			System.out.println("timeout");
			DatagramPacket request = new DatagramPacket(msg,msg.length, InetAddress.getLocalHost(), this.assignedPort);
			DatagramSocket timerSocket = new DatagramSocket();
			timerSocket.send(request);
			timerSocket.close();
			
		}
		catch(InterruptedException e)
		{
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Sends a request to a client Elevator instance with 
	 * information like the floor on which there is a pending
	 * request, the destination floor that passenger wants to 
	 * got to, and the direction the passenger wants to move(up or down)
	 */
	private void sendRequest()
	{
		//[4 - floor - carButton - direction(1 is up -1 is down)
		byte[] msg = new byte[5];
		
		msg[0] = (byte)4;
		msg[1] = (byte)this.currentRequest.floor;
		msg[2] = (byte)this.currentRequest.carButton;
		msg[3] = this.currentRequest.floorButton.equals("up") ? (byte)1 : (byte)-1;
		msg[4] = (byte)this.currentRequest.floorTime;
		try
		{
			DatagramPacket request = new DatagramPacket(msg,msg.length,
					addr,this.elevPortNumber);
			
			System.out.println("Scheduler ->sending Request msg to Elevator with port:" + this.elevPortNumber
					+ " Msg->[floorNum:" + msg[1] + ", carButton:" + msg[2] + ", direction:"
					+ (msg[3] == 1 ? "up" : "down") + "]");
			this.sendElevatorSocket.send(request);
			startRequestTime = System.nanoTime();
			this.lastRequest = this.currentRequest;
			this.currentRequest = null;
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Sends a message to the client Elevator instance
	 * notifying about which port to send
	 * all further communication with Scheduler to
	 */
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
					addr,this.elevPortNumber);
			this.sendElevatorSocket.send(registration);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Sends a message to the client Elevator instance
	 * instructing it to stop at the currentFloor it is
	 * approaching 
	 */
	private void sendStop() {
		
		byte[] stopData = new byte[] {8};
		
		try {
			DatagramPacket elevatorStopPckt = new DatagramPacket(stopData, stopData.length, 
					addr,this.elevPortNumber);
			this.sendElevatorSocket.send(elevatorStopPckt);
			
			System.out.println("Scheduler-> stopping Elevator with port:" + this.elevPortNumber
					+ " to pick up passenger at:" + this.currentFloor
					+ " going " + this.currentRequest.floorButton + " to:" + 
					this.currentRequest.carButton);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Sends a message to the client Elevator instance
	 * instructing it to close it's doors
	 */
	public void sendDoorClose()
	{
		byte[] doorCloseMsg = new byte[] {2,this.currentRequest != null ? (byte)this.currentRequest.doorTime 
				: (byte)this.lastRequest.doorTime};
		System.out.println("Sending door close with " + Arrays.toString(doorCloseMsg));
		try {
			DatagramSocket sendDoorClose = new DatagramSocket();
			DatagramPacket doorClosePkt = new DatagramPacket(doorCloseMsg, doorCloseMsg.length, addr,elevPortNumber);
			sendDoorClose.send(doorClosePkt);
			startDoorTime = System.nanoTime();
			this.useDoorTime = true;
			timer = new Thread(this, "timerThread");
			sendDoorClose.close();
			this.startTime = System.nanoTime();
			timer.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	

	public void reSendDoorClose()
	{
		byte[] doorCloseMsg = new byte[] {15,this.currentRequest != null ? (byte)this.currentRequest.doorTime 
				: (byte)this.lastRequest.doorTime};
		System.out.println("Sending door close with " + Arrays.toString(doorCloseMsg));
		try {
			DatagramSocket sendDoorClose = new DatagramSocket();
			DatagramPacket doorClosePkt = new DatagramPacket(doorCloseMsg, doorCloseMsg.length, addr,elevPortNumber);
			sendDoorClose.send(doorClosePkt);
			this.useDoorTime = true;
			timer = new Thread(this, "timerThread");
			sendDoorClose.close();
			this.startTime = System.nanoTime();
			timer.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		if(Thread.currentThread().getName().equals("timerThread"))
		{
			this.timer();
		}
		else
		{
			this.start();
		}
	}
	
	public void stop()
	{
		this.sendElevatorSocket.close();
		
	}
}
