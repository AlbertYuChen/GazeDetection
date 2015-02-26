package com.example.chenyu.gazeplayer;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.IOException;

/**
 * Created by chenyu on 1/9/15.
 */
public class AVplayer extends Activity {
    /**
     * play Video & Audio
     */
    public boolean play_pause = true;
    public boolean eyes_on_off = true;
    Context context;

    MediaPlayer AudioPlayer = new MediaPlayer();
    VideoView VideoPlayer = null;
    long starttime = System.currentTimeMillis();
    long AccumulativePauseTime = 0;
    long DeltaPauseTime = 0;

    public AVplayer(Context context) {
        this.context = context;
    }

//    public void addAVplayerButton() {
//        //Pause and Play Button
//        final Button button = (Button) findViewById(R.id.button);
//        button.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                if (eyes_on_off == true) {
//                    // switch from play to pause
//                    if (play_pause == true) {
//                        VideoPlayer.pause();
//                        AudioPlayer.pause();
//                        play_pause = false;
//                        DeltaPauseTime = System.currentTimeMillis();
//                        button.setText("Play");
//                    } else {
//                        // switch from pause to play
//                        VideoPlayer.start();
//                        AudioPlayer.start();
//                        play_pause = true;
//                        DeltaPauseTime = System.currentTimeMillis() - DeltaPauseTime;
//                        AccumulativePauseTime += DeltaPauseTime;
//                        button.setText("Pause  " + AccumulativePauseTime / 1000);
//                    }
//                }
//            }
//        });
//
//        //Pause and Play Button
//        final Button button2 = (Button) findViewById(R.id.button2);
//        button2.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                if (play_pause == true) {
//                    // switch from play to pause
//                    if (eyes_on_off == true) {
//                        button2.setText("EYES ON");
//                        VideoPlayer.pause();
//                        eyes_on_off = false;
//                    } else {
//                        // switch from pause to play
//                        button2.setText("EYES OFF");
//                        VideoPlayer.seekTo((int) System.currentTimeMillis() - (int) starttime - (int) AccumulativePauseTime);
//                        VideoPlayer.start();
//                        eyes_on_off = true;
//                    }
//                }
//            }
//        });
//    }

    public void PlayVideo() {
        MediaController vidControl = new MediaController(context);
        vidControl.setAnchorView(VideoPlayer);
        VideoPlayer.setMediaController(vidControl);

//        VideoPlayer.setVideoPath("http://137.110.92.231/~chenyu/BBC.mp4");
//        VideoPlayer.setVideoPath("rtsp://137.110.92.231:5554/stream.sdp");
//        VideoPlayer.setVideoPath("rtsp://137.110.92.231/~chenyu/mp4:BBC.mp4");
//        VideoPlayer.setVideoPath("rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov");

//        VideoPlayer.setVideoPath("rtsp://137.110.92.231:1935/vod/B5.mp3");
//        VideoPlayer.setVideoPath("rtsp://137.110.92.231:1935/vod/mp3:B5.mp3");
//        VideoPlayer.setVideoPath("rtsp://137.110.92.231:1935/vod/T.mp4");
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
}
