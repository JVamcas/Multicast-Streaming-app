# Adaptive trasncoding of Multi-definition Multicasted video streams

This project concerns the development of a streaming server that streams a single multicast
video stream to multiple clients who requires video streams encoded at bitrates commensurate 
to their network bandwidths. Both VoD and Live streaming are supported.

A streaming server and a client application that works with the 
streaming server were developed.

#### Dependencies: _ffmpeg, alsa, v4l2_

## INSTRUCTIONS:

The server and client applications are contained in the `server` and `client` pakages respectively.

## 1. RUNNING THE APPLICATIONS

### 1.1. Run the `server.core.ServerMain` class to start the server application.
	a) You need to enter the IP address of the PC's interface to be used by the server.
	b) You must enter your webcam name, normally the format is "/dev/video0".

### 1.2 Run the `client.core.ClientMain` class to start the client application.
	a) Click on any VoD video of your choice to start streaming.
	b) For live streaming, click on the "live stream" button to start streaming.

### 1.3 If you wish to run the client applications on separate PCs, then:
	a) Either copy the whole project to each of these PCs and run the client application using step 1.2(Recommended).
	b) Or, copy over the directories, `"src/main/java/client"` and `"res/client"`, to the client PCs.

### 2. Adding a VoD to the server:
	2.1 Add the video file to the video folder such that the path format looks like: *res/server/videos/<video name>/<video name>/.<file extension>
	2.2 The video name should not have space or underscore characters.
	2.3 Add the video path to the file _res/server/videoUrl.txt_.

