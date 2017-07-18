package application;
	
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
//import org.apache.commons.io.FilenameUtils;


public class Main extends Application {
	
	//layername, layer (k,v)
	private Map<String, Image> layerMap;
	
	@Override
	public void start(Stage stage) {
		try {
			//app title
			stage.setTitle("ORA-LayerCropper");
			
			//root pane
			BorderPane root = new BorderPane();

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
			
			//Open ORA-File
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
					
					//observable for listView, map for imageView -> image ajustment on selection
					ObservableList<String> layerNames = FXCollections.observableArrayList();
					layerMap = new HashMap<String, Image>();
					
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
			
			//Crop and Export Layers
			DirectoryChooser dirChooser = new DirectoryChooser();
			dirChooser.setTitle("Select Save-Folder for Layer Exports");
			menuCrop.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					// TODO Auto-generated method stub
					
					//select save folder for new cropped images
					File selectedDir = dirChooser.showDialog(stage);
					
					int i=0;
					//crop all images to remove excess transparent pixels
					for(Entry<String, Image> entry : layerMap.entrySet())
					{
						Image img = entry.getValue();
						String imgName = entry.getKey();
						
						//get cropped image
						WritableImage wImg = CropImage(img);
						//save images to selected folder
						BufferedImage bImg = SwingFXUtils.fromFXImage(wImg, null);
						try {
							
							String filename = selectedDir.getAbsolutePath()+"\\layer"+i+".png"; i++;
							System.out.println(filename);
							ImageIO.write(bImg, "png", new File(filename));
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
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
			
			
			/*//test button
			Button btn = new Button("test button bot");
			//vboxLeft.getChildren().add(btn);
			root.setLeft(btn);
			btn.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					// TODO Auto-generated method stub
					//test writableimage to file
					WritableImage wimg = new WritableImage(200, 200);
					//wimg.getPixelWriter().setPixels(0, 0, 199, 199, Pixel, arg5, arg6);
					BufferedImage bImg = SwingFXUtils.fromFXImage(wimg, null);
					try {
						ImageIO.write(bImg, "png", new File("testImg.png"));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});*/
			
			//create scene, add to stage
			Scene scene = new Scene(root,750,400);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			stage.setScene(scene);
			stage.show();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private Image InputStreamToImage(InputStream is)
	{
		Image img = new Image(is);
		return img;
	}
	
	private WritableImage CropImage(Image img)
	{
		PixelReader pixelReader = img.getPixelReader();
		//cropping values once adjusted, start comparing values to middle
		int maxX = 0; int minX = (int) img.getWidth(); //problem with initial cropping settings?
		int maxY = 0; int minY = (int) img.getHeight(); 
		System.out.println("minY Start = "+ minY+ "minX start = " + minX);
		double transparencyTolerance = 0.05;
		
		//read every row
		for(int y = 0; y < img.getHeight(); y++)
		{
			for(int x = 0; x < img.getWidth(); x++)
			{
				//if not transparent, get new boudries for visible content
				if(pixelReader.getColor(x, y).getOpacity() > transparencyTolerance)
				{
					minY = Math.min(minY, y);
					maxY = Math.max(maxY, y);
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
				}
			}
		}
		int newWidth = maxX-minX;
		int newHeight = maxY-minY;
		System.out.println("Dimensions are: (x,y,maxX,maxY) = "+ minX+", "+ minY+", maxX: "+ maxX + ", maxY: " + maxY );
		System.out.println("Dimensions are: (x,y,width,height) = "+ minX+", "+ minY+", "+ newWidth + ", " + newHeight );
		
		WritableImage newImg = new WritableImage(10, 10);
		try {
			//new cropped image
			newImg = new WritableImage(pixelReader, minX, minY, newWidth, newHeight);
		}catch (ArrayIndexOutOfBoundsException ex){
			ex.printStackTrace();
		}catch (IllegalArgumentException ex){
			ex.printStackTrace();
		}
		
		return newImg;
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
