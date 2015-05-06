//package routingTable;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
public class ServerSocket implements Runnable{

	byte[] test= null;
	public Thread t;
	public DatagramSocket serverSocket;
	public node host;
	// the receving data byte array
	public byte[] receiveData;
	public int segmentNum = 0 ;
	// used to redirect the pkt to the destination
	DatagramSocket clientSocketSer ;

	// keep which host is sedning packet to itseld
	public String sessionIP= null;
	public int sessionPort = -1;
	public int pkt = -1;

        /* 
	    initialize the datagramsocket and host
	*/
	public ServerSocket(DatagramSocket serverSocket, node host){
		this.host = host;
		this.serverSocket = serverSocket;
		this.receiveData = new byte[4096];
		try {
			clientSocketSer = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
	}

	public IpPort receive = null;
	public int packetNum = 0 ;

	// manage the pkt order
	public TreeMap<Integer,byte[]> packetOrder = new TreeMap<Integer, byte[]>();

	OutputStream fi = null;
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
		while(true){
			DatagramPacket receivePacket =
					new DatagramPacket(receiveData, receiveData.length);
			try {
				serverSocket.receive(receivePacket);
				byte [] byteArra = receivePacket.getData();
				//String sentence = new String(receivePacket.getData());

				//String IPAddress = receivePacket.getAddress().getHostAddress();

				int messageType = byteArra[0]&0xff;
				String sourceIP = getIpAddress(byteArra,1);

				int sourcePort = getPort(byteArra,5);




				int checksum = getCheckSum(byteArra);

				int totalLength = getTotalLength(byteArra);
				if(messageType==node.link_down){
					handleLinkDown(sourceIP,sourcePort );
				}
				if(messageType == node.route_update){
					handleUpdateMessage(sourceIP,sourcePort,byteArra,totalLength/14);
				}
				if(messageType == node.link_up){
					handleLinkUp(sourceIP, sourcePort);
				}
				if(messageType == node.link_change){
					double costChange = getLinkChangeCost(byteArra);
					handleLinkChange(sourceIP, sourcePort,costChange);
				}
				if(messageType == node.transfer_file){

					handleTransfer(byteArra);
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}


	/*
	   This handle the packet transfer in the array
 	*/
	private void handleTransfer(byte[] array){
		
		// get the useful information in the packet header
		String fileName = getFileName(array);
		String sourceIp = getIpAddress(array,1);
		int sourcePort = this.getPort(array,5 );
		String destinationIP = getIpAddress(array,7);
		int destinationPort = getPort(array,11);
        
		int FragmentFollow = (array[13]&0xff)<<8;
		
		FragmentFollow += (array[14]&0xff);
		
		int fragmentNum = (array[15]&0xff)<<8;
		fragmentNum += (array[16]&0xff);
		
		
		int pktNum = array[17]&0xff;



		// if the packet is destined to this node
		if(destinationIP.equals(this.host.itself.ip) && destinationPort==this.host.itself.port){
			segmentNum++;

			try {

				if( fi == null){
					sessionIP = sourceIp;
					this.sessionPort = sourcePort;
					this.pkt = pktNum;
					fi =  new FileOutputStream(fileName);
				}else{
					if(sessionIP!= null && this.sessionPort != -1 && pktNum!=this.pkt){
						fi.close();
						fi =  new FileOutputStream(fileName);
						segmentNum = 1;
						sessionIP = sourceIp;
						this.sessionPort  = sourcePort;
						this.pkt = pktNum;
						this.packetOrder.clear();
					}
				}


			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			byte[] deepCopy = Arrays.copyOf(array, array.length);
			this.packetOrder.put(fragmentNum, deepCopy);
			if(FragmentFollow == segmentNum){
				
				Set<Map.Entry<Integer,byte[]>> entrySet = this.packetOrder.entrySet();
				Iterator<Map.Entry<Integer,byte[]>> iter = entrySet.iterator();
				while(iter.hasNext()){
					Map.Entry<Integer,byte[]> temp = iter.next();
					byte[] tempByte = temp.getValue();
					int length = getTotalLength(tempByte);
					int startPosition = getFileNameEnd(tempByte)+1;

					try {
						fi.write(temp.getValue(),startPosition , length);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				System.out.println("packet received ");
				System.out.println("source  = " + sourceIp + ":"+sourcePort);
				System.out.println("Destination  = " + destinationIP+ ":" + destinationPort);
				System.out.println("file received successfully");
				try {
					fi.close();
					fi = null;
					this.packetOrder.clear();
					this.segmentNum = 0 ;
					sessionIP = null;
					this.sessionPort  = -1;
					this.pkt = -1;

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else{  // 		if the packet should be redirected

			IpPort redirect = new IpPort(destinationIP,destinationPort);
			System.out.println("packet received ");
			System.out.println("source  = " + sourceIp + ":"+sourcePort);
			System.out.println("Destination  = " + destinationIP+ ":" + destinationPort);
			System.out.println("Next hop = " + this.host.destLink.get(redirect).ip+":" +this.host.destLink.get(redirect).port);
			InetAddress IP = null;
			try {
				IP = InetAddress.getByName(destinationIP);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			DatagramPacket sendPacket =
					new DatagramPacket(array, array.length,IP , destinationPort);


			try {
				clientSocketSer.send(sendPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/*   Get the filiename from the byte array
		*/
	private String getFileName(byte[]array){
		int FileNameEnd  = getFileNameEnd(array);

		byte[] nameArray = new byte[FileNameEnd-26+1];
		int start = 26;
		for ( int  i = 0 ; i < nameArray.length ;i ++){
			nameArray[i] = array[start++];
		}
		String fileName = null;
		try {
			fileName = new String(nameArray, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return fileName;
	}

	/*   Get the position where actual data starts from
	*/
	private int getFileNameEnd (byte[]array ){

		int start = 22;
		byte[] temp = new byte[4];
		for ( int i = 0; i < 4; i++){
			temp[i] = array[start++];
		}
		int fileStartPos =  ByteBuffer.wrap(temp).getInt();
		return fileStartPos-1;
	}
	private double getLinkChangeCost(byte[] array){
		int start = 22;
		return getCost(array,  start);
	}

	public void handleLinkChange(String sourceIP , int sourcePort,double cost){
		IpPort temp = this.host.findNeighbor(sourceIP, sourcePort);
		this.host.linkChange(temp, cost);
	}

	public void handleLinkUp(String sourceIP , int sourcePort){
		IpPort temp = this.host.findNeighbor(sourceIP, sourcePort);
		this.host.linkUp(temp);
	}
	public void handleLinkDown(String sourceIP , int sourcePort ){
		IpPort temp = this.host.findNeighbor(sourceIP, sourcePort);
		this.host.linkdown(temp, Double.MAX_VALUE);
	}

	public static int getTotalLength(byte[]array){
		int length = (array[20]&0xff)<<8;
		length+= array[21]&0xff;
		return length;

	}
	public int getCheckSum(byte[] array){
		int sum = (array[18]&0xff)<<8;
		sum+= array[19]&0xff;
		return sum;
	}
	public int getPort(byte[] array, int start){
		int srcPortRecover = (array[start]&0xff)<<8;
		srcPortRecover += (array[start+1]&0xff);
		return srcPortRecover;
	}

	public double getCost(byte[] array, int start){
		byte[]bytes = new byte[8];
		for ( int i = 0; i < 8 ; i++){
			bytes[i] = array[start++];
		}
		return ByteBuffer.wrap(bytes).getDouble();
	}

	public String getIpAddress(byte[] array , int start){
		String iprrecover = "";
		int j = 0;
		for ( int  i = start ; i <4+start ; i ++){
			int p = array[i]&0xff;
			iprrecover += p;
			if(j <3){
				iprrecover+=".";
			}
			j++;
		}
		return iprrecover;
	}

	public void displayNeighbor(HashMap<IpPort,Double> temp){
		for(IpPort like : temp.keySet()){
			System.out.print("neiDV ip is " + like.ip+" ");
			System.out.print("neiDV port is " + like.port+" ");
			System.out.println("neiDV port is " + temp.get(like)+" ");
		}
	}

	public void handleUpdateMessage(String NeighborIPAddress , int port,byte[] array,int length){
		HashMap<IpPort,Double> neighborDV = new HashMap<IpPort,Double>();
		int start = 22;
		for ( int i = 0 ; i < length; i++){
			String ip  = getIpAddress(array,start);
			start += 4;
			int tempport = getPort(array,start);
			if(tempport == 0){
				System.out.println("port is 000000  !!!!!! bug  ");
			}
			start+=2;
			double tempCost = getCost(array,start);
			start+=8;

			IpPort temp = new IpPort(ip,tempport);
			neighborDV.put(temp, tempCost);

		}

		this.host.updateNeighborDV(NeighborIPAddress, port, neighborDV);
	}



}
