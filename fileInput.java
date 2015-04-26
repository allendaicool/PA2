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

public class fileInput {
	public static int packetID = 0 ;

	public static void main(String[] args){
		
		
		
		String fileName = args[0];
		System.out.println("filename is " + fileName);
		node temp = new node();
		nodeConfig(temp, fileName);
		System.out.println("localport is " + temp.getPort());
		System.out.println("timeout is " + temp.getTimeOUt());
		
		checkDV(temp);

		
		
		/*
		String neightbor2 = "128.59.196.2";
		int port2 = 4116;
		HashMap<IpPort,Integer> neighborDV2 = new HashMap<IpPort,Integer>();
		IpPort temp5 = new IpPort("128.59.196.2" , 4118);
		neighborDV2.put(temp5, 50);
		IpPort temp6 = new IpPort("128.59.196.2" , 4116);
		neighborDV2.put(temp6, 0);
		IpPort temp7 = new IpPort("10.15.1.186" , 4115);
		neighborDV2.put(temp7,Integer.MAX_VALUE);
		updateNeightbor(temp,neightbor2,port2,neighborDV2);
		checkDV(temp);
		
		
		String neightbor3 = "128.59.196.2";
		int port3  = 4118;
		HashMap<IpPort,Integer> neighborDV3 = new HashMap<IpPort,Integer>();
		IpPort temp8 = new IpPort("128.59.196.2" , 4118);
		neighborDV3.put(temp8, 0);
		IpPort temp9 = new IpPort("128.59.196.2" , 4116);
		neighborDV3.put(temp9, 50);
		IpPort temp10 = new IpPort("10.15.1.186" , 4115);
		neighborDV3.put(temp10,Integer.MAX_VALUE);
		
		updateNeightbor(temp,neightbor3,port3,neighborDV3);
		
		String neightbor = "128.59.196.2";
		int port  = 4116;
		HashMap<IpPort,Integer> neighborDV = new HashMap<IpPort,Integer>();
		IpPort temp2 = new IpPort("128.59.196.2" , 4116);
		neighborDV.put(temp2, 0);
		IpPort temp3 = new IpPort("128.59.196.2" , 4118);
		neighborDV.put(temp3, Integer.MAX_VALUE);
		IpPort temp4 = new IpPort("10.15.1.186" , 4115);
		neighborDV.put(temp4,Integer.MAX_VALUE);
		
		updateNeightbor(temp,neightbor,port,neighborDV);
		
		String neightbor4 = "128.59.196.2";
		int port4  = 4118;
		HashMap<IpPort,Integer> neighborDV4 = new HashMap<IpPort,Integer>();
		IpPort temp11 = new IpPort("128.59.196.2" , 4116);
		neighborDV4.put(temp11, Integer.MAX_VALUE);
		IpPort temp12 = new IpPort("128.59.196.2" , 4118);
		neighborDV4.put(temp12, 0);
		IpPort temp13 = new IpPort("10.15.1.186" , 4115);
		neighborDV4.put(temp13,Integer.MAX_VALUE);
		
		updateNeightbor(temp,neightbor4,port4,neighborDV4);
		checkNeighborDV(temp);

		
		
		checkDV(temp);
		
		checkNeighborDV(temp);*/		
		
		
        
       
        try {
			DatagramSocket serverSocket = new DatagramSocket(temp.getPort());
			ServerSocket server = new ServerSocket(serverSocket,temp);
			DatagramSocket clientSocket = new DatagramSocket();
			ClientSocket client = new ClientSocket(temp,clientSocket);
			
			server.start();
			client.start();
			
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
        	if(sentence.equals("showrt")){
        		checkDV(temp);
        		checkNeighborDV(temp);
        	}
        	if(sentence.equalsIgnoreCase("close")){
        		closeSignal = true;
        	}
        }
        System.out.println("system ends");

	}
	
	
	public static void checkDestLink(node temp){
		 for(IpPort ttii : temp.destLink.keySet()){
	        	System.out.print(" the destination ip is " + ttii.ip);
	        	System.out.print(" the destination port is " + ttii.port);
	        	System.out.print(" the link ip is " + temp.destLink.get(ttii).ip);
	        	System.out.println(" the link port is " + temp.destLink.get(ttii).port);


	        }
	        
	}
	
	public static void checkNeighborDV(node temp){
		Iterator<Entry<IpPort, HashMap<IpPort, Integer>>> it2 = temp.getNeighborDV().entrySet().iterator();
        while(it2.hasNext()){
        	Entry<IpPort, HashMap<IpPort, Integer>> ttooo = it2.next();
            System.out.println("neighbor ip is " +  ttooo.getKey().ip);
            System.out.println("neighbor port is " +  ttooo.getKey().port);
            Iterator<Entry<IpPort, Integer>> jooooo = ttooo.getValue().entrySet().iterator();
            
            while(jooooo.hasNext()){
         	   Entry<IpPort, Integer> ppp = jooooo.next();
         	  System.out.print ( "ip is " + ppp.getKey().ip);
         	  System.out.print ( "  port is " + ppp.getKey().port);
         	  System.out.print( "  cost is " + ppp.getValue() + "\n");
               
            }
          }
	}
	
	public static void checkDV(node temp){
		Iterator<Entry<IpPort, Integer>> it1 = temp.getDV().entrySet().iterator();
	       while(it1.hasNext()){
	    	   Entry<IpPort, Integer> ppp = it1.next();
	    	  System.out.print ( "ip is " + ppp.getKey().ip);
	    	  System.out.print ( "  port is " + ppp.getKey().port);
	    	  System.out.print( "  cost is " + ppp.getValue() + "\n");
	          
	       }
	}
	
	
	public static void updateNeightbor(node temp, String neightbor, int port, HashMap<IpPort,Integer> neighborDv){
		temp.updateNeighborDV(neightbor, port, neighborDv);
		
	}
	
	public static void nodeConfig(node host, String filename){
		BufferedReader br = null; 		 
		try {

			String sCurrentLine;
			int localPort = -1;
			int timeOUt = -1;

			br = new BufferedReader(new FileReader(filename));
			boolean firstLine = true;
			while ((sCurrentLine = br.readLine()) != null) {
				if(firstLine){
					String [] portTimeOut = sCurrentLine.split("\\s+");
					localPort = Integer.parseInt(portTimeOut[0]);
					timeOUt = Integer.parseInt(portTimeOut[1]);
					firstLine = false;
				}
				else{
					String[] weightIp = sCurrentLine.split("\\s+");
					int cost = Integer.parseInt(weightIp[1]);
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
