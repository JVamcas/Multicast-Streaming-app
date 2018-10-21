package client.core;

/**
 * This is an interface of constants used by the client application
 *
 * Project: Adaptive transcoding of multi-definition multicasted video stream
 * Dependencies: none
 * @author Petrus Kambala
 * Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */

public interface Constants {
    String ROOTFOLDER = "res/client";
    enum stream_type {LIVE,VOD}
    String CLIENT_IP = "192.168.10.2";
    String VOD_PATH = ROOTFOLDER+"/videos";
    String LIVE_STREAM_PATH = ROOTFOLDER+ "/live_stream";
    String RESULTS  = ROOTFOLDER+"/results.txt";

    String CONFIG_FILE = ROOTFOLDER+"/configs/udp.xml";
    int WAIT_DATA_LEN = 0;
    int REQUEST_VIDEO = 2;
    int WAIT_MULTICAST_GROUP = 3;
    int QUIT = 5;

    String RECEIVE_SEG = "rec", REQUEST_SEG = "rec_seg";

    int BUF_MAX = 11;
    int BUF_3 = 4;
    int BUF_2 = 3;
    int BUF_1 = 1;

    double SENSITIVITY  = 0.65 , SCALER = 0.75;



    int VoD_MAX_BITRATE = 1024;
    int VoD_DEFAULT_MULTICAST = 128;

    int LIVE_MAX_BITRATE = 640;
    int LIVE_DEFAULT_BITRATE = 128;

    int LIVE_VOL = 4;
    int VoD_VOL = 2;

    int PKT_SIZE = 1400;

    String SEG_START = "seg_start";
    String STREAM_END = "stream_end";

    int PORT_NUMBER = 4000;
    String HOST = "192.168.122.1";

    int FPS = 30;
    String RESOL = "640x640";
    int REQUEST_SNAP = 1;
    int RECEIVE_SNAP = 2;

    String SNAP = "snapshot";


    String LIVE_STREAM = "live";

}