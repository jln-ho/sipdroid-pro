sipdroid-pro
============

A modified version of Sipdroid v2.9 with support for AAC and Opus as well as file streaming (wav / aac / mp4 / mp3) and packet duplication / interleaving

AAC support was implemented using the [Fraunhofer FDK AAC library](http://www.iis.fraunhofer.de/en/bf/amm/implementierungen/fdkaaccodec.html)

Opus support was implemented using [libopus v1.0.1](http://www.opus-codec.org)

The basis for the file streaming extension was taken from [extended-sipdroid](https://github.com/codevise/extended-sipdroid) and was further extended to support .wav, .aac and .mp4 files in addition to .mp3 files.
This extension can be accessed through the newly added _file controls_ button obove the in-call dialpad

INSTALLATION
============

Before deploying Sipdroid pro, make sure to download and install ffmpeg for Android using [`jni/ffmpeg/FFmpeg-Android.sh`](https://github.com/juho0006/sipdroid-pro/blob/master/jni/ffmpeg/FFmpeg-Android.sh) 

Copy the resulting `ffmpeg.so` to `jni/ffmpeg/lib`.

Then compile the remaining native libs with the Android NDK.
