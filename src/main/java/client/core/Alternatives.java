package client.core;

/**
 * This is helper class maps the video bitrate to a multicast group ip address and socket number
 *
 * Project: Adaptive transcoding of multi-definition multicasted video stream
 * Dependencies: none
 * @author Petrus Kambala
 * Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */

public class Alternatives {
    String BITRATE;
    String SOCKET_NUMBER;
    String GROUP_IP_ADDRESS;

    public String getCLUSTER_NAME() {
        return CLUSTER_NAME;
    }

    public void setCLUSTER_NAME(String CLUSTER_NAME) {
        this.CLUSTER_NAME = CLUSTER_NAME;
    }

    String CLUSTER_NAME;

    public String getBITRATE() {
        return BITRATE;
    }

    public void setBITRATE(String BITRATE) {
        this.BITRATE = BITRATE;
    }

    public String getSOCKET_NUMBER() {
        return SOCKET_NUMBER;
    }

    public void setSOCKET_NUMBER(String SOCKET_NUMBER) {
        this.SOCKET_NUMBER = SOCKET_NUMBER;
    }

    public String getGROUP_IP_ADDRESS() {
        return GROUP_IP_ADDRESS;
    }

    public void setGROUP_IP_ADDRESS(String GROUP_IP_ADDRESS) {
        this.GROUP_IP_ADDRESS = GROUP_IP_ADDRESS;
    }
}
