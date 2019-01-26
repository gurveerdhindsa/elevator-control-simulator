package test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;
import main.Elevator;

public class ElevatorTest {

	
	private Elevator elev = new Elevator(23);
	@Before
	public void initializeElevator()
	{
		
		elev.start();
	}
	
	@After
	public void cleanUp()
	{
		elev.exit();
	}
	
	@Test
	public void test() {
		fail("Not yet implemented");
	}
	
	/*@Test
	public void testRegistration()
	{
		byte registration[] = new byte[100];
		
		try {
			DatagramPacket registerPacket = new DatagramPacket(registration, registration.length);
			DatagramSocket registerSocket = new DatagramSocket(69);
			
			registerSocket.receive(registerPacket);
			registerSocket.close();
			
			System.out.println("Elevator register received");
			
			assertTrue((int)registration[4] == 23);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			String exception = e.getMessage();
			fail("Did not receive registration packet\n Exception Message:\n" + exception + "\n");
			e.printStackTrace();
		}
		
		
	}*/
	
	@Test
	public void testReceiveRequest()
	{
		byte[] request = new byte[] {0, 2, 0, 5};
		try {
			DatagramPacket requestPckt = new DatagramPacket(request,request.length,
					InetAddress.getLocalHost(),23);
			DatagramSocket sendReq = new DatagramSocket();
			sendReq.send(requestPckt);
			sendReq.close();
			
			Thread.sleep(21000);
			assertTrue(elev.getCurrentFloor() == 2);
			assertTrue(elev.getDestFloor() == 5);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			fail("Did not send ");
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	

}
