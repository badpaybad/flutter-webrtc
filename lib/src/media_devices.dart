import 'package:flutter/cupertino.dart';

import '../flutter_webrtc.dart';

class MediaDevices {
  @Deprecated(
      'Use the navigator.mediaDevices.getUserMedia(Map<String, dynamic>) provide from the factory instead')
  static Future<MediaStream> getUserMedia(
      Map<String, dynamic> mediaConstraints) async {
    try {
      return navigator.mediaDevices.getUserMedia(mediaConstraints);
    } catch (ex) {
      print("ERR MediaDevices.getUserMedia $ex");
      return MediaStreamError();
    }
  }

  @Deprecated(
      'Use the navigator.mediaDevices.getDisplayMedia(Map<String, dynamic>) provide from the factory instead')
  static Future<MediaStream> getDisplayMedia(
      Map<String, dynamic> mediaConstraints) async {
    try {
      return navigator.mediaDevices.getDisplayMedia(mediaConstraints);
    } catch (ex) {
      print("ERR MediaDevices.getDisplayMedia $ex");
      return MediaStreamError();
    }
  }

  @Deprecated(
      'Use the navigator.mediaDevices.getSources() provide from the factory instead')
  static Future<List<dynamic>> getSources() async {
    try {
      return navigator.mediaDevices.getSources();
    } catch (ex) {
      print("ERR MediaDevices.getSources $ex");
      return [];
    }
  }
}

class MediaStreamError extends MediaStream {
  MediaStreamError() : super("ERROR", "ERROR");

  @override
  bool? get active => false;

  @override
  Future<void> addTrack(MediaStreamTrack track,
      {bool addToNative = true}) async {
    print("MediaStreamError addTrack");
  }

  @override
  Future<MediaStream> clone() async {
    print("MediaStreamError clone");
    return this;
  }

  @override
  List<MediaStreamTrack> getAudioTracks() {
    print("MediaStreamError getAudioTracks");
    return [];
  }

  @override
  Future<void> getMediaTracks() async {
    print("MediaStreamError getMediaTracks");
  }

  @override
  List<MediaStreamTrack> getTracks() {
    print("MediaStreamError getTracks");
    return [];
  }

  @override
  List<MediaStreamTrack> getVideoTracks() {
    print("MediaStreamError getVideoTracks");
    return [];
  }

  @override
  Future<void> removeTrack(MediaStreamTrack track,
      {bool removeFromNative = true}) async {
    print("MediaStreamError removeTrack");
  }
}
