# CAS2Audio
Play back tape images for 8-bit Atari computers on mobile devices with Android.

## Overview

* CAS2Audio allows you to play back Atari 8-bit tape images (.cas) files on small portable devices (phones, tablets) running Android. Full description of the tape image format is available at http://a8cas.sourceforge.net/format-cas.html
* CAS2Audio is a satellite project of TURGEN SYSTEM - http://turgen.sourceforge.net/

### Highlights

* The signal is generated on-the-fly (no temporary wave files)
* Standard records (FUJI,baud,data,fsk)
* Turbo records (pwms,pwmc,pwmd,pwml)
* 44100 Hz and 48000 Hz sampling rates
* Mono or Stereo output
* For turbo records, you can choose waveform - sine wave or square wave
* Adjustable amplitude
* Convenient tape image selection

## Technical information

### OS Version
At least Android 4.4 is required, Android 6.0 is recommended.

### Installation

* Download the .apk package from the "Releases" section
* Ensure that the application you will use to open the .apk package has permission to install unknown apps. To do so, go to Settings/Apps/Advanced and enabled the "Install unknown apps" options.
* Open and install the .apk package
* You might need to uninstall the previous version first

### Permissions

* Read only access to storage
* Wake lock (to prevent the device from sleeping when playing back)

## Screenshot
![Screenshot](c2a_shot1.png)

## Acknowledgements
* This application uses [android-file-chooser](https://github.com/hedzr/android-file-chooser)


