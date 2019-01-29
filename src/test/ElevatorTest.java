package test;

import static org.junit.Assert.*;

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
	
	
	@Test 
	public void testRegistration()
	{
		Thread elevThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				Elevator elev = new Elevator(33);
				elev.run();
			}
		});
		
		byte[] regist = new byte[50];
		
		try {
			DatagramPacket registration = new DatagramPacket(regist,regist.length);
			DatagramSocket getRegist = new DatagramSocket(69);
			elevThread.start();
			getRegist.receive(registration);
			getRegist.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		assertTrue((int)regist[0] == 0);
		assertTrue((int)regist[4] == 33);
		
	}
	
	
	@Test
	public void testReceiveRequest()
	{
		Thread elevThread = new Thread(new Runnable()
				{
			@Override
			public void run()
			{
				Elevator elev = new Elevator(23);
				elev.run();
			}
				});
		
		elevThread.start();
		byte[] request = new byte[] {0, 2, 0, 5};
		try {
			DatagramPacket requestPckt = new DatagramPacket(request,request.length,
					InetAddress.getLocalHost(),23);
			DatagramSocket sendReq = new DatagramSocket();
			sendReq.send(requestPckt);
			sendReq.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			fail("Did not send ");
			e.printStackTrace();
		} 
		
		byte[] arrival = new byte[50];
		try {
			Thread.sleep(19500);
			
			DatagramSocket getArrival = new DatagramSocket(69);
			
			DatagramPacket pck = new DatagramPacket(arrival,arrival.length);
			getArrival.receive(pck);
			getArrival.close();
			
			
		} catch (InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		assertTrue((int)arrival[0] == 4);
		assertTrue((int)arrival[1] == 1);
		assertTrue((int)arrival[2] == 5);
		assertTrue((int)arrival[3] == 2);
		
	}
	
	/*@Test
	public void testSendDoorCloseMsg()
	{
		Thread elevThread = new Thread(new Runnable()
		{
		@Override
		public void run()
		{
			Elevator elev = new Elevator(23);
			elev.run();
		}
			});
	
		elevThread.start();
		byte[] doorCloseMsg = new byte[] {2};
		try {
			DatagramPacket doorClosePckt = new DatagramPacket(doorCloseMsg,doorCloseMsg.length,
					InetAddress.getLocalHost(),23);
			DatagramSocket doorCloseSocket = new DatagramSocket();
			doorCloseSocket.receive(doorClosePckt);
			doorCloseSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			fail("Did not send ");
			e.printStackTrace();
		} 
		
		assertTrue((byte)doorCloseMsg[0] == 2);

		}*/

}
