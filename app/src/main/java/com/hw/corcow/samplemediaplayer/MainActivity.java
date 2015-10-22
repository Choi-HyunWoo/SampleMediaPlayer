package com.hw.corcow.samplemediaplayer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    MediaPlayer mPlayer;                    // 음악 재생할 MediaPlayer
    enum PlayState {                        // MediaPlayer 상태 정의
        IDLE,
        INITIALIZED,
        PREPARED,
        STARTED,
        PAUSED,
        STOPPED,
        ERROR,
        RELEASED
    }
    PlayState mState = PlayState.IDLE;      // MediaPlayer 상태 변수

    // Seekbars
    SeekBar progressView;                   // 재생 시간 seekbar
    SeekBar volumeView;                     // 볼륨 조정 seekbar

    /* Volume */
    AudioManager mAM;                       // System volume을 조정할 AudioManager
    CheckBox muteView;

    float volume = 1.0f;
    Runnable volumeUp = new Runnable() {
        @Override
        public void run() {
            if (volume < 1.0f) {
                mPlayer.setVolume(volume, volume);
                volume+=0.1f;
                mHandler.postDelayed(this, CHECK_INTERVAL);
            } else {
                volume = 1.0f;
                mPlayer.setVolume(volume,volume);
            }
        }
    };
    Runnable volumeDown = new Runnable() {
        @Override
        public void run() {
            if (volume > 0) {
                mPlayer.setVolume(volume, volume);
                volume -= 0.1f;
                mHandler.postDelayed(this, CHECK_INTERVAL);
            } else {
                volume = 0;
                mPlayer.setVolume(volume,volume);
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mPlayer = MediaPlayer.create(this, R.raw.better_than_that);
        mState = PlayState.PREPARED;

        Button btn = (Button)findViewById(R.id.btn_play);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });

        btn = (Button)findViewById(R.id.btn_pause);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause();
            }
        });

        btn = (Button)findViewById(R.id.btn_stop);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
            }
        });

        mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mState = PlayState.ERROR;
                return false;
            }
        });

        /* Progress Seekbar setting */
        progressView = (SeekBar) findViewById(R.id.seekProgress);
        progressView.setMax(mPlayer.getDuration());
        progressView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    this.progress = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                progress = -1;
                isSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (progress != -1) {
                    if (mState == PlayState.STARTED) {
                        mPlayer.seekTo(progress);
                    }
                }
                isSeeking = false;
            }
        });

        /* Mute Setting */
        muteView = (CheckBox) findViewById(R.id.check_mute);
        muteView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mHandler.removeCallbacks(volumeUp);
                    mHandler.post(volumeDown);
                } else {
                    mHandler.removeCallbacks(volumeDown);
                    mHandler.post(volumeUp);
                }
            }
        });

        /* Volume Setting */
        mAM = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        volumeView = (SeekBar) findViewById(R.id.seekVolume);
        int max = mAM.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volumeView.setMax(max);
        int current = mAM.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeView.setProgress(current);

        volumeView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mAM.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    /* Progress Seekbar Handler */
    boolean isSeeking = false;
    Handler mHandler = new Handler(Looper.getMainLooper());

    private static final int CHECK_INTERVAL = 100;

    Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mState == PlayState.STARTED) {
                if (!isSeeking) {
                    int position = mPlayer.getCurrentPosition();
                    progressView.setProgress(position);
                }
                mHandler.postDelayed(this, CHECK_INTERVAL);
            }
        }
    };

    /* MediaPlayer Play */
    private void play() {
        // go to PREPARED
        if (mState == PlayState.INITIALIZED || mState == PlayState.STOPPED) {
            try {
                mPlayer.prepare();
                mState = PlayState.PREPARED;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // and go to STARTED
        if (mState == PlayState.PREPARED || mState == PlayState.PAUSED) {
            mPlayer.seekTo(progressView.getProgress());
            mPlayer.start();
            mState = PlayState.STARTED;
            mHandler.post(progressRunnable);
        }
    }
    private void pause() {
        if (mState == PlayState.STARTED) {
            mPlayer.pause();
            mState = PlayState.PAUSED;
        }
    }
    private void stop() {
        if (mState == PlayState.STARTED || mState == PlayState.PREPARED || mState == PlayState.PAUSED) {
            mPlayer.stop();
            mState = PlayState.PAUSED.STOPPED;
            progressView.setProgress(0);
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        pause();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayer != null) {
            mPlayer.release();
            mState = PlayState.RELEASED;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
