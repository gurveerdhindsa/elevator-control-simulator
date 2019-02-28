package test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import main.SchedulerElevators;
import main.FloorRequest;

public class SchedulerElevatorsTest {
	
	private SchedulerElevators scheduler;
	private List<FloorRequest>up;
	private List<FloorRequest>down;
	private int elevatorPortNum;
	private int assignedPortNum;
	private Thread schedulerThread;
	private Timestamp floorTime;
	private Timestamp doorTime;
	
	@Before
	public void setUp()
	{
		this.elevatorPortNum = 23;
		this.assignedPortNum = 46;
		this.up = new ArrayList<>();
		this.down = new ArrayList<>();
		
		for(int i = 0; i < 20; i++)
		{
			this.up.add(new FloorRequest());
			this.down.add(new FloorRequest());
		}
		
		this.scheduler = new SchedulerElevators(this.up,
				this.down,this.elevatorPortNum,this.assignedPortNum,0);
	}
	
	@After
	public void tearDown()
	{
		this.scheduler.stop();
	}
	
	/**
	 * Mimics the incoming of a request to the
	 * scheduler subsystem
	 * @param list: the queue in which to put the request
	 * @param request: the FloorRequest instance representing
	 *               the request
	 */
	public void inputRequest(List<FloorRequest>list, FloorRequest request)
	{
		synchronized(list)
		{
			list.set(request.floor, request);
			list.notifyAll();
		}
	}
	
	/**
	 * Makes current thread sleep for a certain time
	 * @param time
	 */
	public void waitms(int time)
	{
		try {
			Thread.sleep((long)time);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	/**
	 * Mimics an elevator sending 'ready to start moving' msg
	 * to scheduler after processing a request
	 * @param currentFloor: the currentfloor the elevator instance is at
	 * @param destinationFloor: the destinationfloor it plans to start moving to
	 * @param direction: the direction it has to move to get to destinationfloor
	 */
	public void sendReady(int currentFloor, int destinationFloor, int direction)
	{
		byte[] elevReady = new byte[] {5, (byte)currentFloor, (byte)destinationFloor,
				(byte)direction};
		
		try
		{
			DatagramPacket pckt = new DatagramPacket(elevReady,elevReady.length,
					InetAddress.getLocalHost(), this.assignedPortNum);
			DatagramSocket sckt = new DatagramSocket();
			sckt.send(pckt);
			sckt.close();
		}catch(IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		
	}
	
	/**
	 * Mimics an elevator notifying the scheduler that 
	 * it's doors are now closed
	 */
	public void sendDoorClosed()
	{
		//mimic sending door close from scheduler side
		byte[] doorCloseMsg = new byte[] {3};
		try {
			DatagramPacket doorClosePckt = new DatagramPacket(doorCloseMsg,doorCloseMsg.length,
					InetAddress.getLocalHost(),this.assignedPortNum);
			DatagramSocket doorCloseSocket = new DatagramSocket();
			doorCloseSocket.send(doorClosePckt);
			doorCloseSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			fail("Did not send ");
			e.printStackTrace();
		} 
	}
	
	/**
	 * creates a request
	 * @param floor: the floor at which the request is being made
	 * @param carButton: the destination floor the passenger wants to go to
	 * @param direction: the direction(up or down)
	 * @return a FloorRequest instance containing all the information
	 */
	public FloorRequest createRequest(int floor, int carButton, String direction)
	{
		Timestamp timestamp = null;
		direction = direction.toLowerCase();
		SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss.SSS");
		Date parsedDate;
		try {
			parsedDate = dateFormat.parse("14:05:15.0");
			timestamp = new Timestamp(parsedDate.getTime());
			
			 Date f = dateFormat.parse("00:00:08.0");
			 floorTime = new Timestamp(f.getTime());
			    
			    Date door = dateFormat.parse("00:00:02.0");
			    doorTime = new Timestamp(door.getTime());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
	   FloorRequest request = new FloorRequest(timestamp,floor,carButton,direction , floorTime, doorTime);
	   return request;
	}
	
	/**
	 * Info: Tests the messaging sequence between an Elevator 
	 * and Scheduler from doorOpen state to Moving state of Elevator
	 * @throws SocketException
	 */
	@Test
	public void testSequence() throws SocketException {
		
		System.out.println("####Starting:testSequence####");
		
		//setup
		int floor = 5;
		int carButton = 1;
		this.schedulerThread = new Thread(this.scheduler);
		byte[] data = new byte[100];
		DatagramPacket pckt = new DatagramPacket(data,data.length);
		DatagramSocket sckt = new DatagramSocket(this.elevatorPortNum);
		
		
		//assumption is an Elevator registration
		//has already been sent to the Scheduler 
		//and this SchedulerElevators instance 
		//represents the personalized thread the
		//scheduler has spawn for a specific 
		//elevator. So on starting 
		//we expect the thread to make itself
		//known to the elevator, hence we listen 
		//for an ElevatorRegistration confirmation
		//from the scheduler side
		try
		{
			schedulerThread.start();
			sckt.receive(pckt);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		//validate the received registration confirmation message
		assertTrue(data[0] == 1);
		assertTrue(data[1] == 46);
		assertTrue(data[2] == 0);
		
		//create a request and put it into the list
		//visible to the personalized thread for an elevator 
		//on the scheduler side
		FloorRequest request = this.createRequest(floor, carButton, "Down");
		//listen for close door msg from SUT(system under test)
		try
		{
			this.inputRequest(down, request);
			//listen for a close door command on elevator side
			sckt.receive(pckt);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		assertTrue(data[0] == 2);
		
		//send reply to SUT and listen for
		//request msg from SUT(System under test)
		try
		{
			this.sendDoorClosed();
			sckt.receive(pckt);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		assertTrue(data[0] == 4);//expected msg index
		assertTrue(data[1] == floor); //floor num
		assertTrue(data[2] == carButton); //carButton num
		assertTrue(data[3] == -1); //direction shld be down
		
		//mimic sending readyMsg to SUT and listening for move
		try
		{
			this.sendReady(0, 5, -1);
			sckt.receive(pckt);
		}catch(IOException e)
		{
			e.printStackTrace();
		}
		
		assertTrue(data[0] == 6);//verify move msg;
		sckt.close();
		this.waitms(50);
		System.out.println("####EndTest:testSequence####");
	}

}
