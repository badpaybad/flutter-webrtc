import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';

import '../../flutter_webrtc.dart';

class DunpFrameCapture{

  final StreamController<Uint8List> _frameStream =StreamController<Uint8List>.broadcast();

  //todo: should each trackid video got own stream
  Stream<List<int>> get stream=>_frameStream.stream;

 static DunpFrameCapture instance = DunpFrameCapture._();

  var _eventChannel = EventChannel('DunpFrameCapturerEventChannel');

  DunpFrameCapture._(){

 }

  RTCPeerConnection? peerConnection;
  String? peerConnectionId;

  bool _isInit=false;

  Timer ? _timer;

  StreamSubscription? _streamSubscription;

  void dispose(){
    _streamSubscription?.cancel();
    _timer?.cancel();
    peerConnection?.dispose();
  }

 Future<void> init(RTCPeerConnection peerConnection,String peerConnectionId) async{
   if(_isInit) return;

   _isInit=false;

   this.peerConnection=peerConnection;
   this.peerConnectionId=peerConnectionId;
   _streamSubscription = _eventChannel.receiveBroadcastStream().listen((data) async{
     _frameStream.add(data);
   });

   _isInit=true;


 }

 Future<List<int>> getLatestFrame(String trackId)async{
   final response = await WebRTC.invokeMethod('dunpCaptureFrameOfCurrentVideoStream',
       <String, dynamic>{
         'trackId': trackId,
         'peerConnectionId': peerConnectionId
       });
   return response;
 }

}