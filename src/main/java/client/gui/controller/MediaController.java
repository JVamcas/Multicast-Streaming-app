package client.gui.controller;
/**
 * This is the fxml controller class for the VBox containing the video snapshots
 *
 * Project: Adaptive transcoding of multi-definition multicasted video stream
 * Dependencies: none
 * @author Petrus Kambala
 * Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;


import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MediaController implements Initializable {



    @FXML
    private Button live_stream_button;

    @FXML
    private Slider volumeSlider;

    @FXML
    private Button cancelButton;

    @FXML
    private VBox available_video;

    @FXML
    private Pane mediaView_pane;

    @FXML
    private Label playTime;

    @FXML
    private MediaView mediaView;

    @FXML
    private Label timeLabel;

    @FXML
    private Slider timeSlider;

    @FXML
    private Button playButton;
    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }


    @FXML
    private void playButtonHandler(ActionEvent event) throws IOException {

    }

    public Button getLive_stream_button() {
        return live_stream_button;
    }

    public Slider getVolumeSlider() {
        return volumeSlider;
    }

    public Button getCancelButton() {
        return cancelButton;
    }

    public VBox getAvailable_video() {
        return available_video;
    }

    public Pane getMediaView_pane() {
        return mediaView_pane;
    }

    public Label getPlayTime() {
        return playTime;
    }

    public MediaView getMediaView() {
        return mediaView;
    }

    public Label getTimeLabel() {
        return timeLabel;
    }

    public Slider getTimeSlider() {
        return timeSlider;
    }

    public Button getPlayButton() {
        return playButton;
    }
}

