//package routingTable;

import java.util.List;
/*
this thread is used to check if the node receive the update message from neighbor within time out
*/
public class checkTimeOut  implements Runnable{


	public Thread t;
	int timeOUtValue;
	node host;

	public checkTimeOut (int timeout , node host){
		this.timeOUtValue = 3*timeout;
		this.host = host;
	}
	public void start ()
	{
		if (t == null)
		{
			t = new Thread (this);
			t.start ();
		}
	}


	@Override
	public void run() {
		// TODO Auto-generated method stub
		long startTime = System.currentTimeMillis();
		while(true){
			long currentTime = System.currentTimeMillis();
			
			// if the time out expires, check the update message from neighbors
			if(((currentTime-startTime)/1000)> this.timeOUtValue){
				startTime = currentTime;
				List<IpPort> neighborIPPort = this.host.getneighborIPPort();
				for (IpPort neighbor : neighborIPPort){
					// if neighbor is already dead, don't need to add it to dead list again
					if(!this.host.linkDownList.contains(neighbor)){
						if(this.host.neighborDVUpdateTime.get(neighbor) == null){
							this.host.linkDownList.add(neighbor);
							this.host.updateLinkCost(neighbor, Double.MAX_VALUE);

						}
						else if((Math.abs((this.host.neighborDVUpdateTime.get(neighbor) - currentTime)/1000)) > this.timeOUtValue){
							this.host.linkDownList.add(neighbor);
							this.host.updateLinkCost(neighbor, Double.MAX_VALUE);
						}

					}
				}
			}

		}
	}




}
