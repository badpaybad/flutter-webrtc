package com.cloudwebrtc.webrtc.record;

import android.util.Log;
import androidx.annotation.Nullable;

import com.cloudwebrtc.webrtc.audio.AudioSwitchManager;
import com.cloudwebrtc.webrtc.utils.AnyThreadSink;
import com.cloudwebrtc.webrtc.utils.ConstraintsArray;
import com.cloudwebrtc.webrtc.utils.ConstraintsMap;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.webrtc.AudioTrack;
import org.webrtc.CandidatePairChangeEvent;
import org.webrtc.DataChannel;
import org.webrtc.DtmfSender;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.RTCStats;
import org.webrtc.RTCStatsCollectorCallback;
import org.webrtc.RTCStatsReport;
import org.webrtc.RtpCapabilities;
import org.webrtc.RtpParameters;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.RtpTransceiver;
import org.webrtc.StatsObserver;
import org.webrtc.StatsReport;
import org.webrtc.VideoTrack;

import android.util.ArrayMap;

import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.VideoTrack;
import org.webrtc.YuvHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.Looper;

public class DunpFrameCapturer implements VideoSink {

    static java.util.Map<String,java.util.Timer> _listTimer = new ArrayMap<>();

    static java.util.concurrent.PriorityBlockingQueue<byte[]> _frameCaptured=new java.util.concurrent.PriorityBlockingQueue<byte[]>();
    public void dispose(){

        this.Close(_peerConnectionId);

        if(_peerConnection!=null){
            _peerConnection.dispose();
        }
        _frameCaptured.clear();

        _attachEvent = null;
    }

    public static void Close(String peerConnectionId){
        _mapTrackCaputrer.clear();
        _mapTrackResult.clear();
        _mapTracks.remove(peerConnectionId);
        _listTimer.get(peerConnectionId).cancel();
        _listTimer.remove(peerConnectionId);
    }

    static PeerConnection _peerConnection;
    static String _peerConnectionId;

    static BinaryMessenger _binaryMessenger;
    static  EventChannel _eventChannel;

    static java.util.Map<String,DunpFrameCapturer> _mapTrackCaputrer =new ArrayMap<>();
    static java.util.Map<String,MethodChannel.Result > _mapTrackResult =new ArrayMap<>();
    static java.util.Map<String, List<VideoTrack>> _mapTracks = new ArrayMap<>();

    static EventChannel.EventSink _attachEvent;

    public static void Init(String peerConnectionId, PeerConnection peerConnection, BinaryMessenger messager){
        _peerConnection= peerConnection;
        _peerConnectionId= peerConnectionId;
        _binaryMessenger=messager;

        _eventChannel= new EventChannel(messager, "DunpFrameCapturerEventChannel");
        _eventChannel.setStreamHandler(
                new EventChannel.StreamHandler() {
                    @Override
                    public void onListen(Object args, final EventChannel.EventSink events) {
                        _attachEvent = events;
                    }

                    @Override
                    public void onCancel(Object args) {
                        _attachEvent=null;
                    }
                }
        );

    }

    public DunpFrameCapturer(VideoTrack track, MethodChannel.Result callback) {

        String tid=track.id();

        if(_mapTrackCaputrer.containsKey(tid)==false){
            _mapTrackCaputrer.put(tid,this);
            _mapTrackResult.put(tid, callback);
            List<VideoTrack> trackids = new ArrayList<>();
            trackids.add(track);
            _mapTracks.put(_peerConnectionId,trackids);
        }else{
            _mapTrackResult.replace(tid, callback);
            List<VideoTrack> trackids= _mapTracks.get(_peerConnectionId);
            trackids.add(track);
            return;
        }

        java.util.Timer timer= new  java.util.Timer("DunpFrameCapture_"+tid);

        if(_listTimer.containsKey(tid)==false){
            _listTimer.put(tid,timer);

        timer.schedule( new java.util.TimerTask() {
            public void run() {
                //todo: _frameCaptured should be long to each trackId
                int lenRemain= _frameCaptured.size()-1000;
                try {
                if(lenRemain>0){
                    //prevent stuck queue or too delay
                    for(int i=0 ;i<lenRemain;i++){
                        _frameCaptured.remove();
                    }
                }

                byte[] dataImage = _frameCaptured.remove();
                    _latestFrame= dataImage;
                _attachEvent.success(dataImage);
                //fire event to flutter by _attachEvent

                }catch (Exception ex){}

                }
            },40L);
        }

        track.addSink(this);

    }

    //todo: _frameCaptured should be long to each trackId
    static  byte[] _latestFrame;
    public static byte[]  get(String trackId){
       return _latestFrame;
    }

    int width;
    int height;
    int rotation;
    @Override
    public void onFrame(VideoFrame videoFrame) {

        videoFrame.retain();
        VideoFrame.Buffer buffer = videoFrame.getBuffer();
        VideoFrame.I420Buffer i420Buffer = buffer.toI420();
        ByteBuffer y = i420Buffer.getDataY();
        ByteBuffer u = i420Buffer.getDataU();
        ByteBuffer v = i420Buffer.getDataV();
         width = i420Buffer.getWidth();
         height = i420Buffer.getHeight();
        int[] strides = new int[] {
                i420Buffer.getStrideY(),
                i420Buffer.getStrideU(),
                i420Buffer.getStrideV()
        };
        final int chromaWidth = (width + 1) / 2;
        final int chromaHeight = (height + 1) / 2;
        final int minSize = width * height + chromaWidth * chromaHeight * 2;

        ByteBuffer yuvBuffer = ByteBuffer.allocateDirect(minSize);
        // NV21 is the same as NV12, only that V and U are stored in the reverse oder
        // NV21 (YYYYYYYYY:VUVU)
        // NV12 (YYYYYYYYY:UVUV)
        // Therefore we can use the NV12 helper, but swap the U and V input buffers
        YuvHelper.I420ToNV12(y, strides[0], v, strides[2], u, strides[1], yuvBuffer, width, height);

        // For some reason the ByteBuffer may have leading 0. We remove them as
        // otherwise the
        // image will be shifted
        byte[] cleanedArray = Arrays.copyOfRange(yuvBuffer.array(), yuvBuffer.arrayOffset(), minSize);

        YuvImage yuvImage = new YuvImage(
                cleanedArray,
                ImageFormat.NV21,
                width,
                height,
                // We omit the strides here. If they were included, the resulting image would
                // have its colors offset.
                null);
        i420Buffer.release();
        videoFrame.release();

//        new Handler(Looper.getMainLooper()).post(() -> {
//            videoTrack.removeSink(this);
//        });

        //begin#dunp
        try {

            java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
            yuvImage.compressToJpeg(
                    new Rect(0, 0, width, height),
                    100,
                    byteArrayOutputStream
            );
            byte[] dataImage = byteArrayOutputStream.toByteArray();
            Bitmap frameInBmp = BitmapFactory.decodeByteArray(dataImage, 0, dataImage.length);
            rotation=videoFrame.getRotation();
            switch (rotation) {
                case 0:
                    _frameCaptured.put(dataImage);
                    break;
                case 90:
                case 180:
                case 270:

                    Matrix matrix = new Matrix();
                    matrix.postRotate(videoFrame.getRotation());
                    Bitmap rotated = Bitmap.createBitmap(frameInBmp, 0, 0, frameInBmp.getWidth(), frameInBmp.getHeight(), matrix, true);
                    java.io.ByteArrayOutputStream rotatedOutputStream = new java.io.ByteArrayOutputStream();
                    rotated.compress(Bitmap.CompressFormat.JPEG, 100, rotatedOutputStream);

                    _frameCaptured.put(rotatedOutputStream.toByteArray());
                    break;
                default:
                    // Rotation is checked to always be 0, 90, 180 or 270 by VideoFrame
                    throw new RuntimeException("Invalid rotation");
            }
            //callback.success(null);
        }catch (Exception ex){
            //callback.error("IOException",ex);
        }
        //end#dunp

    }
}
