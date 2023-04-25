package com.cloudwebrtc.webrtc.record;

import android.util.Log;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.webrtc.PeerConnection;
import org.webrtc.VideoTrack;

import android.util.ArrayMap;

import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.YuvHelper;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.Looper;

public class DunpFrameCapturer implements VideoSink {

    public DunpFrameCapturer(){}

    static java.util.Map<String,java.util.Timer> _listTimer = new ArrayMap<>();

    static java.util.concurrent.ConcurrentLinkedQueue <int[]> _frameCaptured=new java.util.concurrent.ConcurrentLinkedQueue<>();
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

    static Handler _handlerUiThread;
    static final Runnable _runnableSent2FlutterUi = new Runnable() {
        @Override
        public void run() {
            try {
                //todo: _frameCaptured should be long to each trackIdimer

                int qsize=_frameCaptured.size();
                //Log.i("DunpFrame StartCapture timer ",_mapTrackCaputrer.size()+" "+ _listTimer.size()+" qs "+qsize);

                if(qsize==0) return;

                int[] dataImage = _frameCaptured.poll();

                if(dataImage==null)return;

                //Log.i("DunpFrameCapturer","1");
                _latestFrame= dataImage;

                _handlerUiThread.post(()->_attachEvent.success(dataImage));

                //Log.i("DunpFrameCapturer","2");
                //fire event to flutter by _attachEvent

            }catch (Exception ex){
                Log.i("DunpFrameCapturer","ERR timer dequeue "+ ex.getMessage(),ex);
            }
        }
    };

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

                        _handlerUiThread= new Handler(Looper.getMainLooper());

                    }

                    @Override
                    public void onCancel(Object args) {

                        _handlerUiThread.removeCallbacks(_runnableSent2FlutterUi);
                        _handlerUiThread =null;
                        _attachEvent=null;
                    }
                }
        );

    }

    public void StartCapture(VideoTrack track, MethodChannel.Result callback) {

        String tid=track.id();
        String keyName="DunpFrameCapture_"+tid;

        Log.i("DunpFrame StartCapture",_mapTrackCaputrer.size()+" "+ _listTimer.size());

        if(_mapTrackCaputrer.containsKey(tid)==false){
            _mapTrackCaputrer.put(tid,this);
            _mapTrackResult.put(tid, callback);
            List<VideoTrack> trackids = new ArrayList<>();
            trackids.add(track);
            _mapTracks.put(_peerConnectionId,trackids);
        }else{
            _mapTrackResult.replace(tid, callback);
            return ;
        }

        java.util.Timer timer= new  java.util.Timer(keyName);

        if(_listTimer.containsKey(tid)==false){
            _listTimer.put(tid,timer);

            timer.schedule( new java.util.TimerTask()
            {
                public void run() {
                    _runnableSent2FlutterUi.run();
                    }
            },0L, 40L);
        }

        track.addSink(this);

    }

    //todo: _frameCaptured should be long to each trackId
    static  int[] _latestFrame;
    public static int[]  get(String trackId){
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
            byte[] temp = byteArrayOutputStream.toByteArray();
            rotation=videoFrame.getRotation();
            int[] dataImage= new int[temp.length+3];
            dataImage[0]=rotation;
            dataImage[1]=width;
            dataImage[2]=height;

            for(int i=3;i< dataImage.length;i++){
                dataImage[3]=temp[i-3];
            }
            temp=null;

            int qsize=_frameCaptured.size();

            //Log.i("DunpFrame StartCapture onFrame","ts "+_mapTrackCaputrer.size()+" ts "+ _listTimer.size()+" qs "+qsize);

            int lenRemain= qsize-1000;
            if(lenRemain>0){
                //prevent stuck queue or too delay
                for(int i=0 ;i<lenRemain;i++){
                  int[] torem=  _frameCaptured.remove();
                  torem=null;
                }
            }

            _frameCaptured. offer(dataImage);

//            Bitmap frameInBmp = BitmapFactory.decodeByteArray(dataImage, 0, dataImage.length);

//            switch (rotation) {
//                case 0:
//                    _frameCaptured.put(dataImage);
//                    break;
//                case 90:
//                case 180:
//                case 270:
//
//                    Matrix matrix = new Matrix();
//                    matrix.postRotate(videoFrame.getRotation());
//                    Bitmap rotated = Bitmap.createBitmap(frameInBmp, 0, 0, frameInBmp.getWidth(), frameInBmp.getHeight(), matrix, true);
//                    java.io.ByteArrayOutputStream rotatedOutputStream = new java.io.ByteArrayOutputStream();
//                    rotated.compress(Bitmap.CompressFormat.JPEG, 100, rotatedOutputStream);
//                    Log.i("DunpFrameCapturer","6");
//                    _frameCaptured.put(rotatedOutputStream.toByteArray());
//                    break;
//                default:
//                    // Rotation is checked to always be 0, 90, 180 or 270 by VideoFrame
//                    throw new RuntimeException("Invalid rotation");
//            }
        }catch (Exception ex){
            //callback.error("IOException",ex);
            Log.i("DunpFrameCapturer","ERR "+ ex.getMessage(),ex);
        }
        yuvBuffer=null;
        cleanedArray=null;
        buffer=null;
        yuvImage=null;

        //end#dunp

    }
}
