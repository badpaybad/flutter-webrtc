import 'dart:async';

import 'dart:typed_data';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import '../../flutter_webrtc.dart';

class DunpFrameCapture {
  static DunpFrameCapture instance = DunpFrameCapture._();

  final _eventChannel = EventChannel('DunpFrameCapturerEventChannel');

  ValueNotifier<DunpFrameCaptured> frameNotifier =
      ValueNotifier<DunpFrameCaptured>(DunpFrameCaptured(trackId: "",
          width: 0, height: 0, rotation: 0, data: Uint8List.fromList([])));

  DunpFrameCapture._() {}

  RTCPeerConnection? peerConnection;
  String? peerConnectionId;

  bool _isInit = false;

  Timer? _timer;

  StreamSubscription? _streamSubscription;

  void dispose() {
    _streamSubscription?.cancel();
    _timer?.cancel();
    peerConnection?.dispose();
  }

  Future<void> init(
      RTCPeerConnection peerConnection, String peerConnectionId) async {
    if (_isInit) return;

    _isInit = false;

    this.peerConnection = peerConnection;
    this.peerConnectionId = peerConnectionId;
    _streamSubscription =
        _eventChannel.receiveBroadcastStream().listen((dataRaw) async {
      try {
        //
        if (dataRaw == null) return;

        dataRaw= dataRaw as Map<dynamic,dynamic>;

        String trackid= dataRaw.keys.first;
        var data= dataRaw.values.first;

        var rotation = data[0];
        var width = data[1];
        var height = data[2];
        var temp = Uint8List.fromList(List<int>.from(data).skip(3).toList());

        frameNotifier.value = DunpFrameCaptured(trackId: trackid,
            width: width, height: height, rotation: rotation, data: temp);
      } catch (ex) {
        print("receiveBroadcastStream from java ERR $ex \r\n${dataRaw}");
      }
    });

    _isInit = true;
  }
}

class DunpFrameCaptured {
  DunpFrameCaptured(
      {required this.trackId,
        required this.width,
      required this.height,
      required this.rotation,
      required this.data,
      DateTime? createdAt,
      UniqueKey? id})
      : this.createdAt = createdAt ?? DateTime.now(),
        this.id = id ?? UniqueKey();

  UniqueKey id;
  int width;
  int height;
  Uint8List data;
  int rotation;
  DateTime createdAt ;
  String trackId;

  @override
  String toString() {
    return "id: $id width: $width height: $height rotation: $rotation createdAt: $createdAt data len: ${data.length}";
  }
}
