package server.core;
/***
 * This interface contains constants used by the server application
 * Project: Adaptive Transcoding of multi-definition multicasted video stream
 * Dependecies: none
 * @author Petrus Kambala
 * Final year Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */
public interface Constants {

    enum streamType {VOD, LIVE}
    enum threadState {READY,SEND,DONE, TERMINATE}
    String ROOTFOlDER = "res/server";
    int SERVER_SOCKET_NUMBER = 4000;

    String CONFIG_FILE = ROOTFOlDER+"/udp.xml";

    int VoD_SEG_DURATION = 2;


    int FPS = 30;

    int VoD_GOP = VoD_SEG_DURATION*FPS;

    int PATCH_OR_MULTICAST = 1,MULTICAST = 2,PATCH = 3;
    int SEND_MULTICAST_PLAYLIST= 4, SEND_DATA_LEN = 6, AWAIT_CLIENT_REQUEST  = 0;
    int QUIT = 5;

    String SEG_START = "seg_start";
    String STREAM_END = "stream_end";
    int START = 0, SEND_DATA = 1, END = 2;

    String SNAP = "snapshot";
    String LIVE_STREAM = "live";
    int LIVE_STREAM_DURATION = 60000;
    String VIDEO_RES = "640x480";
    String LIVE_VIDEO_PATH = ROOTFOlDER+"/live_stream/streams/dumy.mp4";
    String WEBCAM_FEED = ROOTFOlDER+"/live_stream/feed";

    String LIVE_IP = ROOTFOlDER+"/live_stream/livemulticast.json";
    String LIVE_STREAM_PATH = ROOTFOlDER+"/live_stream/streams";

}
