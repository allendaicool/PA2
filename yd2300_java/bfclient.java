//package routingTable;
import java.io.*;
import java.net.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class bfclient {
	public static int packetID = 0 ;

	public static void main(String[] args){


		// read the configuration file
		String fileName = args[0];
		System.out.println("filename is " + fileName);
		node temp = new node();
		nodeConfig(temp, fileName);
		System.out.println("localport is " + temp.getPort());
		System.out.println("listening ip is " + temp.itself.ip);
		//checkDV(temp);


		ClientSocket client=null;

		try {
			DatagramSocket serverSocket = new DatagramSocket(temp.getPort());
			ServerSocket server = new ServerSocket(serverSocket,temp);
			DatagramSocket clientSocket = new DatagramSocket();
			client = new ClientSocket(temp,clientSocket);
			checkTimeOut checkout = new checkTimeOut(temp.getTimeOUt(),temp);


			server.start();
			client.start();
			checkout.start();

		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		boolean closeSignal = false;
		while(!closeSignal){
			BufferedReader inFromUser =
					new BufferedReader(new InputStreamReader(System.in));
			String sentence =null;
			try {
				sentence = inFromUser.readLine().trim();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(sentence.toLowerCase().equals("showrt")){
				checkDV(temp);
			}
			else if(sentence.toLowerCase().startsWith("changecost")){
				String check = new String(sentence);
				String [] array = check.split("\\s+");
				if(array.length != 4){
					System.out.println("wrong format");
					continue;
				}
				if(!temp.getneighborIPPort().contains(new IpPort(array[1],Integer.parseInt(array[2])))){
					System.out.println("did not find such neighbor");
					continue;
				}
				IpPort neightbor = temp.findNeighbor(array[1] , Integer.parseInt(array[2]));
				if(neightbor==null){
					System.out.println("bug  here   it is null");
				}
				if(temp.linkDownList.contains(neightbor)){
					System.out.println("linkdown ignored");
					continue;
				}
				double costChange =  Double.parseDouble(array[3]);
				if(costChange < 0){
					System.out.println("cost can not be negative");
					continue;
				}
				client.linkChange = neightbor;
				System.out.println("cost change is " + costChange);
				
				client.linkChangeCost = costChange;
				temp.linkChange(neightbor, costChange);
			}
			else if(sentence.toLowerCase().startsWith("linkdown")){

				String check = new String(sentence);
				String [] array = check.split("\\s+");
				if(array.length != 3){
					System.out.println("wrong format");
					continue;
				}
				if(!temp.getneighborIPPort().contains(new IpPort(array[1],Integer.parseInt(array[2])))){
					System.out.println("did not find such neighbor");
					continue;
				}
				IpPort neightbor = temp.findNeighbor(array[1] , Integer.parseInt(array[2]));
				if(neightbor == null){
					System.out.println("bug  here   it is null");
				}
				if(temp.linkDownList.contains(neightbor)){
					System.out.println("already in the linked down list");
					continue;
				}
				client.linkDown = neightbor;
				
				temp.linkdown(neightbor,Double.MAX_VALUE);

			}
			
			else if(sentence.toLowerCase().startsWith("linkup")){

				String check = new String(sentence);
				String [] array = check.split("\\s+");
				if(array.length != 3){
					System.out.println("wrong format");
					continue;
				}
				if(!temp.getneighborIPPort().contains(new IpPort(array[1],Integer.parseInt(array[2])))){
					System.out.println("did not find such neighbor");
					continue;
				}
				IpPort neightbor = temp.findNeighbor(array[1] , Integer.parseInt(array[2]));
				if(neightbor == null){
					System.out.println("bug  here   it is null");
				}
				if(!temp.linkDownList.contains(neightbor)){
					System.out.println("already linked up");
					continue;
				}
				client.linkUp = neightbor;
				temp.linkUp(neightbor);
				
			}
			else if(sentence.toLowerCase().startsWith("transfer")){
				String check = new String(sentence);
				String [] array = check.split("\\s+");
				if(array.length != 4){
					System.out.println("wrong format");
					continue;
				}
				
				IpPort neightbor = new IpPort(array[2] , Integer.parseInt(array[3]));
				if(!temp.destLink.containsKey(neightbor)){
					System.out.println("there is no such destination ");
					continue;
				}
				client.transfer = true;
				client.fileName = array[1];
				client.transferIP = neightbor;
				
			}
			else if(sentence.equalsIgnoreCase("close")){
				closeSignal = true;
			}
			else{
			 	System.out.println("do not understand your command");
			}
		}
		System.out.println("system ends");
		System.exit(0);
	}



	/*  used to look up the DV 
        */
	public static void checkDestLink(node temp){
		for(IpPort ttii : temp.destLink.keySet()){
			System.out.print(" the destination ip is " + ttii.ip);
			System.out.print(" the destination port is " + ttii.port);
			System.out.print(" the link ip is " + temp.destLink.get(ttii).ip);
			System.out.println(" the link port is " + temp.destLink.get(ttii).port);


		}

	}

	/*  used to look up the neightbor DV 
        */
	public static void checkNeighborDV(node temp){
		Iterator<Entry<IpPort, HashMap<IpPort, Double>>> it2 = temp.getNeighborDV().entrySet().iterator();
		while(it2.hasNext()){
			Entry<IpPort, HashMap<IpPort, Double>> ttooo = it2.next();
			System.out.println("neighbor ip is " +  ttooo.getKey().ip);
			System.out.println("neighbor port is " +  ttooo.getKey().port);
			Iterator<Entry<IpPort, Double>> jooooo = ttooo.getValue().entrySet().iterator();

			while(jooooo.hasNext()){
				Entry<IpPort, Double> ppp = jooooo.next();
				System.out.print ( "ip is " + ppp.getKey().ip);
				System.out.print ( "  port is " + ppp.getKey().port);
				if(ppp.getValue()!= Double.MAX_VALUE)
					System.out.print( "  cost is " + ppp.getValue() + "\n");
				else{
					System.out.print( "  cost is " + "infinity" + "\n");
				}

			}
		}
	}

	// used to check the DV
	public static void checkDV(node temp){
		Iterator<Entry<IpPort, Double>> it1 = temp.getDV().entrySet().iterator();
				System.out.println(System.currentTimeMillis() + "  Distance vector list is:");
				while(it1.hasNext()){
					Entry<IpPort, Double> ppp = it1.next();
					if(!ppp.getKey().equals(temp.itself)){
						System.out.print ( "destination is " + ppp.getKey().ip +" :" + ppp.getKey().port);
						
						if(ppp.getValue() != Double.MAX_VALUE){
							System.out.print( "  cost = " + ppp.getValue() + " ,Link = ( ");
						} else{
							System.out.print( "  cost = " + " infinity" + " ,Link = ( ");
							
						}
						
						
						if (temp.destLink.get(ppp.getKey()) == null){
							System.out.println("none"+ " :" + 
								"none" +")"
								);
						} else{
						
							System.out.println(temp.destLink.get(ppp.getKey()).ip+ " :" + 
								temp.destLink.get(ppp.getKey()).port +")"
									);
					       }
				 	 }
				}
		 	
	}


	public static void updateNeightbor(node temp, String neightbor, int port, HashMap<IpPort,Double> neighborDv){
		temp.updateNeighborDV(neightbor, port, neighborDv);

	}

	/*
	 read the configuration file and set everything up
	 parameter host: the node
	 parameter String fiiename: filename of file
	*/ 
	public static void nodeConfig(node host, String filename){
		BufferedReader br = null; 		 
		try {

			String sCurrentLine;
			int localPort = -1;
			int timeOUt = -1;

			br = new BufferedReader(new FileReader(filename));
			boolean firstLine = true;
			while ((sCurrentLine = br.readLine()) != null) {
				if(sCurrentLine.trim().isEmpty())
					continue;
				if(firstLine){
					String [] portTimeOut = sCurrentLine.split("\\s+");
					localPort = Integer.parseInt(portTimeOut[0]);
					timeOUt = Integer.parseInt(portTimeOut[1]);
					firstLine = false;
				}
				else{
					String[] weightIp = sCurrentLine.split("\\s+");
					
					double cost = Double.parseDouble(weightIp[1]);
					
					String[] IpPort = weightIp[0].split(":");
					String neighborIp = IpPort[0];
					int neighborPort = Integer.parseInt(IpPort[1]);
					host.setListenOnPort( localPort);
					host.setTimeOutValue( timeOUt);
					host.initialize(neighborIp,neighborPort,cost);

				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}
