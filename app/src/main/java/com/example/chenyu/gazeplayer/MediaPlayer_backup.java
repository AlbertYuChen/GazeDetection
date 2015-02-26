package com.example.chenyu.gazeplayer;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

public class MediaPlayer_backup extends ActionBarActivity implements SurfaceHolder.Callback, MediaPlayer.OnPreparedListener {

    MediaPlayer AudioPlayer = new MediaPlayer();
    MediaPlayer VideoPlayer;
    MediaPlayer VideoPlayer2;


    SurfaceHolder vidHolder;
    SurfaceView vidSurface;

    String hostIP = "137.110.91.195";
//    String hostIP = "10.0.1.2";

    String Video_L = "rtsp://" + hostIP + ":1935/vod/SamNoSound_L.mp4";

    String Video_H = "rtsp://" + hostIP + ":1935/vod/SamNoSound_H.mp4";

    long SystemStartTime = 0;
    long AccumulativePauseTime = 0;
    long DeltaPauseTime = 0;
    long TimerStartTime = 0;


    long START;
    long END;
    boolean play_pause = true;
    boolean eyes_on_off = true;

    boolean resolution_H_L = true;
    int scenario_no = 1;


    // UI devices
    Button button_pause;
    Button button_eye;
    Button button_timer;
    Button button_switch;
    Button button_camera;

    TextView timerTextView;
    TextView TTView;
    TextView Clock;

    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - TimerStartTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            boolean Control_Schedule[] = Controller.Gaussian_time_line;

            if (eyes_on_off != Control_Schedule[(int) millis / 100]) {
                timerTextView.setText(Control_Schedule[(int) millis / 100] + " " + eyes_on_off + (int) millis / 100);

                if (scenario_no == 1) {
                    setEyes_on_off_scenario_1();
                } else if (scenario_no == 2) {
                    setEyes_on_off_scenario_2();
                }
            }

            TTView.setText(String.format(Control_Schedule[(int) millis / 100]
                    + "/%10d:%02d:%02d", minutes, seconds, millis % 1000));

            int ms = (int) (System.currentTimeMillis() - SystemStartTime);
            Clock.setText(ms / 1000 + ":" + ms % 1000);

            timerHandler.postDelayed(this, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.face_detect_serface_view);

        vidSurface = (SurfaceView) findViewById(R.id.surfView);
        vidHolder = vidSurface.getHolder();
        vidHolder.addCallback(this);

        TTView = (TextView) findViewById(R.id.textView2);
        Clock = (TextView) findViewById(R.id.textView_clock);
        timerTextView = (TextView) findViewById(R.id.textView);

        //Pause and Play Button
        button_pause = (Button) findViewById(R.id.button_pause_play);
        button_pause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (eyes_on_off == true) {
                    // switch from play to pause
                    if (play_pause == true) {
                        VideoPlayer.pause();
                        AudioPlayer.pause();
                        play_pause = false;
                        DeltaPauseTime = System.currentTimeMillis();
                        button_pause.setText("Play");
                    } else {

                        // switch from pause to play
                        VideoPlayer.start();
                        AudioPlayer.start();

                        play_pause = true;
                        DeltaPauseTime = System.currentTimeMillis() - DeltaPauseTime;
                        AccumulativePauseTime += DeltaPauseTime;
                        button_pause.setText("Pause  " + AccumulativePauseTime / 1000);
                    }
                }
            }
        });

        //eyes on off
        button_eye = (Button) findViewById(R.id.button_eye_on_off);
        button_eye.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (scenario_no == 1) {
                    setEyes_on_off_scenario_1();
                } else if (scenario_no == 2) {
                    setEyes_on_off_scenario_2();
                }
            }
        });

        button_timer = (Button) findViewById(R.id.timer_button);
        button_timer.setText("start");
        button_timer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button timer_button = (Button) v;
                if (timer_button.getText().equals("stop")) {
                    timerHandler.removeCallbacks(timerRunnable);
                    timer_button.setText("start");
                } else {
                    TimerStartTime = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnable, 0);
                    timer_button.setText("stop");
                }
            }
        });

    }

    public void switch_video(String URL, int seek_point) {
        VideoPlayer2 = new MediaPlayer();
        try {
            VideoPlayer2.setDataSource(URL);
            VideoPlayer2.prepare();
            VideoPlayer2.seekTo(seek_point);
            VideoPlayer2.setAudioStreamType(AudioManager.STREAM_MUSIC);
            VideoPlayer2.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(2200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        VideoPlayer.release();
        VideoPlayer2.setDisplay(vidHolder);
        VideoPlayer = VideoPlayer2;
//        VideoPlayer.start();
    }

    public void setEyes_on_off_scenario_2() {
        if (play_pause == true) {
            // switch from play to pause
            if (eyes_on_off == true) {
                button_eye.setText("EYES ON");
                timerTextView.setText("" + (int) (System.currentTimeMillis() - (int) SystemStartTime - (int) AccumulativePauseTime));
//                timerTextView.setText("asdfasdf");

                switch_video(Video_L, (int) System.currentTimeMillis() - (int) SystemStartTime - (int) AccumulativePauseTime);
//                switch_video(Video_L, 11110);
                eyes_on_off = false;

            } else {
                // switch from pause to play
                button_eye.setText("EYES OFF");
                timerTextView.setText("" + (int) (System.currentTimeMillis() - (int) SystemStartTime - (int) AccumulativePauseTime));
//                timerTextView.setText("asdfasdf");

                switch_video(Video_H, (int) System.currentTimeMillis() - (int) SystemStartTime - (int) AccumulativePauseTime);
//                switch_video(Video_H, 11110);
                eyes_on_off = true;
            }
        }
    }

    public void setEyes_on_off_scenario_1() {
        if (play_pause == true) {
            // switch from play to pause
            if (eyes_on_off == true) {
                button_eye.setText("EYES ON");
                VideoPlayer.pause();
                eyes_on_off = false;

                timerTextView.setText("");

            } else {
                // switch from pause to play
                button_eye.setText("EYES OFF");

                START = System.currentTimeMillis();
                VideoPlayer.seekTo((int) System.currentTimeMillis() - (int) SystemStartTime - (int) AccumulativePauseTime);
                VideoPlayer.start();
                END = System.currentTimeMillis();

                int ms = (int) (System.currentTimeMillis() - SystemStartTime);
                timerTextView.setText("time of seek:" + (END - START) + "  System time:" + ms / 1000 + ":" + ms % 1000);

                eyes_on_off = true;
            }
        }
    }

    public void PlayAudio() {
        //play audio
        String url = "http://" + hostIP + "/~chenyu/sample.mp3"; // your URL here

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
    public void surfaceCreated(SurfaceHolder arg0) {
        try {
            VideoPlayer = new MediaPlayer();
            VideoPlayer.setDisplay(vidHolder);

            VideoPlayer.setDataSource(Video_H);
            VideoPlayer.prepare();
            VideoPlayer.setOnPreparedListener(this);
            VideoPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

//            VideoPlayer2 = new MediaPlayer();
//            VideoPlayer2.setDisplay(vidHolder);
//
////            VideoPlayer.setDataSource("rtsp://137.110.90.96:1935/vod/Timer.mp4");
//            VideoPlayer2.setDataSource("rtsp://137.110.90.96:1935/vod/Timer.mp4");
//            VideoPlayer2.prepare();
//            VideoPlayer2.setOnPreparedListener(this);
//            VideoPlayer2.setAudioStreamType(AudioManager.STREAM_MUSIC);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
        Button b = (Button) findViewById(R.id.timer_button);
        b.setText("start");
    }


    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        VideoPlayer.start();
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        SystemStartTime = System.currentTimeMillis();

        PlayAudio();
    }

}
