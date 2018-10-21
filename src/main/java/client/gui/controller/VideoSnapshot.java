package client.gui.controller;
/**
 * This is a handler for events from the VBox containing a video snapshot
 *
 * Project: Adaptive transcoding of multi-definition multicasted video stream
 * Dependencies: none
 * @author Petrus Kambala
 * Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */
import client.core.ClientMain;
import client.core.Constants;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class VideoSnapshot extends VBox {

    private Label videoName;
    public VideoSnapshot(Image image, String video_name) {
        setPrefSize(50, 50);
        final ImageView imageView = new ImageView(image);
        imageView.setFitHeight(160);
        imageView.setFitWidth(230);
        videoName = new Label(video_name.split("\\.")[0]);
        HBox metadata = new HBox(videoName);
        metadata.setPadding(new Insets(2,0,2,50));
        BackgroundFill fill = new BackgroundFill(Color.valueOf("9AB3B2"),new CornerRadii(3),new Insets(2,2,2,1));
        metadata.setBackground(new Background(fill));

        imageView.setEffect(new DropShadow(5d,2d,+4d, Color.valueOf("#187A77")));

        imageView.setOnMouseClicked((javafx.scene.input.MouseEvent e)-> ClientMain.videoPlayer.
                createStreamSession(videoName.getText(), Constants.stream_type.VOD));

        getChildren().addAll(imageView, metadata);
    }
}
