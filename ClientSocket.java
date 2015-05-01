//package routingTable;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
public class ClientSocket implements Runnable{
	public Thread t;
	public IpPort linkUp = null;
	public static int packetNumber =  0 ;
	node host;
	DatagramSocket clientSocket;
	int timesOut;
	IpPort linkDown = null;
	IpPort linkChange = null;
	double linkChangeCost = -1;
	public ClientSocket(node host,DatagramSocket clientSocket){
		this.host = host;
		this.clientSocket = clientSocket;
		this.timesOut = host.getTimeOUt();
	}
	private int MSS = 4000;

	public boolean transfer = false;
	public String fileName = null;
	public IpPort transferIP = null;
	
	
	public void start ()
	{
		if (t == null)
		{
			t = new Thread (this);
			t.start ();
		}
	}
	// index where file start | filename | actual file
	private void fillUpwithFile(byte[]data, int start, int readNumber, InputStream fh){

		int readBytes;

		try {
			readBytes = fh.read(data, start, readNumber);
			int offset = start;
			while(readBytes<readNumber){
				offset += readBytes;
				readNumber -= readBytes;
				readBytes = fh.read(data, offset, readNumber);
				System.out.println("it happens!!!!!!!!!!");
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private int fillUpWithFileName(byte[]data){
		String fileName = this.fileName;
		int start = 20;
		byte[] array = fileName.getBytes();
		int fileNameLength = array.length;
		int fileStart = 24+ fileNameLength;
		byte[] tempBuffer = new byte[4];
		ByteBuffer.wrap(tempBuffer).putInt(fileStart);
		for(int i = 0 ;i < 4;i++){
			data[start++] = tempBuffer[i];
		}
		for ( int i = 0; i < array.length ; i ++){
			data[start++] = array[i];
		}
		return start;
	}


	private void modifyFileHeader(byte[] sendData, int i, int  loop){
		sendData[13] = (byte)(loop&0xff);
		
		sendData[14] =  (byte)(i&0xff);

		sendData[15] =  (byte)(packetNumber&0xff);
		int checksum = checkSumComputation(sendData);
		checksum = ~checksum;
		checksum = checksum&0xffff;
		//System.out.println("checksum is " + checksum);
		sendData[16] =(byte) (checksum>>8&0xff);
		sendData[17] =(byte) (checksum &0xff);
		return;
	}
	
	private void fillFileDataWithLen(byte[] sendData, int number){
		byte src1 = (byte) (number>>8);
		byte src2 = (byte)(number & 0xff);	
		sendData[18] = src1;
		sendData[19] = src2;
		return;
	}
	/*
	 * packet format is : message type| source IP address|source Port number| destination IP|
	 * destination port| fragment indicator | packet fragment number | packet number |checksum| length| actual file 
	 */
	@Override
	public void run() {
		long timesBegin =  (System.currentTimeMillis()/1000);
		while (true){
			 if(this.transfer && this.fileName!=null &&this.transferIP != null){

				File f = new File(fileName);
				long fileLength = f.length();
				int loop = (int)(fileLength/MSS);
				if(fileLength%MSS != 0){
					loop++;
				}

				InputStream fh = null;
				try {
					fh = new FileInputStream(f);
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				for ( int i = 0 ; i < loop ; i++){
					byte[] sendData = new byte[4096];
					fillUpWithHeader(sendData,  node.transfer_file,  this.transferIP);
					modifyFileHeader(sendData,i, loop);
					int start = fillUpWithFileName(sendData);
					System.out.println("start position is  " + start);
					int readNumber = MSS;
					if( i == loop -1){
						readNumber = (int) (fileLength - (loop-1)*MSS);
					}
					fillUpwithFile(sendData,  start, readNumber, fh);
					System.out.println("readNUmber is " + readNumber);
					
					fillFileDataWithLen(sendData,readNumber );
					System.out.println("total lengt is "  + ServerSocket.getTotalLength(sendData));
					
					sendingData(sendData, this.host.destLink.get(this.transferIP));
					System.out.println("Next Hop =  " +this.host.destLink.get(this.transferIP).ip +":"+this.host.destLink.get(this.transferIP).port);
					System.out.println("File sent successfully");
				}
				this.transfer = false;
				this.fileName = null;
				this.transferIP = null;
				try {
					fh.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				packetNumber++;
			}
			if(this.linkChange!= null && this.linkChangeCost != -1){
				byte[] sendData = new byte[4096];

				fillUpWithHeader(sendData,  node.link_change,  this.linkChange);
				fillUpWithLinkChage(sendData,this.linkChange,this.linkChangeCost);
				sendingData(sendData, this.linkChange);
				this.linkChange = null;
				this.linkChangeCost = -1;
			}
			else if(this.linkUp != null){
				byte[] sendData = new byte[4096];

				fillUpWithHeader(sendData,  node.link_up,  this.linkUp);

				sendingData(sendData, this.linkUp);
				this.linkUp = null;
			}
			else if(this.linkDown != null){
				byte[] sendData = new byte[4096];

				fillUpWithHeader(sendData,  node.link_down,  this.linkDown);

				sendingData(sendData, this.linkDown);

				this.linkDown = null;			
			}
			else if((System.currentTimeMillis()/1000 - timesBegin)>=timesOut || this.host.changeSignal ){
				timesBegin =  (System.currentTimeMillis()/1000);
				this.host.changeSignal = false;

				List<IpPort> neighborIpPort = this.host.getneighborIPPort();

				for (IpPort neighbot : neighborIpPort){
					if(!this.host.linkDownList.contains(neighbot)){
						byte[] sendData = new byte[4096];

						fillUpWithHeader(sendData, node.route_update,  neighbot);

						fillUpWithDV(sendData,neighbot);

						sendingData(sendData,  neighbot);
					}
				}
			}
		}
	}


	private void fillUpWithLinkChage(byte[]sendData, IpPort neighbot, double linkcost){
		int start = 20;
		calculateCost(sendData,  start,  linkcost);

	}
	private void fillUpWithDV(byte[] sendData, IpPort neighbot){
		HashMap<IpPort,Double> dv = this.host.getDV();

		int start = 20;
		for (IpPort ipPort:dv.keySet()){

			String ip = ipPort.ip;
			int port = ipPort.port;
			double cost = dv.get(ipPort);

			if(!ipPort.equals(this.host.itself) && (this.host.destLink.get(ipPort) == null|| this.host.destLink.get(ipPort).equals(neighbot))){
				cost = Double.MAX_VALUE;
			}
			start = calculateIp(sendData,start,ip);
			start = calculatePort(sendData,start,port);
			start = calculateCost(sendData,start,cost);
		}
		int totallengh = start-20;
		calculateLength(sendData,totallengh);
	}

	private void sendingData(byte[]sendData, IpPort neighbor){
		InetAddress IP;
		try {
			IP = InetAddress.getByName(neighbor.ip);


			DatagramPacket sendPacket =
					new DatagramPacket(sendData, sendData.length,IP , neighbor.port);

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

	private void fillUpWithHeader(byte[]sendData, int messageType, IpPort neighbot){
		sendData [0] = (byte)messageType;

		String srcIp = null;
		try {
			srcIp = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		checksum = checksum&0xffff;
		//System.out.println("checksum is " + checksum);
		sendData[16] =(byte) (checksum>>8&0xff);
		sendData[17] =(byte) (checksum &0xff);
		sendData[18] = (byte)0;
		sendData[19] = (byte)0;


	}

	public int calculateLength(byte[]sendData, int num){
		byte src1 = (byte)(num>>8);
		byte src2 = (byte)(num&0xff);
		sendData[18] = src1;
		sendData[19] = src2;
		return 1;
	}

	public int calculateCost(byte[]sendData, int start, double cost){
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putDouble(cost);
		for ( int i = 0 ; i < 8 ; i++){
			sendData[start++] = bytes[i];
		}
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
