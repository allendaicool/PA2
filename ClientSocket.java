//package routingTable;
import java.io.*;
import java.net.*;
import java.util.*;
public class ClientSocket implements Runnable{
	public Thread t;

	node host;
	DatagramSocket clientSocket;
	int timesOut;

	public ClientSocket(node host,DatagramSocket clientSocket){
		this.host = host;
		this.clientSocket = clientSocket;
		this.timesOut = host.getTimeOUt();
	}

	public void start ()
	{
		if (t == null)
		{
			t = new Thread (this);
			t.start ();
		}
	}

	
	/*
	 * packet format is : message type| source IP address|source Port number| destination IP|
	 * destination port| fragment indicator | packet fragment number | packet number | actual file 
	 */
	@Override
	public void run() {
		long timesBegin =  (System.currentTimeMillis()/1000);
		while (true){
			if((System.currentTimeMillis()/1000 - timesBegin)>=timesOut || this.host.changeSignal){
				timesBegin =  (System.currentTimeMillis()/1000);
				this.host.changeSignal = false;
				
				List<IpPort> neighborIpPort = this.host.getneighborIPPort();
				
				String sendInformation = null;
				
				for (IpPort neighbot : neighborIpPort){

					byte[] sendData = new byte[1024];
					
					
					try {
						sendData[0] = (byte)1;
						String srcIp = InetAddress.getLocalHost().getHostAddress();
						String[] srcip = srcIp.split("[.]+");
						int j = 1;
						for ( int i = 0 ; i < 4;i++){
							int temp = Integer.valueOf(srcip[i]);
							sendData[j++] = (byte)temp;
						}
						
						int srcPort = host.getPort();
						byte src1 = (byte)(srcPort>>8);
						sendData[j++] = src1;
						byte src2 = (byte)(srcPort&0xff);
						sendData[j++] = src2;
						
						String destIp = neighbot.ip;
						String[] destip = destIp.split("[.]+");
						for ( int i = 0 ; i < 4;i++){
							int temp = Integer.valueOf(destip[i]);
							sendData[j++] = (byte)temp;
						}
						int destport = neighbot.port;
						sendData[j++] = (byte)(destport>>8);
						sendData[j++] = (byte)(destport&0xff);
						sendData[j++] = (byte)0;
						sendData[j++] = (byte)0;
						sendData[j++] = (byte)0;
						int checksum = checkSumComputation(sendData);
						checksum = ~checksum;
						checksum =checksum&0xffff;
						//System.out.println("checksum is " + checksum);
						sendData[16] =(byte) (checksum>>8&0xff);
						sendData[17] =(byte) (checksum &0xff);
						sendData[18] = (byte)0;
						sendData[19] = (byte)0;
						
						/*sendInformation = "1\n"+InetAddress.getLocalHost().getHostAddress()
								+"\n"+host.getPort()+"\n"+neighbot.ip+"\n" + neighbot.port
								+"\n0" + "\n0"+"\n0";*/
						HashMap<IpPort,Integer> dv = this.host.getDV();
						
						int start = 20;
						for (IpPort ipPort:dv.keySet()){
							
							String ip = ipPort.ip;
							int port = ipPort.port;
							int cost = dv.get(ipPort);
							if(neighbot.port==4115 && port==4118){
								System.out.println("4118 port to port 4115 cost is " + cost);
							}
							if(!ipPort.equals(this.host.itself) && this.host.destLink.get(ipPort).equals(neighbot)){
								cost = Integer.MAX_VALUE;
							}
							
							start = calculateIp(sendData,start,ip);
							start = calculatePort(sendData,start,port);
							start = calculateCost(sendData,start,cost);
						}
						int totallengh = start-20;
						calculateLength(sendData,totallengh);
						
					} catch (UnknownHostException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					

					InetAddress IP;
					try {
						IP = InetAddress.getByName(neighbot.ip);
						
						System.out.println("send information is " +sendInformation );
						System.out.println("send to IP is " + neighbot.ip );
						System.out.println("send to port is " + neighbot.port );
						System.out.println();
						
						DatagramPacket sendPacket =
								new DatagramPacket(sendData, sendData.length,IP , neighbot.port);

						clientSocket.send(sendPacket);

					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

		}

	}
	
	public int calculateLength(byte[]sendData, int num){
		byte src1 = (byte)(num>>8);
		byte src2 = (byte)(num&0xff);
		sendData[18] = src1;
		sendData[19] = src2;
		return 1;
	}
	
	public int calculateCost(byte[]sendData, int start, int cost){
		byte src1 = (byte)(cost>>24);
		byte src2 = (byte)((cost>>16)&0xff);
		byte src3 = (byte)(cost>>8&0xff);
		byte src4 = (byte)(cost&0xff);
		sendData[start++] = src1;
		sendData[start++] = src2;
		sendData[start++] = src3;
		sendData[start++] = src4;
		return start;
	}
	public int calculatePort(byte[] sendData, int start, int port){		
		byte src1 = (byte)(port>>8);
		sendData[start++] = src1;
		byte src2 = (byte)(port&0xff);
		sendData[start++] = src2;
		return start;
	}
	
	public int calculateIp(byte[] sendData, int start, String ip){
		String tempIp = new String(ip);
		String[] tempIpSeg =  tempIp.split("[.]+");
		
		for ( int i = 0 ; i < 4; i++){
			int temp = Integer.valueOf(tempIpSeg[i]);
			sendData[start++] = (byte)temp;
		}
		return start;
	}
	
	public  int checkSumComputation(byte[] cal){
		int  sum = 0 ;
		for ( int  i = 0 ; i <16;i=i+2){
			int temp1 = (cal[i]<<8)&0xff;
			int temp2 = (cal[i+1])&0xff;
			int tempSum = temp1+temp2;
			sum += tempSum;
			if((sum&0x1ff)>>16 == 1){
				sum = sum&0xffff + 1;
			}
		}
		return sum;
	}

}
