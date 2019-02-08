package main;

import java.util.List;

public class SchedulerElevators implements Runnable{
	
	int currentFloor;
	int destinationFloor;
	int portNumber;
	int initialList;
	List<FloorRequest> up;
	List<FloorRequest> down;
	FloorRequest currentRequest;
	int topFloor;
	int direction; // 1 is up -1 is down
	
	public SchedulerElevators(List<FloorRequest>up, List<FloorRequest>down)
	{
		this.up = up;
		this.down = down;
	}

	public FloorRequest checkDownRequests(int floor)
	{
		synchronized(this.down)
		{
			for(int i = floor; i >= 0; i--)
			{
				FloorRequest req = this.down.get(i);
				if(req.timestamp != null)
				{
					return req;
				}
			}
			return null;
		}
	}
	
	public FloorRequest getUpRequest(int floor)
	  {
		synchronized(this.up)
		{
			if(this.up.get(floor).timestamp != null)
			{
				// get request
		        // set this.up[floor]  to contain no request
				return req;
			}
		}
	  }
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
			// dunno how to clear 
			return req;
		}
	}
	
	
	public void workerFunction()
	{
		while(true)
		{
			byte[] msg = new byte[100];
			//listen for elevator message 
			
			
			// 8s notification
			if(msg[0] == 9)
			{
				//updateCurrentFloor();
				
				//if special case (if elevator was going Up but then gets a down Request it will set a bit till it gets to that floor)
				   //do nothing
				//else if up
				  //handleUp8s()
				//else
				  //handleDown8s()
			}
			
			//Arrival notification
			else if(msg[0] == 8)
			{
				/**
				 * FloorRequest req;
				 * if currentFloor == topMostFloor
				 * {
				 *     req = checkDownRequest()
				 *     if req == null 
				 *         checkInitialDown(currentFloor)
				 * }
				 * else if(direction == 1 && msg[pendingIndicator] != 1)
				 * {
				 *    req = checkUpRequests(currentFloor);
				 *    if req == null
				 *    {
				 *       req = checkDownRequest(topfloor)
				 *        if req == null:
				 *             checkInitialUp(currentFloor);
				 *    }
				 *    
				 *    if there is somekind of request do process below
				 *    close door 
				 *    this.currentRequest = req;
				 *        
				 * }
				 * 
				 * else if(direction == 1 && msg[pendingIndicator] == 1)
				 * {
				 *     this.currentRequest = getUpRequest(this.currentFloor);
				 *     closedoor
				 *     wait for door closed 
				 *      
				 * }
				 */
				
			}
			
			//door closed
			else if(msg[0] == 3)
			{
				/**
				 * if this.currentRequest != null
				 * {
				 *   sendRequest() ->  
				 *   
				 * }
				 * else
				 * {sendMove()}
				 */
			}
			
			//elevatorReady
			else if(msg[0] == 1)
			{
				//update direction, destination, specialCase, currentfloor
				//sendMove()
				
			}
			//elevator stopped message 
			else
			{
				//sendDoorClose()
			}
		}
	}
	
	
	public void waitUp()
	{
		synchronized(this.up)
		{
			
		}
	}
	public void handleUp8s()
	{
		FloorRequest req = null;
		
		if(this.currentFloor != this.destinationFloor)
		{
			req = getUpRequest(this.currentFloor);
		}
		
		if(req != null)
		{
			//sendStop();
		}
		
	}
	public void updateCurrentFloor()
	{
		//if direction == up
		     //currentFloor++
		//else
		   //currentFloor--
	}
	public void start()
	{
		//FloorRequest req = initialList == 1 ? checkInitialUp() : checkInitialDown();
		//sendDoorClose
		
		//listen for message from elevator
		
		// get it 
		//send req
		
		// call workerFunction()
		
		
	}
	
	
	public void sendDoorClose()
	{
		//socket, packet and send 
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}
