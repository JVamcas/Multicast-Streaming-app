# Adaptive trasncoding of Multi-definition Multicasted video streams

This project concerns the development of a streaming server that streams a single multicast
video stream to multiple clients who requires video streams encoded at bitrates commensurate 
to their network bandwidths. Both VoD and Live streaming are supported.

A streaming server and a client application that works with the 
streaming server were developed.

#### Dependencies: _ffmpeg, alsa, v4l2_

## INSTRUCTIONS:

The server and client applications are contained in the `server` and `client` pakages respectively.

### 1. RUNNING THE APPLICATIONS

#### 1.1. How to run the server application

    a) Run the `server.core.ServerMain` class to start the server.
	b) You need to enter the IP address of the PC's interface to be used by the server.
	c) You must enter your webcam name, normally the format is "/dev/video0".

#### 1.2 Hot to run the client application
    a) Run the `client.core.ClientMain` class to start the client application.
	b) Click on any VoD video of your choice to start streaming.
	c) For live streaming, click on the "live stream" button to start streaming.

#### 1.3 Running the client application on separate PCs other than the server's:
	a) Either copy the whole project to each of these PCs and run the client application using step 1.2(Recommended).
	b) Or, Create a maven project and copy the dependencies from the pom.xml file then,
	c) Copy over the directories, `"src/main/java/client"` and `"res/client"` to the client PCs.

### 2. ADDING A VoD VIDEO TO THE SERVER
	2.1 Add the video file to the video folder such that the path format looks like: *res/server/videos/<video name>/<video name>/.<file extension>
	2.2 The video name should not have space or underscore characters.
	2.3 Add the video path to the file _res/server/videoUrl.txt_.
	
`Note:` You can find results for videos streamed at bandwidth 128k,300k,800k,2000k and 10000k in the 
folder named "screen_grabs".

