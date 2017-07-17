package application;
	
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;


public class Main extends Application {
	@Override
	public void start(Stage stage) {
		try {
			//app title
			stage.setTitle("ORA-LayerCropper");
			
			//root pane
			BorderPane root = new BorderPane();
			//test
			//listview for layers
			VBox vboxLeft = new VBox();
			vboxLeft.setPadding(new Insets(10));
			ListView<String> listView = new ListView<String>();
			vboxLeft.getChildren().add(listView);
			vboxLeft.prefHeightProperty().bind(stage.heightProperty());
			root.setLeft(vboxLeft);
			
			//image panel
			ImageView imageView = new ImageView();
			//HBox imgHBox = new HBox(imageView);
			root.setCenter(imageView);
			imageView.fitWidthProperty().bind(stage.widthProperty());
			imageView.fitHeightProperty().bind(stage.heightProperty());
			
			//menu bar with file menu
			MenuBar menuBar = new MenuBar();
			//root.getChildren().add(menuBar);
			root.setTop(menuBar);
			
			Menu menuFile = new Menu("File");
			menuBar.getMenus().addAll(menuFile);
			
			//crop and export option
			MenuItem menuCrop = new MenuItem("Crop & Export");
			menuFile.getItems().add(menuCrop);
			menuCrop.setDisable(true);
			
			//open file option in file menu
			MenuItem menuFileOpen = new MenuItem("Open File...");
			menuFile.getItems().add(menuFileOpen);
			menuFileOpen.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
			
			FileChooser fileChooser = new FileChooser();
			menuFileOpen.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					// TODO Auto-generated method stub
					fileChooser.setTitle("Select ORA File");
					fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ORA-File", "*.ora"));
					//just for testing
						//fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
					File oraFile = fileChooser.showOpenDialog(stage);
					
					//obeservable for listView, map for imageView -> image ajustment on selection
					ObservableList<String> layerNames = FXCollections.observableArrayList();
					Map<String, Image> layerMap = new HashMap<String, Image>();
					
					try {
						ZipFile zip = new ZipFile(oraFile);
						Enumeration<? extends ZipEntry> entries = zip.entries();
						while(entries.hasMoreElements())
						{
							ZipEntry entry = (ZipEntry)entries.nextElement();
							//if png and not directory add file to list
							//TODO: fix ugly code with FilenameUtils.getExtension(entry.getName()).equals("png") // requires apache commons
							if(!entry.isDirectory() && entry.getName().endsWith(".png"))
							{
								layerNames.add(entry.getName());
								layerMap.put(entry.getName(), InputStreamToImage(zip.getInputStream(entry)));
								//debug
								System.out.println("found png named: "+ entry.getName());
								
							}
						}
						zip.close();
						
					} catch (ZipException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					listView.setItems(layerNames);
					//change the image when listitem is selected
					listView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
						public void changed(ObservableValue<? extends String> ov, String old_val, String new_val)
						{
							Image img = layerMap.get(new_val);
							imageView.setImage(img); 
						}
					});
					
					menuCrop.setDisable(false);
				}
			});
			
			//event handler for menuCrop
			DirectoryChooser dirChooser = new DirectoryChooser();
			dirChooser.setTitle("Select Save-Folder for Layer Exports");
			menuCrop.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					// TODO Auto-generated method stub
					//TODO: crop all images to remove excess transparent pixels, open folder dialog for save location of layers
					dirChooser.showDialog(stage);
				}
			});
			
			//exit option in file menu, prefixed separator line
			MenuItem menuExit = new MenuItem("Exit");
			menuFile.getItems().addAll(new SeparatorMenuItem(),menuExit);
			menuExit.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					// TODO Auto-generated method stub
					Platform.exit();
				}
			});
			
			
			/*//file button
			Button btn = new Button("test button bot");
			//vboxLeft.getChildren().add(btn);
			root.setBottom(btn);*/
			
			
			
			//create scene, add to stage
			Scene scene = new Scene(root,750,400);
		   /* stage.setMinWidth(400);
		    stage.setMaxWidth(1200);
		    stage.setMinHeight(200);
			stage.setMaxHeight(800); 
		    */
		     
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			stage.setScene(scene);
			stage.show();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private ArrayList<Image> GetImagesFromORAFile(File file)
	{
		ArrayList<Image> imgList = new ArrayList<Image>();
			
		try {
			ZipFile zip = new ZipFile(file);
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while(entries.hasMoreElements())
			{
				ZipEntry entry = (ZipEntry)entries.nextElement();
				//if png and not directory add file to list
				//TODO: fix ugly code with FilenameUtils.getExtension(entry.getName()).equals("png") // requires apache commons
				if(!entry.isDirectory() && entry.getName().endsWith(".png"))
				{
					imgList.add(InputStreamToImage(zip.getInputStream(entry)));
					System.out.println("found png named: "+ entry.getName());
				}
			}
			zip.close();
			
		} catch (ZipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return imgList;
	}
	
	private Image InputStreamToImage(InputStream is)
	{
		Image img = new Image(is);
		return img;
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
