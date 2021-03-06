/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.da.twilight.leyla.ui.fxml;



import com.da.twilight.leyla.components.FileWatcher;
import com.da.twilight.leyla.components.Finder;
import com.da.twilight.leyla.components.TreeNode;
import static com.da.twilight.leyla.components.TreeNode.createDirTree;
import static com.da.twilight.leyla.components.TreeNode.renderDirectoryTree;
import com.da.twilight.leyla.ui.Loggable;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * FXML Controller class
 *
 * @author ShadowWalker
 */
public class FileFXMLController implements Initializable, Loggable {

    private final Logger LOGGER = LoggerFactory.getLogger( FileFXMLController.class );
    
    @FXML
    private Button searchBtn;
    @FXML
    private Button watchBtn;
    @FXML
    private Button stopWatchBtn;
    @FXML
    private Button printTreeBtn;
    @FXML
    private TextArea searchTxtarea;
    @FXML
    private TextArea errTxtarea;
    @FXML
    private TextField searchTxt;
    
    @FXML
    private Button dirChooserBtn;
    
    @FXML
    private TextField searchLocationTxt;
    
    private Properties prop;
    
    private Thread watcherTask;
    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        try {
            prop = new Properties();
            prop.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
        } catch (IOException ex) {
            errTxtarea.appendText("Can't load application properties file!");
            errTxtarea.appendText("Error: " + ex.toString());
        }
        
        searchTxt.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>(){
            @Override
            public void handle(KeyEvent event) {
                if(event.getCode() == KeyCode.ENTER){
                    searchByString( searchTxt.getText().trim() );
                }
            }
        });
        
        //String workingDir = prop.getProperty("working.path");
        // SEARCHING THE STRING
        searchBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                searchByString( searchTxt.getText().trim() );
            }
        });
        
        // WATCHING SERVICE BUTTON for logging file change on console
        watchBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                FileFXMLController.this.watchBtn.setDisable(true);
                FileWatcher fw = new FileWatcher();
                fw.setFolder( getCurrentDir() );
                fw.setLogger( FileFXMLController.this );
                
                watcherTask = new Thread(fw);
                watcherTask.setDaemon(true);   // this line for deamon app. when app's closed. this task running on another thread close too 
                watcherTask.start();
            }
        });
        // STOP WATCHING SERVICE BUTTON
        stopWatchBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                watchBtn.setDisable(false);
                if(watcherTask != null){
                    watcherTask.interrupt();
                }
            }
        });
        
        // select line of text area when click
        searchTxtarea.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    
                    // check, if click was inside the content area
                    Node n = event.getPickResult().getIntersectedNode();
                    while (n != searchTxtarea) {
                        if (n.getStyleClass().contains("content")) {
                            // find previous/next line break
                            int caretPosition = searchTxtarea.getCaretPosition();
                            String text = searchTxtarea.getText();
                            int lineBreak1 = text.lastIndexOf('\n', caretPosition - 1);
                            int lineBreak2 = text.indexOf('\n', caretPosition);
                            if (lineBreak2 < 0) {
                                // if no more line breaks are found, select to end of text
                                lineBreak2 = text.length();
                            }

                            searchTxtarea.selectRange(lineBreak1, lineBreak2);
                            
                            // only open file | directory when double click
                            if(event.getClickCount() > 1 ){
                                String selectedText = searchTxtarea.getSelectedText().trim();
                                File file = new File( selectedText );

                                // open media file for mp4 because JFX only support mp4 container
                                if( file.isFile()){
                                    LOGGER.info("Opening file: {}", file.getAbsolutePath());
                                    /*if(selectedText.contains(".mp4")){
                                        Stage stage = new Stage();

                                        Player player = new Player( file.toURI().toString());

                                        // Adding player to the Scene 
                                        Scene scene = new Scene(player, 1920/2, 1080/2, Color.BLACK);
                                        stage.setScene(scene);  
                                        stage.setTitle("Playing video");  
                                        stage.show(); 

                                        stage.setOnCloseRequest(evt -> {
                                            player.pause();
                                            LOGGER.debug("Player is closing");
                                        }); 
                                    } else {} */
                                    
                                    // open file with default application
                                    Desktop desktop = Desktop.getDesktop();
                                    try{
                                        desktop.open( file );
                                    }catch(IOException ioe){
                                        Alert alert = new Alert(Alert.AlertType.ERROR);
                                        alert.setTitle("Error while open file ");
                                        alert.setHeaderText("File open error");
                                        alert.setContentText("File does not support for opening!");
                                        alert.show(); 
                                    }
                                    
                                } else if( file.isDirectory() ) {
                                    LOGGER.info("Opening directory: {}", file.getAbsolutePath());
                                    try {
                                        Desktop.getDesktop().open( file );
                                    } catch (IOException ex) {
                                        LOGGER.error("Directory does not exist! err=", ex.toString());

                                        Alert alert = new Alert(Alert.AlertType.ERROR);
                                        alert.setTitle("Error while open directory ");
                                        alert.setHeaderText("Directory open error");
                                        alert.setContentText("Directory does not exist! ");
                                    }
                                }
                            }

                            event.consume();
                            break;
                        }
                        n = n.getParent();
                    }
                }
            }
        });
        
        printTreeBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent event) {
                File file = new File( getCurrentDir() );
                TreeNode<File> DirTree = createDirTree(file);
                String result = renderDirectoryTree(DirTree);
                log(result);
                /*
                try {
                    BufferedWriter bw = Files.newBufferedWriter( Paths.get("D:\\tree.txt"), StandardCharsets.UTF_16, StandardOpenOption.WRITE);
                    bw.write(result);
                } catch (IOException iox) {
                    System.out.println("[FAILED] Error: " + iox.toString());
                } 
                */
            }
        });
        
        searchLocationTxt.setText(new File(".").getAbsolutePath());
        dirChooserBtn.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                Stage curStage = ( Stage ) dirChooserBtn.getScene().getWindow();
                DirectoryChooser dc = new DirectoryChooser(); 
                dc.setInitialDirectory(new File("."));
                File selectedDirectory = dc.showDialog(curStage);
                searchLocationTxt.setText( selectedDirectory.getAbsolutePath() );
            }
        });
        
        /* Roll-back to old value when value change to new value */
        /*searchLocationCb.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal)->{
            if(newVal != null && !newVal.equals("Two")){
                Platform.runLater(() -> searchLocationCb.setValue(oldVal));
            }
        }); */
        
        // auto focus TextField when application start
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                searchTxt.requestFocus();
            }
        });
    }
    
    private void searchByString(String searchString){
        
        // Searching main task
        final Thread mainTask = new Thread(new Runnable() {
            @Override
            public void run() {
                searchTxtarea.clear();
        
                Finder finder = new Finder("*" + searchString + "*");
                finder.setLogger(FileFXMLController.this);
                try {
                    Files.walkFileTree( Paths.get( getCurrentDir() ), finder);
                } catch (IOException ex) {
                    LOGGER.error("Error while walking tree err={}",ex.toString());
                }
                finder.done();
            }
        });
        mainTask.start();
        
        // Searching indicator task.
        Thread indicatorTask = new Thread(() -> {
            while( mainTask.isAlive() ){
                Platform.runLater(() -> {
                    searchBtn.setDisable(true);
                });
                
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException ex) {}
            }
            Platform.runLater(() -> {
                searchBtn.setDisable(false);
            });
        });
        indicatorTask.start();
    }
    
    public String getCurrentDir(){
        return searchLocationTxt.getText();
    }
    
    @Override
    public void log(String msg) {
        Platform.runLater(() -> {
            searchTxtarea.appendText(msg + "\n");
        });
    }

    @Override
    public void err(String msg) {
        Platform.runLater(() -> {
            errTxtarea.appendText(msg + "\n");
        });
    }
}
