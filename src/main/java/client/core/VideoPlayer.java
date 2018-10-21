package client.core;

/**
 * This is class handles all events e.g button press, from the client application GUI,
 * It's responsible for playing the video stream via its inner class VP_Controller
 *There is only one instance of it.
 *
 * Project: Adaptive transcoding of multi-definition multicasted video stream
 * Dependencies: none
 * @author Petrus Kambala
 * Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */
import client.gui.controller.MediaController;
import client.gui.controller.VideoSnapshot;
import client.session.StreamSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

import java.io.IOException;


public class VideoPlayer {
    private MediaPlayer mediaPlayer;
    private Button playButton;
    private Slider volumeSlider, timeSlider;
    private Label playTime, timeLabel;
    private VBox available_video;
    private Pane mediaView_pane;
    private volatile StreamSession streamSession;

    public VideoPlayer(Stage primaryStage) throws IOException {
        FXMLLoader clientApplication = new FXMLLoader(getClass().getResource("/fxml/ClientApplication.fxml"));
        Parent root = clientApplication.load();
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        MediaController mediaController = clientApplication.getController();

        playButton = mediaController.getPlayButton();
        Button livestream_button = mediaController.getLive_stream_button();
        Button cancelButton = mediaController.getCancelButton();

        cancelButton.setOnAction(event -> ClientMain.videoPlayer.getStreamSession().terminateStream());

        volumeSlider = mediaController.getVolumeSlider();
        timeSlider = mediaController.getTimeSlider();

        timeLabel = mediaController.getTimeLabel();
        playTime = mediaController.getPlayTime();

        available_video = mediaController.getAvailable_video();
        mediaView_pane = mediaController.getMediaView_pane();

        playTime.setText("00:00:00");

        primaryStage.show();

        playButton.setOnAction((event -> {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();

            } else if (mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED) {
                mediaPlayer.play();
            }
        }));
        livestream_button.setOnAction(event -> createStreamSession(Constants.LIVE_STREAM, Constants.stream_type.LIVE));

    }

    void addVideoSnapshot(Image image, String videoName) {
        Platform.runLater(() -> available_video.getChildren().add(new VideoSnapshot(image, videoName)));
    }

    public synchronized StreamSession getStreamSession() {
        return streamSession;
    }

    public void createStreamSession(String videoName, Constants.stream_type stream_type) {
        StreamSession.start_tim = System.currentTimeMillis();
        if (streamSession != null) {
            streamSession.terminateStream();
            streamSession = new StreamSession(videoName, stream_type);

        } else {
            streamSession = new StreamSession(videoName, stream_type);
        }
        new Thread(() -> {
            streamSession.initialize();
        }).start();
    }

    /***
     * Update the mediaview instance recursively till the mediaList is empty
     */

    public class VP_Controller {
        public volatile ObservableList<MediaPlayer> mediaList;
        private MediaView mediaView;

        public VP_Controller() {
            mediaList = FXCollections.observableArrayList();
            mediaView = new MediaView();
            mediaView_pane.getChildren().clear();//remove existing children
            mediaView_pane.getChildren().add(mediaView);
        }

        public void updateVideo() {
            while (size() == 0);
            if (mediaPlayer != null) mediaPlayer.dispose();
            if ((mediaPlayer = getMedia()) == null) {
                return;
            }

            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setAutoPlay(true);

            mediaPlayer.currentTimeProperty().addListener((ov) -> {
                streamSession.decreStreamDuration();
                updateTime();
            });
//
            mediaPlayer.setOnEndOfMedia(() -> {
                streamSession.decreStreamDuration();
                updateVideo();
            });
            mediaPlayer.setOnStalled(this::updateVideo);
//
            mediaPlayer.setOnError(() -> {
                System.out.println(mediaPlayer.getError().toString());
                updateVideo();

            });//skip corrupted segment

            mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);
            volumeSlider.valueProperty().addListener((ov) -> {
                if (volumeSlider.isValueChanging())
                    mediaPlayer.setVolume(volumeSlider.getValue() / 100);
            });

            mediaPlayer.setOnPlaying(() -> playButton.setText("Pause"));
            mediaPlayer.setOnPaused(() -> playButton.setText("Play"));
        }

        public synchronized void addMedia(MediaPlayer mediaPlayer) {
            mediaList.add(mediaPlayer);
        }

        synchronized MediaPlayer getMedia() {
            return mediaList.remove(0);
        }

        public synchronized int size() {
            return mediaList.size();
        }

        public synchronized void stopVideoPlayer() {

            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }
            mediaList.clear();
            addMedia(null);//force update to stop
            updateVideo();//force update to stop
        }

        public void updateTime() {
            int duration = (int) streamSession.getStreamDuration();

            int sec = duration % 3600 % 60;
            int min = duration % 3600 / 60;
            int hr = duration / 3600;
            String HH = hr < 10 ? "0" + hr : String.valueOf(hr);
            String mm = min < 10 ? "0" + min : String.valueOf(min);
            String ss = sec < 10 ? "0" + sec : String.valueOf(sec);
            if (duration < 0)
                Platform.runLater(()->playTime.setText("00:00:00"));
            else
            Platform.runLater(() -> playTime.setText(HH + ":" + mm + ":" + ss));
        }
    }
}
