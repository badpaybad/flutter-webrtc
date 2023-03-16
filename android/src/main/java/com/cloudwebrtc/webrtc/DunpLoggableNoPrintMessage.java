package com.cloudwebrtc.webrtc;

import android.util.Log;

import org.webrtc.Logging;

class DunpLoggableNoPrintMessage implements org.webrtc.Loggable {

    @Override
    public void onLogMessage(String s, Logging.Severity severity, String s1) {
        //BufferQueueProducer //SurfaceTexture
        if (s.indexOf("BufferQueueProducer") <= 0 || s.indexOf("SurfaceTexture") <= 0 || s.indexOf("queueBuffer")<=0) {
            Log.i("dunp", s);
            Log.i("dunp", s1);
        }
        if (s1.indexOf("BufferQueueProducer") <= 0 || s1.indexOf("SurfaceTexture") <= 0|| s.indexOf("queueBuffer")<=0) {
            Log.i("dunp", s1);
        }
    }
}
