package com.example.chenyu.gazeplayer;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FdActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, SurfaceHolder.Callback, MediaPlayer.OnPreparedListener {


    MediaPlayer AudioPlayer = new MediaPlayer();
    MediaPlayer VideoPlayer;
    MediaPlayer VideoPlayer2;
    SurfaceHolder vidHolder;
    SurfaceView vidSurface;
    String hostIP = "137.110.91.195";
//        String hostIP = "10.0.1.2";
//    String Video_L = "rtsp://" + hostIP + ":1935/vod/Timer.mp4";
//    String Video_H = "rtsp://" + hostIP + ":1935/vod/Timer.mp4";

    String Video_L = "rtsp://" + hostIP + ":1935/vod/SamNoSound_L.mp4";
    String Video_H = "rtsp://" + hostIP + ":1935/vod/SamNoSound_H.mp4";
    //    String Video_H = "rtsp://" + hostIP + ":1935/vod/Sample.mp4";
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

    private int frame_index = 0;

    boolean gaze_control = false;
    Point left_eye_position = new Point(0, 0);
    Point right_eye_position = new Point(0, 0);

    Point ground_left_eye_position = new Point(0, 0);
    Point ground_right_eye_position = new Point(0, 0);

    boolean LEFT_EYE = true;
    boolean RIGHT_EYE = false;

    boolean gaze_detection_on_off = true;

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

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final String TAG = "OCVSample::Activity";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    public static final int JAVA_DETECTOR = 0;
    private static final int TM_SQDIFF = 0;
    private static final int TM_SQDIFF_NORMED = 1;
    private static final int TM_CCOEFF = 2;
    private static final int TM_CCOEFF_NORMED = 3;
    private static final int TM_CCORR = 4;
    private static final int TM_CCORR_NORMED = 5;


    private int learn_frames = 0;
    private Mat teplateR;
    private Mat teplateL;
    int method = TM_CCORR_NORMED;

    private Mat mRgba;
    private Mat mGray;
    // matrix for zooming
    private Mat mZoomWindow;
    private Mat mZoomWindow2;

    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private CascadeClassifier mJavaDetectorEye;


    private int mDetectorType = JAVA_DETECTOR;
    private String[] mDetectorName;

    private float mRelativeFaceSize = 0.2f;
    private int mAbsoluteFaceSize = 0;

    private CameraBridgeViewBase mOpenCvCameraView;

    double xCenter = -1;
    double yCenter = -1;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(
                                R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir,
                                "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        // --------------------------------- load left eye
                        // classificator -----------------------------------
                        InputStream iser = getResources().openRawResource(
                                R.raw.haarcascade_lefteye_2splits);
                        File cascadeDirER = getDir("cascadeER",
                                Context.MODE_PRIVATE);
                        File cascadeFileER = new File(cascadeDirER,
                                "haarcascade_eye_right.xml");
                        FileOutputStream oser = new FileOutputStream(cascadeFileER);

                        byte[] bufferER = new byte[4096];
                        int bytesReadER;
                        while ((bytesReadER = iser.read(bufferER)) != -1) {
                            oser.write(bufferER, 0, bytesReadER);
                        }
                        iser.close();
                        oser.close();

                        mJavaDetector = new CascadeClassifier(
                                mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from "
                                    + mCascadeFile.getAbsolutePath());

                        mJavaDetectorEye = new CascadeClassifier(
                                cascadeFileER.getAbsolutePath());
                        if (mJavaDetectorEye.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetectorEye = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from "
                                    + mCascadeFile.getAbsolutePath());

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
                    mOpenCvCameraView.setCameraIndex(1);
                    mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.enableView();

                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public FdActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";

        Log.i(TAG, "Instantiated new " + ((Object) this));
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

/*********************************************************************************************/

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

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
        } catch (Exception e) {
            e.printStackTrace();
        }
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


////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
                mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
        mZoomWindow.release();
        mZoomWindow2.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Core.flip(inputFrame.rgba(), mRgba, 1);
        Core.flip(inputFrame.gray(), mGray, 1);

//        mRgba = inputFrame.rgba();
//        mGray = inputFrame.gray();
        int eyes_area_blue_box_width = 10;
        int eyes_area_blue_box_height = 10;

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        if (mZoomWindow == null || mZoomWindow2 == null)
            CreateAuxiliaryMats();

        MatOfRect faces = new MatOfRect();

        if (mJavaDetector != null)
            mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2,
                    2, // objdetect.CV_HAAR_SCALE_IMAGE
                    new Size(mAbsoluteFaceSize, mAbsoluteFaceSize),
                    new Size());

        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++) {
            Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(),
                    FACE_RECT_COLOR, 1);
            xCenter = (facesArray[i].width) / 2 + facesArray[i].x;
            yCenter = (facesArray[i].height) / 2 + facesArray[i].y;

            Point center = new Point(xCenter, yCenter);

//            Core.circle(mRgba, center, 10, new Scalar(255, 0, 0, 255), 3);
//            Core.putText(mRgba, "[" + center.x + "," + center.y + "]",
//                    new Point(center.x + 20, center.y + 20),
//                    Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255,
//                            255));

            Rect r = facesArray[i];

            eyes_area_blue_box_width = r.width;
            eyes_area_blue_box_height = r.height;

            // compute the eye area
            Rect eyearea = new Rect(r.x + r.width / 8,
                    (int) (r.y + (r.height / 4.5)), r.width - 2 * r.width / 8,
                    (int) (r.height / 3.0));
            // split it
            Rect eyearea_right = new Rect(r.x + r.width / 16,
                    (int) (r.y + (r.height / 4.5)),
                    (r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
            Rect eyearea_left = new Rect(r.x + r.width / 16
                    + (r.width - 2 * r.width / 16) / 2,
                    (int) (r.y + (r.height / 4.5)),
                    (r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
            // draw the area - mGray is working grayscale mat, if you want to
            // see area in rgb preview, change mGray to mRgba
            Core.rectangle(mRgba, eyearea_left.tl(), eyearea_left.br(),
                    new Scalar(0, 0, 205, 255), 1);
            Core.rectangle(mRgba, eyearea_right.tl(), eyearea_right.br(),
                    new Scalar(0, 0, 205, 255), 1);


            if (learn_frames < 5) {
                teplateR = get_template(mJavaDetectorEye, eyearea_right, eyes_area_blue_box_width >> 3);
                teplateL = get_template(mJavaDetectorEye, eyearea_left, eyes_area_blue_box_width >> 3);
                learn_frames++;
            } else if (learn_frames == 5) {
                ground_left_eye_position = left_eye_position;
                ground_right_eye_position = right_eye_position;
                learn_frames++;
            } else {
                // Learning finished, use the new templates for template
                // matching
                match_eye(eyearea_right, teplateR, method, RIGHT_EYE);
                match_eye(eyearea_left, teplateL, method, LEFT_EYE);
            }


        }

        if (gaze_control) {

            int left_delta_x = (int) (left_eye_position.x - ground_left_eye_position.x);
            int left_delta_y = (int) (left_eye_position.y - ground_left_eye_position.y);

            int right_delta_x = (int) (right_eye_position.x - ground_right_eye_position.x);
            int right_delta_y = (int) (right_eye_position.y - ground_right_eye_position.y);

            if (facesArray.length == 0) {
                gaze_detection_on_off = false;
                Core.putText(mRgba, "No face", new Point(100, 400), Core.FONT_ITALIC, 2, new Scalar(255, 0, 255, 255), 3);
            } else if (Math.abs(right_delta_x + left_delta_x) > eyes_area_blue_box_width / 13.5 ||
                    Math.abs(right_delta_y + right_delta_y) > eyes_area_blue_box_width / 10) {
                Core.putText(mRgba, "EYES OFF", new Point(200, 400), Core.FONT_ITALIC, 2, new Scalar(255, 0, 255, 255), 3);
                gaze_detection_on_off = false;
            } else {
                Core.putText(mRgba, "EYES ON", new Point(200, 400), Core.FONT_ITALIC, 2, new Scalar(255, 0, 255, 255), 3);
                gaze_detection_on_off = true;
            }

            Core.putText(mRgba, eyes_area_blue_box_width + "delta_left x:" + left_delta_x + " y:" + left_delta_y,
                    new Point(100, 100), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 0, 255, 255), 2);

            Core.putText(mRgba, eyes_area_blue_box_width + "delta_right x:" + right_delta_x + " y:" + right_delta_y,
                    new Point(100, 150), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 0, 255, 255), 2);

            Core.putText(mRgba, "delta x:" + (right_delta_x + left_delta_x) + " y:" + (right_delta_y + right_delta_y),
                    new Point(100, 200), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 0, 255, 255), 2);
        }

        frame_index++;

        return mRgba;
    }


    private void CreateAuxiliaryMats() {
        if (mGray.empty())
            return;

        int rows = mGray.rows();
        int cols = mGray.cols();

        if (mZoomWindow == null) {
            mZoomWindow = mRgba.submat(rows / 2 + rows / 10, rows, cols / 2
                    + cols / 10, cols);
            mZoomWindow2 = mRgba.submat(0, rows / 2 - rows / 10, cols / 2
                    + cols / 10, cols);
        }

    }

    private void match_eye(Rect area, Mat mTemplate, int type, boolean left_right) {
        Point matchLoc;
        Mat mROI = mGray.submat(area);
        int result_cols = mROI.cols() - mTemplate.cols() + 1;
        int result_rows = mROI.rows() - mTemplate.rows() + 1;
        // Check for bad template size
        if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
            return;
        }
        Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

        switch (type) {
            case TM_SQDIFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_SQDIFF);
                break;
            case TM_SQDIFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_SQDIFF_NORMED);
                break;
            case TM_CCOEFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF);
                break;
            case TM_CCOEFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_CCOEFF_NORMED);
                break;
            case TM_CCORR:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR);
                break;
            case TM_CCORR_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_CCORR_NORMED);
                break;
        }

        Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);
        // there is difference in matching methods - best match is max/min value
        if (type == TM_SQDIFF || type == TM_SQDIFF_NORMED) {
            matchLoc = mmres.minLoc;
        } else {
            matchLoc = mmres.maxLoc;
        }

        Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
        Point matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x,
                matchLoc.y + mTemplate.rows() + area.y);

        if (left_right == LEFT_EYE) {
            left_eye_position = matchLoc;
        } else {
            right_eye_position = matchLoc;
        }


        Core.rectangle(mRgba, matchLoc_tx, matchLoc_ty, new Scalar(255, 255, 0,
                255));


        Rect rec = new Rect(matchLoc_tx, matchLoc_ty);
    }

    private Mat get_template(CascadeClassifier clasificator, Rect area, int size) {
        Mat template = new Mat();
        Mat mROI = mGray.submat(area);
        MatOfRect eyes = new MatOfRect();
        Point iris = new Point();
        Rect eye_template = new Rect();
        clasificator.detectMultiScale(mROI, eyes, 1.15, 2,
                Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                        | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
                new Size());

        Rect[] eyesArray = eyes.toArray();
        for (int i = 0; i < eyesArray.length; ) {
            Rect e = eyesArray[i];
            e.x = area.x + e.x;
            e.y = area.y + e.y;
            Rect eye_only_rectangle = new Rect((int) e.tl().x,
                    (int) (e.tl().y + e.height * 0.4), (int) e.width,
                    (int) (e.height * 0.6));
            mROI = mGray.submat(eye_only_rectangle);
            Mat vyrez = mRgba.submat(eye_only_rectangle);


            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

            Core.circle(vyrez, mmG.minLoc, 5, new Scalar(255, 255, 255, 255), 4);
            iris.x = mmG.minLoc.x + eye_only_rectangle.x;
            iris.y = mmG.minLoc.y + eye_only_rectangle.y;
            eye_template = new Rect((int) iris.x - size / 2, (int) iris.y
                    - size / 2, size, size);
            Core.rectangle(mRgba, eye_template.tl(), eye_template.br(),
                    new Scalar(255, 0, 0, 255), 2);
            template = (mGray.submat(eye_template)).clone();
            return template;
        }
        return template;
    }

    public void onRecreateClick(View v) {
        learn_frames = 0;
    }

    @Override
    public void onBackPressed() {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

}
