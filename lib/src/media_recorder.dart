import 'package:webrtc_interface/webrtc_interface.dart' as rtc;

import '../flutter_webrtc.dart';

class MediaRecorder extends rtc.MediaRecorder {
  MediaRecorder() : _delegate = mediaRecorder();
  final rtc.MediaRecorder _delegate;

  @override
  Future<void> start(String path,
      {MediaStreamTrack? videoTrack,
      RecorderAudioChannel? audioChannel}) async {
    try {
      _delegate.start(path, videoTrack: videoTrack, audioChannel: audioChannel);
    } catch (ex) {
      print("ERR MediaRecorder.start $ex");
    }
  }

  @override
  Future stop() async {
    try {
      _delegate.stop();
    } catch (ex) {
      print("ERR MediaRecorder.stop $ex");
    }
  }

  @override
  void startWeb(
    MediaStream stream, {
    Function(dynamic blob, bool isLastOne)? onDataChunk,
    String? mimeType,
    int timeSlice = 1000,
  }) async {
    try {
      _delegate.startWeb(
        stream,
        onDataChunk: onDataChunk,
        mimeType: mimeType ?? 'video/webm',
        timeSlice: timeSlice,
      );
    } catch (ex) {
      print("ERR MediaRecorder.startWeb $ex");
    }
  }
}
