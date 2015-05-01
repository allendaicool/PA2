//package routingTable;

import java.util.List;

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
			if(((currentTime-startTime)/1000)> this.timeOUtValue){
				startTime = currentTime;
				List<IpPort> neighborIPPort = this.host.getneighborIPPort();
				for (IpPort neighbor : neighborIPPort){
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
