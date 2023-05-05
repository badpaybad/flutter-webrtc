import 'dart:async';

import 'dart:typed_data';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import '../../flutter_webrtc.dart';

class DunpAudioFrameCapture {

  DunpAudioFrameCapture._() {}
  static DunpAudioFrameCapture instance = DunpAudioFrameCapture._();

  final _eventChannel = EventChannel('DunpAudioFrameCapturerEventChannel');

  ValueNotifier<DunpAudioFrameCaptured> frameNotifierInput =
      ValueNotifier<DunpAudioFrameCaptured>(DunpAudioFrameCaptured(
          audioChannel: 0,
          audioFormat: 0,
          audioChannelCount: 0,
          audioSample: 0,
          data: Uint8List.fromList([])));

  ValueNotifier<DunpAudioFrameCaptured> frameNotifierOutput =
      ValueNotifier<DunpAudioFrameCaptured>(DunpAudioFrameCaptured(
          audioChannel: 0,
          audioFormat: 0,
          audioChannelCount: 0,
          audioSample: 0,
          data: Uint8List.fromList([])));

  RTCPeerConnection? peerConnection;
  String? peerConnectionId;

  bool _isInit = false;
  StreamSubscription? _streamSubscription;

  void dispose() {
    _streamSubscription?.cancel();
    peerConnection?.dispose();
  }

  Future<void> init(
      RTCPeerConnection peerConnection, String peerConnectionId) async {
    if (_isInit) return;

    _isInit = false;

    this.peerConnection = peerConnection;
    this.peerConnectionId = peerConnectionId;
    _streamSubscription =
        _eventChannel.receiveBroadcastStream().listen((data) async {
      try {
        //
        if (data == null) return;

        var audioChannel = data[0];
        var audioFormat = data[1];
        var audioSample = data[2];
        var audioChannelCount = data[3];

        var temp = Uint8List.fromList(List<int>.from(data).skip(4).toList());
        if (audioChannel == 0) {
          frameNotifierInput.value = DunpAudioFrameCaptured(
              audioChannelCount: audioChannelCount,
              audioSample: audioSample,
              audioFormat: audioFormat,
              audioChannel: audioChannel,
              data: temp);
        }
        if (audioChannel == 1) {
          frameNotifierOutput.value = DunpAudioFrameCaptured(
              audioChannelCount: audioChannelCount,
              audioSample: audioSample,
              audioFormat: audioFormat,
              audioChannel: audioChannel,
              data: temp);
        }
      } catch (ex) {
        print("receiveBroadcastStream from java ERR $ex \r\n${data}");
      }
    });

    _isInit = true;
  }
}

class DunpAudioFrameCaptured {

  DunpAudioFrameCaptured(
      {required this.audioChannel,
      required this.audioChannelCount,
      required this.audioFormat,
      required this.audioSample,
      required this.data,
      DateTime? createdAt})
      : this.createdAt = createdAt ?? DateTime.now();
  int audioChannel;
  int audioFormat;
  int audioSample;
  int audioChannelCount;
  DateTime createdAt;
  Uint8List data;


  @override
  String toString() {
    return "audioChannel: $audioChannel audioFormat: $audioFormat audioSample: $audioSample audioChannelCount: $audioChannelCount createdAt: $createdAt data len: ${data.length}";
  }
}
