package com.example.chenyu.gazeplayer;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.IOException;

public class MainActivity extends ActionBarActivity {

    public boolean play_pause = true;
    public boolean eyes_on_off = true;

    MediaPlayer AudioPlayer = new MediaPlayer();
    VideoView VideoPlayer = null;
    long starttime = System.currentTimeMillis();
    long AccumulativePauseTime = 0;
    long DeltaPauseTime = 0;

    Button button = null;
    Button button2 = null;


    TextView timerTextView;
    long startTime = 0;

    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            timerTextView.setText(String.format("%d:%02d", minutes, seconds));

            timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.face_detect_serface_view);

        VideoPlayer = (VideoView) findViewById(R.id.myVideo);

        PlayVideo();
        PlayAudio();

        //Pause and Play Button
        button = (Button) findViewById(R.id.button_pause_play);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (eyes_on_off == true) {
                    // switch from play to pause
                    if (play_pause == true) {
                        VideoPlayer.pause();
                        AudioPlayer.pause();
                        play_pause = false;
                        DeltaPauseTime = System.currentTimeMillis();
                        button.setText("Play");
                    } else {
                        // switch from pause to play
                        VideoPlayer.start();
                        AudioPlayer.start();
                        play_pause = true;
                        DeltaPauseTime = System.currentTimeMillis() - DeltaPauseTime;
                        AccumulativePauseTime += DeltaPauseTime;
                        button.setText("Pause  " + AccumulativePauseTime / 1000);
                    }
                }
            }
        });

        //Pause and Play Button
        button2 = (Button) findViewById(R.id.button_eye_on_off);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (play_pause == true) {
                    // switch from play to pause
                    if (eyes_on_off == true) {
                        button2.setText("EYES ON");
                        VideoPlayer.pause();
                        eyes_on_off = false;
                    } else {
                        // switch from pause to play
                        button2.setText("EYES OFF");
                        VideoPlayer.seekTo((int) System.currentTimeMillis() - (int) starttime - (int) AccumulativePauseTime);
                        VideoPlayer.start();
                        eyes_on_off = true;
                    }
                }
            }
        });


        timerTextView = (TextView) findViewById(R.id.textView);
        Button timer_button = (Button) findViewById(R.id.timer_button);
        timer_button.setText("start");
        timer_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Button timer_button = (Button) v;
                if (timer_button.getText().equals("stop")) {
                    timerHandler.removeCallbacks(timerRunnable);
                    timer_button.setText("start");
                } else {
                    startTime = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnable, 0);
                    timer_button.setText("stop");
                }
            }
        });


    }

    public void PlayVideo() {
        MediaController vidControl = new MediaController(this);
        vidControl.setAnchorView(VideoPlayer);
        VideoPlayer.setMediaController(vidControl);

//        VideoPlayer.setVideoPath("http://137.110.92.231/~chenyu/BBC.mp4");
//        VideoPlayer.setVideoPath("rtsp://137.110.92.231:5554/stream.sdp");
//        VideoPlayer.setVideoPath("rtsp://137.110.92.231/~chenyu/mp4:BBC.mp4");
//        VideoPlayer.setVideoPath("rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov");

//        VideoPlayer.setVideoPath("rtsp://137.110.92.231:1935/vod/B5.mp3");
//        VideoPlayer.setVideoPath("rtsp://137.110.92.231:1935/vod/mp3:B5.mp3");
//        VideoPlayer.setVideoPath("rtsp://137.110.92.231:1935/vod/T.mp4");
//        VideoPlayer.setVideoPath("rtsp://10.0.1.2:1935/vod/SamNoSound.mp4");
        VideoPlayer.setVideoPath("rtsp://137.110.92.231:1935/vod/SamNoSound.mp4");
//        VideoPlayer.setVideoPath("rtsp://137.110.92.231:1935/vod/Mnosound.mp4");

        VideoPlayer.start();

    }

    public void PlayAudio() {
        //play audio
//        String url = "http://137.110.92.231/~chenyu/P.mkv"; // your URL here
//        String url = "rtsp://184.72.239.149/vod/mp4:BigBuc[kBunny_175k.mov"; // your URL here
//        String url = "rtsp://137.110.92.231:5554/stream.sdp"; // your URL here
//        String url = "http://137.110.92.231/~chenyu/BBC.mp4"; // your URL here
//        String url = "http://10.0.1.2/~chenyu/sample.mp3"; // your URL here
        String url = "http://137.110.92.231/~chenyu/sample.mp3"; // your URL here
//        String url = "http://137.110.92.231/~chenyu/Ms.mp3"; // your URL here
//        String url = "rtsp://137.110.92.231:1935/vod/B5.mp3"; // your URL here
//        String url = "rtsp://137.110.92.231:1935/vod/mp3:B5.mp3"; // your URL here

        try {
            AudioPlayer.setDataSource(url);
            AudioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            AudioPlayer.prepare();
            AudioPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    @Override
    public void onPause(){
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
        Button b = (Button)findViewById(R.id.timer_button);
        b.setText("start");
    }

}
