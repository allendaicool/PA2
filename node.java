//package routingTable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Map.Entry;


public class node {
    
	public static int route_update = 1;
	public static int link_change = 2;
	public static int transfer_file = 3;
	
	//private HashMap<String,Integer> neighborIPPort  = new HashMap<String,Integer>();
	private List<IpPort> neighborIPPort = new ArrayList<IpPort>();
	private HashMap<IpPort,Integer> neighborCost = new HashMap<IpPort, Integer>();
	private HashMap<IpPort,Integer> DV = new HashMap<IpPort, Integer>();
	private HashMap<IpPort, HashMap<IpPort,Integer>> neigborDV = new HashMap<IpPort, HashMap<IpPort, Integer>>();
	// <destination, link>?
	public HashMap<IpPort,IpPort> destLink = new HashMap<IpPort,IpPort>();
	
	private int listenOnPort;
	private int timeOutValue;
	private String ipAddress;
	public IpPort itself;
	public boolean changeSignal = false;
	public node( ){
		try {
			this.ipAddress = (InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	public List<IpPort> getneighborIPPort (){
		return this.neighborIPPort;
	}

	public HashMap<IpPort,Integer> getNeighborCost (){
		return this.neighborCost;
	}

	public HashMap<IpPort,Integer> getDV (){
		return this.DV;
	}

	public HashMap<IpPort, HashMap<IpPort,Integer>> getNeighborDV(){
		return this.neigborDV;
	}

	public int getPort(){
		return this.listenOnPort;
	}

	public int getTimeOUt(){
		return this.timeOutValue;
	}

	public void setListenOnPort(int listenOnPort){
		this.listenOnPort = listenOnPort;		
		itself = new IpPort(this.ipAddress,this.listenOnPort);
		// add itself to the distance vector the cost is 0
		this.DV.put(itself,0);
		this.neighborCost.put(itself, 0);

	}

	public void setTimeOutValue(int timeOutValue){
		this.timeOutValue = timeOutValue;
	}

	public void initialize(String neighbor, int port, int cost){
		IpPort temp = new IpPort(neighbor, port);
		this.neighborIPPort.add(temp);
		this.neighborCost.put(temp, cost);


		this.DV.put(temp, cost);
		this.destLink.put(temp, temp);
		this.neigborDV.put(temp, new HashMap<IpPort, Integer>());
		this.neigborDV.get(temp).put(temp,0);
	}


	public boolean updateNeighborDV(String neighbor,int port, HashMap<IpPort,Integer> neighborDv){
		IpPort temp = new IpPort(neighbor, port);
		int index = this.neighborIPPort.indexOf(temp);
		if(index != -1){
			temp = this.neighborIPPort.get(index);
		}
		else{
			return false;
		}
		this.neigborDV.put(temp, neighborDv);
		updateOwnDV(neighborDv,neighbor, port);
		
		return true;
	}

	private boolean updateOwnDV(HashMap<IpPort,Integer> neighborDV, String ip, int port){
		IpPort temp = new IpPort(ip, port);
		int index = this.neighborIPPort.indexOf(temp);
		if(index != -1){
			temp = this.neighborIPPort.get(index);
		}
		else{
			return false;
		}
		List<IpPort> allNode = new ArrayList<IpPort>();
		allNode.addAll(this.neighborIPPort);
		
		Iterator<Entry<IpPort, Integer>> it = neighborDV.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<IpPort, Integer> pair = it.next();
			if(!allNode.contains(pair.getKey()))
				allNode.add(pair.getKey());
		}
		allNode.remove(this.itself);
		for(IpPort iter : allNode){
			
			int min  = Integer.MAX_VALUE;
			for ( IpPort neighbor:this.neighborIPPort){
				if (!this.neigborDV.get(neighbor).containsKey(iter)){
					continue;
				}
				else{
					int neighborDistance = this.neighborCost.get(neighbor);
					if(neighborDistance == Integer.MAX_VALUE){
						continue;
					}
					int dvDistance = this.neigborDV.get(neighbor).get(iter);
					if(dvDistance == Integer.MAX_VALUE){
						continue;
					}
					
					if((neighborDistance+dvDistance) < min){
						
						min = neighborDistance+dvDistance;
						this.destLink.put(iter, neighbor);	
					}
				}  
			}
			
			if( !this.DV.containsKey(iter) || min < this.DV.get(iter)){
				this.changeSignal = true;
			}
			if(min == Integer.MAX_VALUE){
				System.out.println("bug here !!!!!!!!!!!!!!!!!");
				System.out.println("dv iport is " + iter.port);
				System.out.println("itself is " + this.itself.port);
			}
			this.DV.put(iter, min);
		}
		return true;
	}
}
