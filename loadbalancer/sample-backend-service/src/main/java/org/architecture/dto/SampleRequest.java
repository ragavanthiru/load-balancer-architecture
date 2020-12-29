package org.architecture.dto;

public class SampleRequest {

    int delayTime;

    public SampleRequest() {
    }

    public SampleRequest(int delayTime) {
        this.delayTime = delayTime;
    }

    public int getDelayTime() {
        return delayTime;
    }

    public void setDelayTime(int delayTime) {
        this.delayTime = delayTime;
    }
}
