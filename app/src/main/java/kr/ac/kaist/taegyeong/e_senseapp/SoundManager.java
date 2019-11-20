package kr.ac.kaist.taegyeong.e_senseapp;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.util.Log;

public class SoundManager {
    private final String TAG = "SqueakyManager";

    private final int SOUND_SAMPLE_RATE = 8000;
    private final double SOUND_FREQ = 500;

    private final int duration = 1; // seconds
    private final int sampleRate = 8000;
    private final int numSamples = sampleRate*duration;
    private final double sample[] = new double[numSamples];
    private final byte generatedSnd[] = new byte[2 * numSamples];

    private AudioTrack audioTrack;


    public SoundManager () {
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SOUND_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                AudioTrack.MODE_STATIC);
        generateSound(SOUND_FREQ);
    }

    public void generateSound(double freqOfTone){
        Log.d(TAG,"freq: "+freqOfTone);

        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
        }
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.setLoopPoints(0, generatedSnd.length/2, -1);
        audioTrack.setVolume(10);
    }

    public void play(int duration){
        if (sound)
            return;
        audioTrack.play();
        (new SoundGenTask()).execute(duration);
    }

    public void play(){
        audioTrack.play();
    }

    public void pause() {
        audioTrack.pause();
    }

    public void release() {
        audioTrack.release();
        audioTrack = null;
    }

    public boolean sound = false;

    public class SoundGenTask extends AsyncTask<Integer, Void, Void> {
        @Override
        public Void doInBackground(Integer... params) {
            sound = true;
            try {
                Thread.sleep(params[0]);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            audioTrack.pause();
            sound = false;
            return null;
        }

        @Override
        public void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }
}
