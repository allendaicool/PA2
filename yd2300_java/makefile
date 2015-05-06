JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
       	ClientSocket.java\
	bfclient.java\
        IpPort.java\
        node.java\
	ServerSocket.java\
	checkTimeOut.java
	

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
