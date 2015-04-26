JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
       	ClientSocket.java\
	fileInput.java\
        IpPort.java\
        node.java\
	ServerSocket.java
	

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
