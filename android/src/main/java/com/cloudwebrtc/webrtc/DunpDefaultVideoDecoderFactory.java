package com.cloudwebrtc.webrtc;

import android.util.Log;

import androidx.annotation.Nullable;

import org.webrtc.EglBase;
import org.webrtc.HardwareVideoDecoderFactory;
import org.webrtc.PlatformSoftwareVideoDecoderFactory;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoDecoder;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoDecoderFallback;

import java.util.Arrays;
import java.util.LinkedHashSet;

public class DunpDefaultVideoDecoderFactory implements VideoDecoderFactory {
    private final VideoDecoderFactory hardwareVideoDecoderFactory;
    private final VideoDecoderFactory softwareVideoDecoderFactory = new SoftwareVideoDecoderFactory();
    @Nullable
    private final VideoDecoderFactory platformSoftwareVideoDecoderFactory;



    public DunpDefaultVideoDecoderFactory(@Nullable EglBase.Context eglContext) {
        this.hardwareVideoDecoderFactory = new HardwareVideoDecoderFactory(eglContext);
        this.platformSoftwareVideoDecoderFactory = new PlatformSoftwareVideoDecoderFactory(eglContext);

    }

    @Nullable
    public VideoDecoder createDecoder(VideoCodecInfo codecType) {
        VideoDecoder softwareDecoder = this.softwareVideoDecoderFactory.createDecoder(codecType);

        VideoDecoder hardwareDecoder = this.hardwareVideoDecoderFactory.createDecoder(codecType);

        if(DunpPeerConnectionContext.videoDecoder1Hardware2Software3Fallback==1){

            if (hardwareDecoder != null) {
                Log.i("dunp", "dunp----------------------- hardwareDecoder NOT null");
                return hardwareDecoder;
            }
        }

        if(DunpPeerConnectionContext.videoDecoder1Hardware2Software3Fallback==2){

            if (softwareDecoder != null) {
                Log.i("dunp", "dunp----------------------- softwareDecoder NOT null");
                return softwareDecoder;
            }

            if (softwareDecoder == null && this.platformSoftwareVideoDecoderFactory != null) {
                softwareDecoder = this.platformSoftwareVideoDecoderFactory.createDecoder(codecType);
            }

            if (softwareDecoder != null) {
                Log.i("dunp", "dunp----------------------- softwareDecoderPlatform NOT null");
                return softwareDecoder;
            }
        }

        if (hardwareDecoder != null && softwareDecoder != null) {
           return new VideoDecoderFallback(softwareDecoder, hardwareDecoder);
        } else {
            Log.i("dunp", "dunp----------------------- return softwareDecoder!=null?softwareDecoder:hardwareDecoder;");
           return hardwareDecoder !=null?hardwareDecoder:softwareDecoder;
        }
    }

    public VideoCodecInfo[] getSupportedCodecs() {
        LinkedHashSet<VideoCodecInfo> supportedCodecInfos = new LinkedHashSet();
        supportedCodecInfos.addAll(Arrays.asList(this.softwareVideoDecoderFactory.getSupportedCodecs()));
        supportedCodecInfos.addAll(Arrays.asList(this.hardwareVideoDecoderFactory.getSupportedCodecs()));
        if (this.platformSoftwareVideoDecoderFactory != null) {
            supportedCodecInfos.addAll(Arrays.asList(this.platformSoftwareVideoDecoderFactory.getSupportedCodecs()));
        }

        return (VideoCodecInfo[]) supportedCodecInfos.toArray(new VideoCodecInfo[supportedCodecInfos.size()]);
    }
}
