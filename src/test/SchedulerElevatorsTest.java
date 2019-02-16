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
	
	
	public void inputRequest(List<FloorRequest>list, FloorRequest request)
	{
		synchronized(list)
		{
			list.set(request.floor, request);
			list.notifyAll();
		}
	}
	
	public void waitms(int time)
	{
		try {
			Thread.sleep((long)time);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	
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
	
	public FloorRequest createRequest(int floor, int carButton, String direction)
	{
		Timestamp timestamp = null;
		direction = direction.toLowerCase();
		SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss.SSS");
		Date parsedDate;
		try {
			parsedDate = dateFormat.parse("14:05:15.0");
			timestamp = new Timestamp(parsedDate.getTime());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
	   FloorRequest request = new FloorRequest(timestamp,floor,carButton,direction);
	   return request;
	}
	
	@Test
	public void testSequence() throws SocketException {
		
		this.schedulerThread = new Thread(this.scheduler);
		byte[] data = new byte[100];
		DatagramPacket pckt = new DatagramPacket(data,data.length);
		DatagramSocket sckt = new DatagramSocket(this.elevatorPortNum);
		try
		{
			schedulerThread.start();
			sckt.receive(pckt);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		
		assertTrue(data[0] == 1);
		assertTrue(data[1] == 46);
		assertTrue(data[2] == 0);
		
		FloorRequest request = this.createRequest(5,1, "Down");
		
		try
		{
			this.inputRequest(down, request);
			sckt.receive(pckt);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		assertTrue(data[0] == 2);
		
		//elevators reply
		//this.sendDoorClosed();
		
		//this.waitms(1000);
	}

}
