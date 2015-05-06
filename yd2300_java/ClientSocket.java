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
	// linkdown signal
	IpPort linkDown = null;
	IpPort linkChange = null;
	double linkChangeCost = -1;
	public ClientSocket(node host,DatagramSocket clientSocket){
		this.host = host;
		this.clientSocket = clientSocket;
		this.timesOut = host.getTimeOUt();
	}
	private int MSS = 4000;
	private int filePktSize = 4096;
	
	// file transfer signal
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

	/*
 	     this method fill up the header with file name 
	     parameter: byte[] data : received data from the sender
	*/
	private int fillUpWithFileName(byte[]data){
		String fileName = this.fileName;
		int start = 22;
		byte[] array = fileName.getBytes();
		int fileNameLength = array.length;
		int fileStart = 26+ fileNameLength;
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

	/*
 	     this method modify  the header with total packet number 
	     parameter: byte[] senddata : received data from the sender
	     parameter: int i: the pkt number
	     parameter: int loop senddata : total pkt number
		
	*/
	private void modifyFileHeader(byte[] sendData, int i, int  loop){
		sendData[14] = (byte)(loop&0xff);
		sendData[13] = (byte)(loop>>8&0xff);


		sendData[16] =  (byte)(i&0xff);
		sendData[15] = (byte)(i>>8&0xff);

		sendData[17] =  (byte)(packetNumber&0xff);
		int checksum = checkSumComputation(sendData);
		checksum = ~checksum;
		checksum = checksum&0xffff;
		//System.out.println("checksum is " + checksum);
		sendData[18] =(byte) (checksum>>8&0xff);
		sendData[19] =(byte) (checksum &0xff);
		return;
	}

	/*
 	     this method fill  the header with total packet length 
	     parameter: byte[] senddata : received data from the sender
	     parameter: int number : packet length
		
	*/
	private void fillFileDataWithLen(byte[] sendData, int number){
		byte src1 = (byte) (number>>8);
		byte src2 = (byte)(number & 0xff);	
		sendData[20] = src1;
		sendData[21] = src2;
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
				if(!f.isFile()){
					System.out.println("file does not exists");
					this.transfer = false;
					this.fileName = null;
					this.transferIP = null;
					continue;
				}

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
				/*
				    sending each packet 
					*/
				for ( int i = 0 ; i < loop ; i++){
					byte[] sendData = new byte[filePktSize];
					fillUpWithHeader(sendData,  node.transfer_file,  this.transferIP);
					modifyFileHeader(sendData,i, loop);
					int start = fillUpWithFileName(sendData);
					int readNumber = MSS;
					if( i == loop -1){
						readNumber = (int) (fileLength - (loop-1)*MSS);
					}
					fillUpwithFile(sendData,  start, readNumber, fh);

					fillFileDataWithLen(sendData,readNumber );
					//int FragmentFollow = sendData[14]&0xff;
					//System.out.println("fragment number is " + FragmentFollow);
					sendingData(sendData, this.host.destLink.get(this.transferIP));
					System.out.println("Next Hop =  " +this.host.destLink.get(this.transferIP).ip +":"+this.host.destLink.get(this.transferIP).port);
					System.out.println("File sent successfully");
				}
				// transfer is the signal passed from the bfclient
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
			// sending update message when timeout expires
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

	/* This method is used to fill up the data with link change information
	 *  parameter sendData : data received from sender
	 * parameter neighbot: the neighbor to notify
	 * double linkcost: updated link cost
	 */
	private void fillUpWithLinkChage(byte[]sendData, IpPort neighbot, double linkcost){
		int start = 22;
		calculateCost(sendData,  start,  linkcost);

	}
	
	/* This method is used to fill up the packet with DV
	 *  parameter sendData : data received from sender
	 * parameter neighbot: the neighbor to notify
	 */
	private void fillUpWithDV(byte[] sendData, IpPort neighbot){
		HashMap<IpPort,Double> dv = this.host.getDV();

		int start = 22;
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
		int totallengh = start-22;
		calculateLength(sendData,totallengh);
	}
	/* This method is used to send the actual data
	 *  parameter sendData : data received from sender
	 * parameter neighbot: the neighbor to notify
	 */
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

	/* This method fill up the header with message type 
	 *  parameter sendData : data received from sender
	 * parameter messageType: type of packet message
	 * parameter neighbot: the neighbor to notify
	 */
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
		sendData[j++] = (byte)0;

		sendData[j++] = (byte)0;
		int checksum = checkSumComputation(sendData);
		checksum = ~checksum;
		checksum = checksum&0xffff;
		//System.out.println("checksum is " + checksum);
		sendData[18] =(byte) (checksum>>8&0xff);
		sendData[19] =(byte) (checksum &0xff);
		sendData[20] = (byte)0;
		sendData[21] = (byte)0;


	}

	/* This method fill up the header with packet length 
	 *  parameter sendData : data received from sender
	 * parameter num: the calculated length
	 */
	public int calculateLength(byte[]sendData, int num){
		byte src1 = (byte)(num>>8);
		byte src2 = (byte)(num&0xff);
		sendData[20] = src1;
		sendData[21] = src2;
		return 1;
	}
	
	/* This method fill up the header with cost
	 * parameter sendData : data received from sender
	 * parameter start: the position to fill up the cost
	 * parameter cost:  the cost to fill up
	 */
	public int calculateCost(byte[]sendData, int start, double cost){
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putDouble(cost);
		for ( int i = 0 ; i < 8 ; i++){
			sendData[start++] = bytes[i];
		}
		return start;
	}
	
	/* This method fill up the header with pport number
	 * parameter sendData : data received from sender
	 * parameter start: the position to fill up the cost
	 * parameter port:  the port to fill up
	 */
	public int calculatePort(byte[] sendData, int start, int port){		
		byte src1 = (byte)(port>>8);
		sendData[start++] = src1;
		byte src2 = (byte)(port&0xff);
		sendData[start++] = src2;
		return start;
	}

	/* This method fill up the header with pport number
	 * parameter sendData : data received from sender
	 * parameter start: the position to fill up the cost
	 * parameter ip:  the ip to fill up
	 */
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
