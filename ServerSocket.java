//package routingTable;
import java.io.*;
import java.net.*;
import java.util.*;
public class ServerSocket implements Runnable{


	public Thread t;
	public DatagramSocket serverSocket;
	public node host;
	public byte[] receiveData;

	public ServerSocket(DatagramSocket serverSocket, node host){
		this.host = host;
		this.serverSocket = serverSocket;
		this.receiveData = new byte[1024];
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
		while(true){
			DatagramPacket receivePacket =
					new DatagramPacket(receiveData, receiveData.length);
			try {
				serverSocket.receive(receivePacket);
				byte [] byteArra = receivePacket.getData();
				//String sentence = new String(receivePacket.getData());

				String IPAddress = receivePacket.getAddress().getHostAddress();
				int port  = receivePacket.getPort();

				int messageType = byteArra[0]&0xff;
				String sourceIP = getIpAddress(byteArra,1);

				int sourcePort = getPort(byteArra,5);

				String destinationIP = getIpAddress(byteArra,7);
				int destinationPort = getPort(byteArra,11);
				int isFragment = byteArra[13]&0xff;
				int fragmentNum = byteArra[14]&0xff;
				int pktNum = byteArra[15]&0xff;
				int checksum = getCheckSum(byteArra);

				int totalLength = getTotalLength(byteArra);
				if(messageType == node.route_update){
					handleUpdateMessage(sourceIP,sourcePort,byteArra,totalLength/10);
				}


			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	public int getTotalLength(byte[]array){
		int length = (array[18]&0xff)<<8;
		length+= array[19]&0xff;
		return length;
	}
	public int getCheckSum(byte[] array){
		int sum = (array[16]&0xff)<<8;
		sum+= array[17]&0xff;
		return sum;
	}
	public int getPort(byte[] array, int start){
		int srcPortRecover = (array[start]&0xff)<<8;
		srcPortRecover += (array[start+1]&0xff);
		return srcPortRecover;
	}
	public int getCost(byte[] array, int start){
		int sum = 0;


		sum+=(array[start]&0xff)<<24;
		sum+=(array[start+1]&0xff)<<16;
		sum+=(array[start+2]&0xff)<<8;
		sum+=(array[start+3]&0xff);
		return sum;
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

	public void displayNeighbor(HashMap<IpPort,Integer> temp){
		for(IpPort like : temp.keySet()){
			System.out.print("neiDV ip is " + like.ip+" ");
			System.out.print("neiDV port is " + like.port+" ");
			System.out.println("neiDV port is " + temp.get(like)+" ");
		}
	}

	public void handleUpdateMessage(String NeighborIPAddress , int port,byte[] array,int length){
		HashMap<IpPort,Integer> neighborDV = new HashMap<IpPort,Integer>();
		int start = 20;
		for ( int i = 0 ; i < length; i++){
			String ip  = getIpAddress(array,start);
			start += 4;
			int tempport = getPort(array,start);
			start+=2;
			int tempCost = getCost(array,start);
			start+=4;

			IpPort temp = new IpPort(ip,tempport);
			neighborDV.put(temp, tempCost);
		}
		System.out.println("receive side ip address is " + NeighborIPAddress);
		System.out.println("receive port is " + port);
		displayNeighbor(neighborDV);

		this.host.updateNeighborDV(NeighborIPAddress, port, neighborDV);
	}



}
