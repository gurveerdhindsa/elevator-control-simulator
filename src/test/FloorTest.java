package test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.junit.Test;

import main.Elevator;
import main.Floor;
import main.FloorRequest;
public class FloorTest {

	Timestamp timestamp = null;
	Date parsedDate = null;
	
	@SuppressWarnings("deprecation")
	@Test 
	// start listening to the Floor thread
	public void testRegistration()
	{
		FloorRequest fr = new FloorRequest();
		Thread floorThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				Floor floor = new Floor();
				floor.start();
			}
		});
		
		byte[] regist = new byte[300];
		
		try {
			DatagramPacket registration = new DatagramPacket(regist,regist.length);
			DatagramSocket getRegist = new DatagramSocket(45);
			floorThread.start();
			getRegist.receive(registration);
			// read in buffer 
			//System.out.println(Arrays.toString(regist));
			byte[] actualMsg = Arrays.copyOfRange(regist, 1, registration.getLength());
			//System.out.println(Arrays.toString(actualMsg));
			System.out.println(actualMsg.length);
			FloorRequest result = (FloorRequest) FloorRequest.getObjectFromBytes(actualMsg);
			assertTrue(result.getFloor() == 2);
			assertTrue(result.getCarButton() == 4);
			SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss.SSS");
		    
			try {
				parsedDate = dateFormat.parse("14:05:15.0");
			} catch (ParseException e) {
			
				e.printStackTrace();
			}
		    timestamp = new Timestamp(parsedDate.getTime());
			assertTrue(result.getTimestamp().equals(timestamp));
			assertTrue(result.getFloorButton().equals("Up"));
			
			getRegist.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		assertTrue(regist[0] == (byte)0);
		
		
	}
	// test case to receive packet that elevator is moving and sending every 8 seconds that it has approached the floor
	/*
	 * @SuppressWarnings("deprecation")
	 * 
	 * @Test public void receiveFloor() throws IOException {
	 * 
	 * 
	 * Thread scheduler = new Thread(new Runnable() {
	 * 
	 * @Override public void run() { Floor floor = new Floor(); floor.run(); }
	 * 
	 * }); byte[] regist = new byte[300]; try { scheduler.start(); DatagramSocket
	 * receiveSocket = new DatagramSocket(45); DatagramPacket registration = new
	 * DatagramPacket(regist,regist.length); receiveSocket.receive(registration);
	 * 
	 * 
	 * } catch (SocketException e) {
	 * 
	 * e.printStackTrace(); } assertTrue(regist[0] == (byte)2);
	 * 
	 * }
	 * 
	 */
		
		
	

	
}
	


