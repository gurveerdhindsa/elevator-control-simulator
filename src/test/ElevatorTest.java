package test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;
import main.Elevator;

public class ElevatorTest {
	
	/**
	 * Tests that an Elevator instance when started,
	 * sends its information such as port number to the 
	 * Scheduler on port 69 
	 */
	
	private Elevator elev;
	@Before
	public void setUp()
	{
		elev = new Elevator(10);
	}
	
	@After
	public void cleanUp()
	{
		this.elev.stop();
	}
	
	@Test 
	public void testRegistration()
	{
		
		Thread elevThread = new Thread(elev);
		
		byte[] regist = new byte[50];
		
		try {
			DatagramPacket registration = new DatagramPacket(regist,regist.length);
			DatagramSocket getRegist = new DatagramSocket(69);
			
			//start an elevator with all its child threads 
			elevThread.start();
			//mimic the listening for that information on the scheduler side
			getRegist.receive(registration);
			getRegist.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//verify that the message being sent 
		//contains the correct message identifier - 0 
		//and the correct number -10
		assertTrue((int)regist[0] == 0);
		assertTrue((int)regist[4] == 10);
		
	}
	
	/**
	 * Tests the functionality of an elevator 
	 * recieving a request with floor number 
	 * and car button number and validating that
	 * an elevator moves from its current floor 
	 * to the request floor and then the car button
	 * floor
	 */
	
	@Test
	public void testReceiveRequest()
	{
		Thread elevThread = new Thread(this.elev);
		
		//make elevator start listening
		elevThread.start();
		
		//send a registrationConfirmation msg
		byte[] request = new byte[] {1, 46, 0};
		try {
			DatagramPacket requestPckt = new DatagramPacket(request,request.length,
					InetAddress.getLocalHost(),10);
			DatagramSocket sendReq = new DatagramSocket();
			sendReq.send(requestPckt);
			sendReq.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			fail("Did not send ");
			e.printStackTrace();
		}
		
		//validate elevator state 
		
		
		//make up a request 
		//send fake request {4, 8, 3, -1}
		
		//listen for ready 
		// when you get ready assert that info is correct
		// veriy data[2] == 8, data[3] == 1(up to answer down req) 
	}

	/**
	 * 
	 */
	/*
	@Test
	public void testSendDoorCloseMsg()
	{
		Elevator elev = new Elevator(10);
		Thread elevThread = new Thread(elev);
		
		byte[] regist = new byte[40];
		try
		{
			DatagramPacket registration = new DatagramPacket(regist,regist.length);
			DatagramSocket getRegist = new DatagramSocket(69);
			
			//start an elevator with all its child threads 
			elevThread.start();
			//mimic the listening for that information on the scheduler side
			getRegist.receive(registration);
			getRegist.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		assertTrue(regist[4] == (byte)10);
		
		//mimic sending registration confirmed from scheduler side
		byte[] sampleConfirmation = new byte[] {1, 12, 1};
		try {
			DatagramPacket doorClosePckt = new DatagramPacket(sampleConfirmation, sampleConfirmation.length,
					InetAddress.getLocalHost(),10);
			DatagramSocket doorCloseSocket = new DatagramSocket();
			doorCloseSocket.send(doorClosePckt);
			doorCloseSocket.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		//mimic sneding door close from scheduler side
		byte[] doorCloseMsg = new byte[] {2};
		try {
			DatagramPacket doorClosePckt = new DatagramPacket(doorCloseMsg,doorCloseMsg.length,
					InetAddress.getLocalHost(),10);
			DatagramSocket doorCloseSocket = new DatagramSocket();
			doorCloseSocket.send(doorClosePckt);
			doorCloseSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			fail("Did not send ");
			e.printStackTrace();
		} 
		
		//mimic sending a request from scheduler side 
		//request being sent is floor = 2, carButton = 7, direction = Up
		byte[] request = new byte[] {4, 2, 7, 1};	
		try {
			DatagramPacket doorClosed = new DatagramPacket(request, request.length,
					InetAddress.getLocalHost(), 10);
			DatagramSocket doorClosedSocket = new DatagramSocket();
			doorClosedSocket.send(doorClosed);
			doorClosedSocket.close();
		}
		catch(IOException e)
		{
			fail("Did not Receive");
			e.printStackTrace();
		}
		
		//listening for elevator Ready message
		try {
			DatagramPacket doorClosed = new DatagramPacket(request, request.length);
			DatagramSocket doorClosedSocket = new DatagramSocket(12);
			doorClosedSocket.receive(doorClosed);
			doorClosedSocket.close();
			System.out.println("Received ready with " + Arrays.toString(request));
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		//sending move
		byte[] move = new byte[] {6};
		
		try
		{
			DatagramPacket doorClosed = new DatagramPacket(move, move.length,
					InetAddress.getLocalHost(), 10);
			DatagramSocket doorClosedSocket = new DatagramSocket();
			doorClosedSocket.send(doorClosed);
			doorClosedSocket.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		int i = 0;
		
		//listening for 8s notifications
		while(i < 3)
		{
			byte[] sensorNot = new byte[4];
			try
			{
				DatagramPacket sesnor = new DatagramPacket(sensorNot, sensorNot.length);
				DatagramSocket sensor = new DatagramSocket(12);
				sensor.receive(sesnor);
				sensor.close();
				System.out.println("Receieved 2s not with " + Arrays.toString(sensorNot));
				i++;
			} catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		
        byte[] moved = new byte[] {19};
		
		try
		{
			DatagramPacket doorClosed = new DatagramPacket(moved, moved.length,
					InetAddress.getLocalHost(), 10);
			DatagramSocket doorClosedSocket = new DatagramSocket();
			doorClosedSocket.send(doorClosed);
			elev.stop();
			doorClosedSocket.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		
		
		
	}*/
}
