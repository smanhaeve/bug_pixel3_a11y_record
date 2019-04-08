package com.manhaeve.bug.a11yaudiorecord;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RecordActivity";

    private Button mBtnAction;
    private AudioRecorder mRecorder;

    private static final int SAMPLE_RATE = 48000;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int SOURCE = MediaRecorder.AudioSource.DEFAULT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mBtnAction = findViewById(R.id.audio_btnRecord);
        mBtnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecorder != null)
                    MainActivity.this.stopRecord();
                else
                    MainActivity.this.startRecord();
            }
        });
    }

    @Override
    protected void onPause() {
        stopRecord();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    void startRecord() {
        if (mRecorder != null)
            return;
        mBtnAction.setEnabled(false);
        AudioRecorder recorder = new AudioRecorder();
        if (recorder.startRecord()) {
            mRecorder = recorder;
            mBtnAction.setText(R.string.audio_record_stop);
            mBtnAction.setEnabled(true);
        }
    }

    void stopRecord() {
        if (mRecorder == null)
            return;
        mBtnAction.setEnabled(false);
        mRecorder.stopRecord();
        mRecorder = null;
        mBtnAction.setText(R.string.audio_record_start);
        mBtnAction.setEnabled(true);
    }

    private File getOutputFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), "Debugging");

        if (! mediaStorageDir.exists()) {
            if (! mediaStorageDir.mkdirs()) {
                Log.e(TAG, "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return new File(mediaStorageDir, "AUDIO_" + timeStamp + ".raw");
    }

    private class AudioRecorder {

        private Thread mThread;
        private RecorderThread mRecorder;

        boolean startRecord() {
            try {
                mRecorder = new RecorderThread();
                mThread = new Thread(mRecorder);
                mThread.start();
                return true;
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Failed to open output file", e);
                Toast.makeText(MainActivity.this, "Failed to open output file.", Toast.LENGTH_LONG).show();
                return false;
            } catch (UnsupportedOperationException e) {
                Log.e(TAG, "Failed to configure recorder", e);
                Toast.makeText(MainActivity.this, "Failed to configure recorder.", Toast.LENGTH_LONG).show();
                return false;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid recorder configuration", e);
                Toast.makeText(MainActivity.this, "Invalid recorder configuration.", Toast.LENGTH_LONG).show();
                return false;
            }
        }

        void stopRecord() {
            mRecorder.stop();
            try {
                mThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "Failed to join recorder thread: ", e);
            }
            mRecorder = null;
            mThread = null;
        }
    }


    private class RecorderThread implements Runnable {
        AudioRecord mAudioRecord;
        private boolean stop = false;
        FileOutputStream file;
        final int mBufferSize;

        public RecorderThread() throws FileNotFoundException {
            mBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING);
            Log.d(TAG, "Creating AudioRecorder with:");
            Log.d(TAG, "  Channel mask: 0x" + Integer.toHexString(CHANNEL));
            Log.d(TAG, "  Encoding: 0x" + Integer.toHexString(ENCODING));
            Log.d(TAG, "  Source: 0x" + Integer.toHexString(SOURCE));
            Log.d(TAG, "  Buffer size: " + mBufferSize + "B");
            if (mBufferSize < 0) {
                throw new IllegalArgumentException();
            }
            mAudioRecord = new AudioRecord.Builder().setAudioFormat(
                    new AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(CHANNEL)
                            .setEncoding(ENCODING)
                            .build()
            ).setAudioSource(SOURCE)
                    .setBufferSizeInBytes(mBufferSize)
                    .build();
            File f = getOutputFile();
            try {
                file = new FileOutputStream(f);
                Log.d(TAG, "Writing audio data to " + f);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Failed to open file " + (f == null ? "<null>" : f.getAbsolutePath()), e);
                throw e;
            }
        }

        public synchronized void stop() {
            stop = true;
        }

        @Override
        public void run() {
            byte[] bytes = new byte[mBufferSize];
            mAudioRecord.startRecording();
            while(true) {
                synchronized(this) {
                    if (stop)
                        break;
                }
                // save the next sample
                int read = mAudioRecord.read(bytes, 0, mBufferSize);
                if (read > 0) {
                    try {
                        file.write(bytes, 0, read);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to write data", e);
                    }
                }
            }
            mAudioRecord.stop();
            mAudioRecord.release();
            try {
                file.close();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Success!", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Failed to close output file", e);
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Failed to close output file.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }
}
