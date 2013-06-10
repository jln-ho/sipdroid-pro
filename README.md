sipdroid-pro
============

A modified version of Sipdroid v2.9 with support for AAC and Opus as well as file streaming (wav / aac / mp4 / mp3) and packet duplication / interleaving

AAC support was implemented using the Fraunhofer FDK AAC library (http://www.iis.fraunhofer.de/en/bf/amm/implementierungen/fdkaaccodec.html)
Opus support was implemented using libopus v1.0.1 (http://www.opus-codec.org)
Both codecs can be configured precisely for outgoing calls (samplingrate, bitrate, audio profile etc.)

The basis for the file streaming extension was taken from extended-sipdroid (https://github.com/codevise/extended-sipdroid) and extended to support .wav, .aac and .mp4 files in addition to .mp3 files
The file streaming extension can be accessed through the newly added "file controls" button obove the in-call dialpad

INSTALLATION
============

Before deploying Sipdroid pro, make sure to download and install ffmpeg for Android using jni/ffmpeg/FFmpeg-Android.sh 

Copy the resulting ffmpeg.so to jni/ffmpeg/lib.

Then compile the remaining native libs with the Android NDK.
