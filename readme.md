Project for showing android bug 158646286
=========================================

On a Google Pixel 3a device running Android 10 (Build number QQ2A.200501.001.B2),
a running accessibility service interferes with `android.media.AudioRecord`.

As long as the a11y service is running, `android.media.AudioRecord#read` will fill the buffer given to it with all zeroes,
instead of correct audio data, as if the recording volume was muted.

When the a11y service is disabled (or never enabled in the first place), the app properly records audio.


Reproducible on
---------------

The issue was successfully reproduced on:
* Google Pixel 3a; Android 10 (Build number QQ2A.200501.001.B2)

The issue is *not* reproducible on:
* Google Pixel; Android 9 (Build number PQ3A.190801.002)


Buggy behavior
--------------

When an accessibility service is enabled, `android.media.AudioRecord#read` only reads null bytes instead of an audible audio signal.

This buggy behavior is only present when the recording activity and the accessibility service belong to the same package.

Whether the activity and the service are run in a separate process does not influence the result.


Expected behavior
-----------------

`android.media.AudioRecord` can record audio, regardless of whether an accessibility service is enabled.


Steps to reproduce
------------------

1. Build and install the app in this project
2. Grant audio recording and file writing permissions (the app does not ask for them if they are missing!).
3. Open the app and start recording audio by tapping the `Start recording` button.
4. Say something or make some noise for the microphone to pick up.
5. Tap the `Stop recording` button. A toast should inform you that the recording went well.
6. Go to the Music/Debugging directory. A file with raw audio data is added here.
   Upon inspection, this file shows a proper 16-bit, little endian mono audio stream.
7. Enable the accompanying accessibility service in the Settings app.
8. Repeat steps 3 through 5.
9. Check out the new audio file stored in the Music/Debugging directory. This file contains only null bytes.
10. Disable the accessibility service again.
11. Repeat steps 3 through 5.
12. The newest audio file stored in the Music/Debugging directory again contains a proper audio stream.

For details, see [https://issuetracker.google.com/issues/158646286](https://issuetracker.google.com/issues/158646286).