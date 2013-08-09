sipdroid-pro
============

A modified version of Sipdroid v2.9 with support for AAC and Opus as well as file streaming (wav / aac / mp4 / mp3) and packet duplication / interleaving

AAC support was implemented using the [Fraunhofer FDK AAC library](http://www.iis.fraunhofer.de/en/bf/amm/implementierungen/fdkaaccodec.html)

Opus support was implemented using [libopus v1.0.1](http://www.opus-codec.org)

The basis for the file streaming extension was taken from [extended-sipdroid](https://github.com/codevise/extended-sipdroid) and was further extended to support .wav, .aac and .mp4 files in addition to .mp3 files.
This extension can be accessed through the newly added _file controls_ button above the in-call dialpad

Other libraries used:
* [libsamplerate](http://www.mega-nerd.com/SRC/) for PCM/WAV resampling
* [ffmpeg](http://www.ffmpeg.org/) for decoding .mp4 files
* [mpg123](http://mpg123.org) for decoding .mp3 files

Installation from source
============

* Before deploying Sipdroid pro, make sure to download and install ffmpeg for Android using [`jni/ffmpeg/FFmpeg-Android.sh`](https://github.com/juho0006/sipdroid-pro/blob/master/jni/ffmpeg/FFmpeg-Android.sh) 
Copy the resulting `ffmpeg.so` to `jni/ffmpeg/lib` and to `libs/` in your project's root directory.

* Compile the remaining native libs with the Android NDK and make sure they are in `libs/` aswell.
* Deploy

Usage
============
Before using Sipdroid, you should first read the FAQ in the [sipdroid wiki](http://code.google.com/p/sipdroid/wiki/FAQ)

* **Adding a SIP account**

  In order to place or receive calls with Sipdroid, you must first enter your SIP account's credentials under _Settings > Sip Account (Line1 or Line2)_.
  Remember to check _Use WLAN_ and/or _Use 3G_. 
  
  Once Sipdroid has successfully signed into an account, a small green icon will appear in the Android statusbar.
  Sipdroid can sign into two SIP accounts at the same time. The account entered in _Line1_ will be the preferred one by default. This means that outgoing calls will be placed using this account, while incoming calls to both accounts can be received. 
  
  The preferred account can be changed by pulling down the Android statusbar and touching the desired account.
  
* **Setting up the codecs**

  Sipdroid-pro supports 4 main codecs: AAC, Opus, G.722 and G.711 (PCMA).
  If and when a certain codec shall be used can be defined under _Settings > Audio Codecs_.

  The G.722 and G.711 codecs only have one mode : 64 kbit/s at 16 kHz (G.722) or 8 kHz (G.711).
  AAC and Opus are more versatile, so each has their own settings menu which can be accessed from the main Sipdroid dialing screen. From here one can set a range of codec-specific parameters for the next outgoing call. 
  
  For AAC, one can choose between 4 profiles (AAC-LC, HE-AAC, AAC-LD and AAC-ELD), 4 bitrates from 24 kbit/s to 64 kbit/s and 3 sampling rates: 32, 44.1 and 48 kHz.
  
  For Opus, one can choose between 3 profiles (AUDIO, VOIP and LOW-DELAY), 3 frame sizes (20ms, 40ms and 60ms) and 5 sampling rates from 8 to 48 kHz.
  
* **Placing calls**
  
  To place a call, simply enter the callee's phone number or SIP URI in the corresponding text field of the dialling screen and press _send_. Enabling packet duplication and/or interleaving improves the stability of the stream while doubling the required data rates.

  In order to call certain contacts with a specific codec regardless of the current codec configuration, one can set up a preset under _Settings > Codec Presets_.
  
* **Streaming audio files**

  To stream an audio file during a call, open up the dialpad by pulling up the small tab at the bottom of the screen. Then select _file controls_. Here, one can select a file from the android device's internal storage. 
  
  Supported file types are .wav (16 bit, mono/stereo), .mp3, .aac and .m4a. 
  
  Once play is pressed, the stream is initiated and the microphone is automatically muted. It can be re-enabled, though, in order to 'talk over the stream'. One can also monitor both the incoming and the outgoing stream at the same time by selecting _wiretap_ and adjusting the slider below. To prevent clipping/distortion the outgoing stream's output volume can be adjusted with the top slider. 
