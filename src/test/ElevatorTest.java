package test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import main.Elevator;

public class ElevatorTest {
	
	
	private Elevator elev;
	private int assignedPort;
	private int elevPort;
	
	@Before
	public void setUp()
	{
		this.elevPort = 10;
		elev = new Elevator(this.elevPort);
		this.assignedPort = 46;
	}
	
	@After
	public void cleanUp()
	{
		this.elev.stop();
	}
	
	/**
	 * Thread sleeps for time seconds
	 * @param time int
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
	 *Mimics the scheduler sending
	 *a message instructing an elevator 
	 *to stat moving 
	 */
	public void sendMove() {
		//mimic sending of move message from scheduler to elevator
		byte[] elevMoveMsg = new byte[] {6};
		try
		{
			DatagramSocket elevMoveSckt = new DatagramSocket();
			DatagramPacket elevMovePckt = new DatagramPacket(elevMoveMsg,elevMoveMsg.length,
					InetAddress.getLocalHost(), this.elevPort);
			elevMoveSckt.send(elevMovePckt);
			elevMoveSckt.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Mimics the scheduler sending a floor request
	 * to an elevator instance
	 */
	public void sendRequest()
	{
		//make up a request 
		byte[] newRequest = new byte[] {4, 8, 3, -1};  // going down from floor 8 to 3
		
		//send fake request {4, 8, 3, -1}
		try {
			DatagramPacket requestPckt = new DatagramPacket(newRequest,newRequest.length,
					InetAddress.getLocalHost(),this.elevPort);
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
	 * Mimics a personalized scheduler thread
	 * notifying an elevator about which port
	 * number to send all further communications
	 * with scheduler to
	 */
	public void sendRegistrationConfirmed()
	{
		//send a registrationConfirmation msg
		byte[] confirmation = new byte[] {1, (byte)this.assignedPort, 0};
		try {
			DatagramPacket confirmationPckt = new DatagramPacket(confirmation,confirmation.length,
					InetAddress.getLocalHost(),this.elevPort);
			DatagramSocket sendConfirm = new DatagramSocket();
			sendConfirm.send(confirmationPckt);
			sendConfirm.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			fail("Did not send ");
			e.printStackTrace();
		}
	}
	
	/**
	 *Mimic the Scheduler instructing
	 *an elevator to close it's doors
	 */
	public void sendDoorClose()
	{
		//mimic sending door close from scheduler side
		byte[] doorCloseMsg = new byte[] {2};
		try {
			DatagramPacket doorClosePckt = new DatagramPacket(doorCloseMsg,doorCloseMsg.length,
					InetAddress.getLocalHost(),this.elevPort);
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
	 * Test that an elevator instance correctly
	 * sends a message making itself known 
	 * to the scheduler subsystem after initialization
	 */
	@Test
	public void testRegistration()
	{
		System.out.println("###Starting Test: testRegistration#####");
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
		assertTrue((int)regist[4] == this.elevPort);
		
		this.waitms(100);
		
		System.out.println("####EndingTest:testRegistrationt####\n\n\n\n");
	}
	
	/**
	 * Test that an elevator instance correctly receives
	 * a request, process it and notify scheduler that 
	 * it is ready to start moving
	 */
	@Test
	public void testReceiveRequest()
	{
		System.out.println("####Starting Test: testReceiveRequest#####");
		Thread elevThread = new Thread(this.elev);
		
		//make elevator start listening
		elevThread.start();
		
		this.sendRegistrationConfirmed();
		
		this.waitms(200);
		
		//validate elevator state for confirmation received 
		assertTrue(this.elev.getAssignedPort() == this.assignedPort);
		assertTrue(this.elev.getDirection() == -1);    //this elevator will initially 
		                                               //take requests from downRequests list
		this.sendRequest();
		
		//listen for ready 
		byte[] readyMsg = new byte[100]; 
		try {
			DatagramSocket readySckt = new DatagramSocket(this.assignedPort);
			DatagramPacket readyPckt = new DatagramPacket(readyMsg, readyMsg.length);
			readySckt.receive(readyPckt);
			readySckt.close();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} 
		
		// when you get ready assert that info is correct
		// verify data[1]==19, data[2] == 8, data[3] == 1(up to answer down req)
		assertTrue(this.elev.getCurrentFloor() == 0);
		assertTrue(this.elev.getDestinationFloor() == 8);
		assertTrue(this.elev.getDirection() == 1);
		
		this.waitms(50);
		System.out.println("####EndingTest:testReceiveRequest####\n\n\n\n");
		
	}
	/**
	 * Test that an elevator instance 
	 * correctly receives a close door 
	 * msg/command from scheduler and 
	 * proceed to close it's doors and
	 * notify scheduler that they are now 
	 * closed 
	 */
	@Test
	public void testDoorClose()
	{
		System.out.println("###Starting Test:testDoorClose#####");
		Thread elevThread = new Thread(elev);
		elevThread.start();
		
		this.sendRegistrationConfirmed();
		this.waitms(20);
		this.sendDoorClose();
		
		//listen confirmation of door closed from elevator to scheduler
		byte[] doorClosedConfirmation = new byte[100]; 
		try {
			DatagramSocket doorCloseConfirmationSckt = new DatagramSocket(this.assignedPort);
			DatagramPacket doorCloseConfirmationPckt = new DatagramPacket(doorClosedConfirmation, 
					doorClosedConfirmation.length);
			doorCloseConfirmationSckt.receive(doorCloseConfirmationPckt);
			doorCloseConfirmationSckt.close();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} 
		
		//verify elevator door is closed
		assertTrue(this.elev.getDoorsOpen() == false);
		assertTrue(doorClosedConfirmation[0] == 3);
		
		this.waitms(20);
		System.out.println("####EndingTest:testDoorClose####\n\n\n\n");
	}
	
	/**
	 * Testing the sequence of an elevator
	 * 1)making itself known to the scheduler subsytem.
	 * 2)Scheduler subsystem assigning it a new port to
	 *   send all new messages
	 * 3)An elevator receiving the close door command 
	 *  and closing it's doors and notifying the 
	 *  scheduler subsystem that doors are now closed
	 * 4)An elevator receiving a request from the
	 *   scheduler subystem, correctly processing it
	 *   and notifying the scheduler that it is ready 
	 *   to start moving
	 * 5)An elevator receives a move msg/command from
	 *   the scheduler subsystem and starts moving 
	 *   whilst also notifying the scheduler every
	 *   time it approaches a new floor
	 * 6)An elevator notifies the scheduler 
	 *   that it is approaching floor 1 and 
	 *   the scheduler stops it at floor
	 *   to answer a pending request
	 */
	@Test
	public void testElevatorMoveAndStopped()
	{
		System.out.println("###StartingTest:testElevatorMoveAndStopped#####");
		Thread elevThread = new Thread(elev);
		elevThread.start();
		
		this.sendRegistrationConfirmed();
		this.waitms(1);
		this.sendDoorClose();//make elevator close it's  doors
		this.waitms(1);
		this.sendRequest(); //send a request
		this.waitms(1);
		this.sendMove(); // tell elevator to move
		this.waitms(1);
		
		//assertTrue(this.elev.isSensorThreadExecuting() == true);
		//assertTrue(this.elev.isMotorThreadExecuting() == true);
		
		this.waitms(2200); // wait till it is approaching 1st floor
		//mimic sending of stop message from scheduler to elevator
		byte[] stopData = new byte[] {8};
		try {
			DatagramSocket elevatorStopSckt = new DatagramSocket();
			DatagramPacket elevatorStopPckt = new DatagramPacket(stopData, stopData.length, 
					InetAddress.getLocalHost(),this.elevPort);
			elevatorStopSckt.send(elevatorStopPckt);
			elevatorStopSckt.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.waitms(20);
		//verify that elevator is stopped
		assertTrue(this.elev.getDoorsOpen() == true);
		assertTrue(this.elev.getCurrentFloor() == 1);
		assertTrue(this.elev.getDirection() == 1);
		assertTrue(this.elev.getSensorCount() == 7);
		assertTrue(this.elev.isMotorThreadExecuting() == false);
		assertTrue(this.elev.isSensorThreadExecuting() == false);
		this.waitms(20);
		
		System.out.println("####EndingTest:testElevatorMoveAndStopped####\n\n\n\n");
	}
	
}

