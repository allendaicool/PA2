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
	public static int link_down = 2;
	public static int transfer_file = 5;
	public static int link_change = 4;
	public static int link_up = 3;
	public HashSet<IpPort> linkDownList = new HashSet<IpPort>();

	private HashMap<IpPort,Double> neighborConfigureCost = new HashMap<IpPort,Double>();
	//private HashMap<String,Integer> neighborIPPort  = new HashMap<String,Integer>();
	private List<IpPort> neighborIPPort = new ArrayList<IpPort>();
	private HashMap<IpPort,Double> neighborCost = new HashMap<IpPort, Double>();
	private HashMap<IpPort,Double> DV = new HashMap<IpPort, Double>();
	private HashMap<IpPort, HashMap<IpPort,Double>> neigborDV = new HashMap<IpPort, HashMap<IpPort, Double>>();


	public HashMap<IpPort, Long> neighborDVUpdateTime = new HashMap<IpPort,Long>();

	// <destination, link>?
	public HashMap<IpPort,IpPort> destLink = new HashMap<IpPort,IpPort>();

	//public IpPort linkDownNode;
	private int listenOnPort;
	private int timeOutValue;
	private String ipAddress;
	public IpPort itself;
	public boolean changeSignal = false;
	public boolean linkdown = false;
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

	public HashMap<IpPort,Double> getNeighborCost (){
		return this.neighborCost;
	}

	public HashMap<IpPort,Double> getDV (){
		return this.DV;
	}

	public HashMap<IpPort, HashMap<IpPort,Double>> getNeighborDV(){
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
		this.DV.put(itself,(double) 0);
		this.neighborCost.put(itself, (double) 0);

	}

	public void setTimeOutValue(int timeOutValue){
		this.timeOutValue = timeOutValue;
	}

	public void initialize(String neighbor, int port, double cost){
		IpPort temp = new IpPort(neighbor, port);
		this.neighborIPPort.add(temp);
		this.neighborCost.put(temp, cost);
		this.neighborConfigureCost.put(temp, cost);
		this.DV.put(temp, cost);
		this.destLink.put(temp, temp);
		this.neigborDV.put(temp, new HashMap<IpPort, Double>());
		this.neigborDV.get(temp).put(temp,(double) 0);

	}



	public IpPort findNeighbor(String ip, int port){
		List<IpPort> setting =this.getneighborIPPort();
		for(IpPort temp:setting){
			if (temp.ip.equals(ip) && temp.port == port){
				return temp;
			}
		}
		return null;
	}

	public boolean updateNeighborDV(String neighbor,int port, HashMap<IpPort,Double> neighborDv){
		IpPort temp = this.findNeighbor(neighbor,port);
		if(temp == null){
			return false;
		}


		this.neighborDVUpdateTime.put(temp, System.currentTimeMillis());

		this.neigborDV.put(temp, neighborDv);
		if(this.linkDownList.contains(temp)){
			this.linkDownList.remove(temp);
			this.getNeighborCost().put(temp, this.neighborConfigureCost.get(temp));
		}
		updateOwnDV(neighborDv,neighbor, port);

		return true;
	}

	
	public boolean linkChange(IpPort neighbor, double cost){
		updateLinkCost(neighbor,cost);
		return true;
	}
	
	public boolean linkUp(IpPort neighbor){
		
		linkDownList.remove(neighbor);
		updateLinkCost(neighbor,this.neighborConfigureCost.get(neighbor));
		return true;

	}
	public boolean linkdown(IpPort neighbor,double cost){		
		linkDownList.add(neighbor);
		updateLinkCost(neighbor,cost);
		return true;
	}

	public boolean updateLinkCost(IpPort neighbor, double cost){
		this.getNeighborCost().put(neighbor, cost);
		List<IpPort> allNode = new ArrayList<IpPort>();
		allNode.addAll(this.DV.keySet());
		allNode.remove(this.itself);
		updateOwnDVAlgo(allNode);
		return true;
	}

	private void updateOwnDVAlgo(List<IpPort> allNode){
		
		for(IpPort iter : allNode){

			double min  = Double.MAX_VALUE;
			for ( IpPort neighbor:this.neighborIPPort){
				if (!this.neigborDV.get(neighbor).containsKey(iter)){
					continue;
				}
				else{
					double neighborDistance = this.neighborCost.get(neighbor);
					if(neighborDistance == Double.MAX_VALUE){
						continue;
					}
					double dvDistance = this.neigborDV.get(neighbor).get(iter);
					if(dvDistance == Double.MAX_VALUE){
						continue;
					}

					if(   ((Double.MAX_VALUE -neighborDistance) >dvDistance) &&(neighborDistance+dvDistance) < min){

						min = neighborDistance+dvDistance;
						this.destLink.put(iter, neighbor);	
						if(this.itself.port == 4115 && iter.port == 4118){
								if(min == 30){
								System.out.println("link path is " + this.destLink.get(iter).port);
							}
						}
					}
				}  
			}
			
			

			if( !this.DV.containsKey(iter) || min != this.DV.get(iter)){
				this.changeSignal = true;
			}
			if(min == Double.MAX_VALUE){
				this.destLink.put(iter, null);
			}
			
			this.DV.put(iter, min);
		}

	}

	private boolean updateOwnDV(HashMap<IpPort,Double> neighborDV, String ip, int port){
		IpPort temp = new IpPort(ip, port);
		int index = this.neighborIPPort.indexOf(temp);
		if(index != -1){
			temp = this.neighborIPPort.get(index);
		}
		else{
			return false;
		}
		List<IpPort> allNode = new ArrayList<IpPort>();
		allNode.addAll(this.DV.keySet());

		Iterator<Entry<IpPort, Double>> it = neighborDV.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<IpPort, Double> pair = it.next();

			if(!allNode.contains(pair.getKey()))
				allNode.add(pair.getKey());
		}

		allNode.remove(this.itself);

		updateOwnDVAlgo(allNode);
		return true;
	}
}
