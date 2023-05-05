package com.cloudwebrtc.webrtc.record;

import org.webrtc.PeerConnection;
import org.webrtc.audio.JavaAudioDeviceModule;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;

public class DunpAudioFrameCapturer implements JavaAudioDeviceModule.SamplesReadyCallback {

    Integer _audioFrameCaptureId;
    AudioChannel _audioChannel;
    String _currentKey = _audioChannel + "_" + _audioFrameCaptureId;

    public DunpAudioFrameCapturer(AudioChannel audioChannel, Integer audioFrameCaptureId) {
        _audioFrameCaptureId = audioFrameCaptureId;
        _audioChannel = audioChannel;
        _currentKey = _audioChannel + "_" + _audioFrameCaptureId;
    }

    static JavaAudioDeviceModule audioDeviceModule;

    static java.util.concurrent.ConcurrentLinkedQueue<int[]> _frameCapturedInput = new java.util.concurrent.ConcurrentLinkedQueue<>();
    static java.util.concurrent.ConcurrentLinkedQueue<int[]> _frameCapturedOutput = new java.util.concurrent.ConcurrentLinkedQueue<>();

    static PeerConnection _peerConnection;
    static String _peerConnectionId;

    static BinaryMessenger _binaryMessenger;
    static EventChannel _eventChannel;

    static EventChannel.EventSink _attachEvent;

    static Handler _handlerUiThread;
    static final Runnable _runnableSent2FlutterUi = new Runnable() {
        @Override
        public void run() {
            try {
                //todo: _frameCaptured should be long to each trackIdtimer

                int qsize = _frameCapturedInput.size();
                //Log.i("DunpFrame StartCapture timer ",_mapTrackCaputrer.size()+" "+ _listTimer.size()+" qs "+qsize);

                if (qsize > 0) {
                    int[] temp = _frameCapturedInput.poll();
                    if (temp != null)
                        _handlerUiThread.post(() -> _attachEvent.success(temp));
                }
                qsize = _frameCapturedOutput.size();
                if (qsize > 0) {
                    int[] temp = _frameCapturedOutput.poll();
                    if (temp != null)
                        _handlerUiThread.post(() -> _attachEvent.success(temp));
                }
                //Log.i("DunpFrameCapturer","2");
                //fire event to flutter by _attachEvent

            } catch (Exception ex) {
                Log.i("DunpAudioFrameCapturer", "ERR timer dequeue " + ex.getMessage(), ex);
            }
        }
    };

    static AudioSamplesInterceptor _inputInterceptor = null;
    static AudioSamplesInterceptor _outputInterceptor = null;

    public static void Init(AudioSamplesInterceptor inputInterceptor, AudioSamplesInterceptor outputInterceptor, JavaAudioDeviceModule audioDeviceModuleContext, String peerConnectionId, PeerConnection peerConnection, BinaryMessenger messager) {

        _inputInterceptor = inputInterceptor;
        _outputInterceptor = outputInterceptor;

        audioDeviceModule = audioDeviceModuleContext;
        _peerConnection = peerConnection;
        _peerConnectionId = peerConnectionId;
        _binaryMessenger = messager;
        _eventChannel = new EventChannel(messager, "DunpAudioFrameCapturerEventChannel");
        _eventChannel.setStreamHandler(
                new EventChannel.StreamHandler() {
                    @Override
                    public void onListen(Object args, final EventChannel.EventSink events) {
                        _attachEvent = events;
                        _handlerUiThread = new Handler(Looper.getMainLooper());
                    }

                    @Override
                    public void onCancel(Object args) {
                        _handlerUiThread.removeCallbacks(_runnableSent2FlutterUi);
                        _handlerUiThread = null;
                        _attachEvent = null;
                    }
                }
        );

        _timer = new java.util.Timer("timer_DunpAudioFrameCapturerEventChannel");

        _timer.schedule(new java.util.TimerTask() {
            public void run() {
                _runnableSent2FlutterUi.run();
            }
        }, 0L, 1L);
    }

    static java.util.Timer _timer;

    public static void Start(AudioChannel audioChannel, Integer audioFrameCaptureId) {

        Stop(audioChannel, audioFrameCaptureId);

        if (audioChannel == AudioChannel.INPUT) {
            try {
                _inputInterceptor.attachCallback(audioFrameCaptureId, new DunpAudioFrameCapturer(audioChannel, audioFrameCaptureId));
            } catch (Exception ex) {
                Log.d("DunpAudioFrameCapturer.Start", ex.getMessage(), ex);
            }
        }

        if (audioChannel == AudioChannel.OUTPUT) {
            try {
                _outputInterceptor.attachCallback(audioFrameCaptureId, new DunpAudioFrameCapturer(audioChannel, audioFrameCaptureId));
            } catch (Exception ex) {
                Log.d("DunpAudioFrameCapturer.Start", ex.getMessage(), ex);
            }
        }
        Log.d("DunpAudioFrameCapturer.Start", "Done");
    }

    public static void Stop(AudioChannel audioChannel, Integer audioFrameCaptureId) {
        try {
            if (audioChannel == AudioChannel.INPUT) {
                _inputInterceptor.detachCallback(audioFrameCaptureId);
            }
        } catch (Exception ex) {
        }
        try {
            if (audioChannel == AudioChannel.OUTPUT) {
                _outputInterceptor.detachCallback(audioFrameCaptureId);
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public void onWebRtcAudioRecordSamplesReady(JavaAudioDeviceModule.AudioSamples audioSamples) {
        try {
            byte[] temp = audioSamples.getData();
            int[] data = new int[temp.length + 4];
            //Log.d("DunpAudioFrameCapturer.onWebRtcAudioRecordSamplesReady", " " + temp);

            java.util.concurrent.ConcurrentLinkedQueue<int[]> _frameCaptured = null;

            if (_audioChannel == AudioChannel.INPUT) {
                _frameCaptured = _frameCapturedInput;
                data[0] = 0;
            }
            if (_audioChannel == AudioChannel.OUTPUT) {
                data[0] = 1;
                _frameCaptured = _frameCapturedOutput;
            }

            data[1] = audioSamples.getAudioFormat();
            data[2] = audioSamples.getSampleRate();
            data[3] = audioSamples.getChannelCount();

            for (int i = 4; i < data.length; i++) {
                data[i] = temp[i - 4];
            }

            int qsize = _frameCaptured.size();

            //Log.i("DunpFrame StartCapture onFrame","ts "+_mapTrackCaputrer.size()+" ts "+ _listTimer.size()+" qs "+qsize);

            int lenRemain = qsize - 5000;
            if (lenRemain > 0) {
                //prevent stuck queue or too delay
                for (int i = 0; i < lenRemain; i++) {
                    _frameCaptured.poll();

                }
            }

            _frameCaptured.offer(data);
        } catch (Exception ex) {
            Log.d("DunpAudioFrameCapturer.onWebRtcAudioRecordSamplesReady", ex.getMessage(), ex);
        }


    }

}
