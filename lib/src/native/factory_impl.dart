import 'dart:async';
import 'dart:core';
import 'dart:typed_data';
import 'package:flutter_webrtc/src/native/DunpFrameCapture.dart';
import 'package:flutter_webrtc/src/native/mediadevices_impl.dart';
import 'package:webrtc_interface/webrtc_interface.dart';

import '../desktop_capturer.dart';
import 'DunpAudioFrameCapture.dart';
import 'desktop_capturer_impl.dart';
import 'media_recorder_impl.dart';
import 'media_stream_impl.dart';
import 'navigator_impl.dart';
import 'rtc_peerconnection_impl.dart';
import 'rtc_video_renderer_impl.dart';
import 'utils.dart';

class RTCFactoryNative extends RTCFactory {
  RTCFactoryNative._internal();

  static final RTCFactory instance = RTCFactoryNative._internal();

  @override
  Future<MediaStream> createLocalMediaStream(String label) async {
    final response = await WebRTC.invokeMethod('createLocalMediaStream');
    if (response == null) {
      throw Exception('createLocalMediaStream return null, something wrong');
    }
    return MediaStreamNative(response['streamId'], label);
  }

  @override
  Future<RTCPeerConnection> createPeerConnection(
      Map<String, dynamic> configuration,
      [Map<String, dynamic> constraints = const {}]) async {
    print("------------dunp-------------------");

    var defaultConstraints = <String, dynamic>{
      'mandatory': {},
      'optional': [
        {'DtlsSrtpKeyAgreement': true},
      ],
    };

    final response = await WebRTC.invokeMethod(
      'createPeerConnection',
      <String, dynamic>{
        'configuration': configuration,
        'constraints': constraints.isEmpty ? defaultConstraints : constraints
      },
    );

    String peerConnectionId = response['peerConnectionId'];
    var peerConnect = RTCPeerConnectionNative(peerConnectionId, configuration);

    await DunpFrameCapture.instance.init(peerConnect, peerConnectionId);

    await DunpAudioFrameCapture.instance.init(peerConnect, peerConnectionId);

    return peerConnect;
  }

  @override
  MediaRecorder mediaRecorder() {
    return MediaRecorderNative();
  }

  @override
  VideoRenderer videoRenderer() {
    return RTCVideoRenderer();
  }

  @override
  Navigator get navigator => NavigatorNative.instance;

  @override
  Future<RTCRtpCapabilities> getRtpReceiverCapabilities(String kind) async {
    final response = await WebRTC.invokeMethod(
      'getRtpReceiverCapabilities',
      <String, dynamic>{
        'kind': kind,
      },
    );
    return RTCRtpCapabilities.fromMap(response);
  }

  @override
  Future<RTCRtpCapabilities> getRtpSenderCapabilities(String kind) async {
    final response = await WebRTC.invokeMethod(
      'getRtpSenderCapabilities',
      <String, dynamic>{
        'kind': kind,
      },
    );
    return RTCRtpCapabilities.fromMap(response);
  }
}

/**
 * have to call this to listen onFrame DunpFrameCapture
 */
Future<void> dunpCaptureFrameOfCurrentVideoStream(
    String trackId, String peerConnectionId,
    {Future<void> Function(DunpFrameCaptured)? onFrame}) async {
  if (onFrame != null) {
    DunpFrameCapture.instance.frameNotifier.addListener(() async {
      // print("meCapture.instance.frameNotifier.addListener ${DunpFrameCapture.instance.frameNotifier.value}");

      await onFrame(DunpFrameCapture.instance.frameNotifier.value);
    });
  }

  await WebRTC.invokeMethod(
    'dunpCaptureFrameOfCurrentVideoStream',
    <String, dynamic>{'trackId': trackId, 'peerConnectionId': peerConnectionId},
  );
}


Future<void> dunpCaptureFrameOfCurrentAudioStream(String peerConnectionId,
    {Future<void> Function(DunpAudioFrameCaptured)? onInputCapture,Future<void> Function(DunpAudioFrameCaptured)? onOutputCapture}) async {
  if (onInputCapture != null) {
    DunpAudioFrameCapture.instance.frameNotifierInput.addListener(() async {
      await onInputCapture(DunpAudioFrameCapture.instance.frameNotifierInput.value);
    });
  }
  if (onOutputCapture != null) {
    DunpAudioFrameCapture.instance.frameNotifierOutput.addListener(() async {
      await onOutputCapture(DunpAudioFrameCapture.instance.frameNotifierOutput.value);
    });
  }
  await WebRTC.invokeMethod(
    'dunpCaptureFrameOfCurrentAudioStream',
    <String, dynamic>{'peerConnectionId': peerConnectionId},
  );
}


Future<void> initPeerConnectionFactory(Map<String, dynamic> args) async {
  /*
  * args
     {
        "decoders":{
          "video": 2 //1: hardware, 2: soft, 3: fallback
        },
        "logs":{
          "traceEnable":0
        }
      }
  */
  final response = await WebRTC.invokeMethod(
    'initPeerConnectionFactory',
    args,
  );
  print("dunp initPeerConnectionFactory: $args");
}

Future<RTCPeerConnection> createPeerConnection(
    Map<String, dynamic> configuration,
    [Map<String, dynamic> constraints = const {}]) async {
  return RTCFactoryNative.instance
      .createPeerConnection(configuration, constraints);
}

Future<MediaStream> createLocalMediaStream(String label) async {
  return RTCFactoryNative.instance.createLocalMediaStream(label);
}

Future<RTCRtpCapabilities> getRtpReceiverCapabilities(String kind) async {
  return RTCFactoryNative.instance.getRtpReceiverCapabilities(kind);
}

Future<RTCRtpCapabilities> getRtpSenderCapabilities(String kind) async {
  return RTCFactoryNative.instance.getRtpSenderCapabilities(kind);
}

MediaRecorder mediaRecorder() {
  return RTCFactoryNative.instance.mediaRecorder();
}

Navigator get navigator => RTCFactoryNative.instance.navigator;

DesktopCapturer get desktopCapturer => DesktopCapturerNative.instance;

MediaDevices get mediaDevices => MediaDeviceNative.instance;
