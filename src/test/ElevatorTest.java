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
	
	public void sendRequest()
	{
		//make up a request 
		byte[] newRequest = new byte[] {4, 8, 3, -1}; 
		
		//send fake request {4, 8, 3, -1}
		try {
			DatagramPacket requestPckt = new DatagramPacket(newRequest,newRequest.length,
					InetAddress.getLocalHost(),10);
			DatagramSocket sendRequest = new DatagramSocket();
			sendRequest.send(requestPckt);
			sendRequest.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			fail("Did not send ");
			e.printStackTrace();
		}
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
		byte[] confirmation = new byte[] {1, 46, 0};
		try {
			DatagramPacket confirmationPckt = new DatagramPacket(confirmation,confirmation.length,
					InetAddress.getLocalHost(),10);
			DatagramSocket sendConfirm = new DatagramSocket();
			sendConfirm.send(confirmationPckt);
			sendConfirm.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			fail("Did not send ");
			e.printStackTrace();
		}
		
		try {
			Thread.sleep(400);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//validate elevator state for confirmation received 
		assertTrue(this.elev.getAssignedPort()== 46);
		assertTrue(this.elev.getDirection() == -1);    //this elevator will initially take requests from downRequests list
		
		this.sendRequest();
		
		//listen for ready 
		byte[] readyMsg = new byte[100]; 
		try {
			DatagramSocket readySckt = new DatagramSocket(46);
			DatagramPacket readyPckt = new DatagramPacket(readyMsg, readyMsg.length);
			System.out.println("Scheduler " + Thread.currentThread().getName() + 
					" Waiting for message from elevator on Port " + this.elev.getAssignedPort());
			readySckt.receive(readyPckt);
			readySckt.close();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} 
		
		
		// when you get ready assert that info is correct
		// verify data[1]==19, data[2] == 8, data[3] == 1(up to answer down req)
		assertTrue(this.elev.getCurrentFloor()== 0);
		assertTrue(this.elev.getDestinationFloor()== 8);
		assertTrue(this.elev.getDirection()== 1);
		
	}
	

	/*
	@Test
	public void testDoorCloseMsg()
	{
		Thread elevThread = new Thread(elev);
		elevThread.start();
		
		//mimic sending door close from scheduler side
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
		
		//sending confirmation of door closed from elevator to scheduler
		byte[] doorClosedConfirmation = new byte[100]; 
		try {
			DatagramSocket doorCloseConfirmationSckt = new DatagramSocket(46);
			DatagramPacket doorCloseConfirmationPckt = new DatagramPacket(doorClosedConfirmation, doorClosedConfirmation.length);
			System.out.println("Scheduler " + Thread.currentThread().getName() + 
					" Waiting for message from elevator on Port " + this.elev.getAssignedPort());
			doorCloseConfirmationSckt.receive(doorCloseConfirmationPckt);
			doorCloseConfirmationSckt.close();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} 
		
		//verify elevator door is closed
		assertTrue(this.elev.getDoorsOpen()==false);
	}
	
	@Test
	public void testElevatorStopped()
	{
		
		Thread elevThread = new Thread(elev);
		elevThread.start();
		
		
		//mimic sending of stop message from scheduler to elevator
		byte[] stopData = new byte[] {8};
		
		try {
			DatagramSocket elevatorStopSckt = new DatagramSocket(46);
			DatagramPacket elevatorStopPckt = new DatagramPacket(stopData, stopData.length, InetAddress.getLocalHost(),10);
			elevatorStopSckt.send(elevatorStopPckt);
			System.out.println(Thread.currentThread().getName() + 
					" Sent stop");
			elevatorStopSckt.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//sending confirmation of elevator stopped from elevator to scheduler
		byte[] elevatorStoppedConfirmation = new byte[100]; 
		try {
			DatagramSocket elevStoppedConfirmationSckt = new DatagramSocket(46);
			DatagramPacket elevStoppedConfirmationPckt = new DatagramPacket(elevatorStoppedConfirmation, elevatorStoppedConfirmation.length);
			System.out.println("Scheduler " + Thread.currentThread().getName() + 
					" Waiting for message from elevator on Port " + this.elev.getAssignedPort());
			elevStoppedConfirmationSckt.receive(elevStoppedConfirmationPckt);
			elevStoppedConfirmationSckt.close();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} 
		
		//verify that elevator is stopped
		//assertTrue that elevator motorThread and sensorThread is interrupted
		assertTrue(this.elev.isMotorInterrupted()==true);
		assertTrue(this.elev.isSensorInterrupted()==true);
	}
	
	@Test
	public void testElevatorMove()
	{
		Thread elevThread = new Thread(elev);
		elevThread.start();
		
		//mimic sending of move message from scheduler to elevator
		byte[] elevMoveMsg = new byte[] {6};
		try
		{
			DatagramSocket elevMoveSckt = new DatagramSocket(46);
			DatagramPacket elevMovePckt = new DatagramPacket(elevMoveMsg,elevMoveMsg.length,
					InetAddress.getLocalHost(), 10);
			elevMoveSckt.send(elevMovePckt);
			System.out.println(Thread.currentThread().getName() + 
					" Sent move");
			elevMoveSckt.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		//verify that elevator is moving
		//assertTrue that elevator motorThread and sensorThread is not interrupted
		assertTrue(this.elev.isMotorInterrupted()==false);
		assertTrue(this.elev.isSensorInterrupted()==false);
		
	}


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
