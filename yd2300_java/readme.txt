  Programming design and structure:
  	The main function is in bfclient.java used to read the configuration from config.txt file. Also, it waits for and execute the user command.
	
	node.java is the class that defines a node in the network
	
	CheckTimeout.java is used to check after the timeout expires whether it has received the packet from neighbor or not.
	
	ServerSocket.java is used to listen for the packet on the specified port. It receives messages and transfers the packets which is not destined to the node.
	
	IpPort.java defines the combination of Ip and Port. It implements the equal and hashcode method
	
	ClientSocket.java waits for the signal to send the packet.
	
	

the packet format: byte[] array
      message type 0 |  sourceIP 1-4 | sourcePort| 5-6 | destIP 7-10 | destPort 11-12
| total packet number 13-14 |  pkt number 15-16 | file number 17 | checksum 18-19 | packet length 20-21

for File transfer:  22-26 the index where file starts | 27 - the filename
    linkup/ link down message: no message after the packet length
    change cost: cost after the packet length
    updateDV: ip|port|cost
	
How to compile the code : just enter make in the terminal

how to run the code: java bfclient config2.txt 
 			// bfclient is the main java function. config2.txt is the configuration file for client2
			
			
The maximum packet size is MSS = 4000 and the packet size is set to 4096.


Did not implement the bonus part. Therefore, packet loss might happen
     