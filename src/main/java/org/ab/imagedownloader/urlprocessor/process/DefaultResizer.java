package org.ab.imagedownloader.urlprocessor.process;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.imageio.ImageIO;

import org.ab.imagedownloader.IDConfiguration;
import org.ab.imagedownloader.obj.ImgRequest;
import org.ab.imagedownloader.obj.ImgRequest.Status;
import org.ab.imagedownloader.obj.ProcessedImg;
import org.ab.imagedownloader.utils.ImageResizer;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
/**
 * using input request, create resized versions based on state info in request object
 * */
@ParametersAreNonnullByDefault
@Service
public class DefaultResizer implements Resizer {

	private static final Logger LOGGER = Logger.getLogger(DefaultResizer.class);	
	
	@Autowired
	private IDConfiguration iDConfiguration;
	
	@Autowired
	private ImageResizer imageResizer;
	
	@Override
	public ImgRequest process(ImgRequest request) {		
		try{
			switch(request.getStatus()){
				case ERROR:
					LOGGER.error(String.format("There was an error downloading img src [%s], nothing to resize", request.getUrl()));
					return request;
					
				case OK_FILE_NOT_CHANGED:
					LOGGER.info(String.format("contents of url [%s] has not changed, verifying resized versions are correct sizes", request.getUrl()));
					verifyInputs(request);
					return verifyResizedVersions(request);
					
				case OK_NEW_FILE_VERSION:
					LOGGER.info(String.format("contents of url [%s] has changed, purging old resized versions and creating new ones", request.getUrl()));
					//deliberate follow through					
				case OK:
					LOGGER.info(String.format("creating resized versions for [%s]", request.getUrl()));
					verifyInputs(request);
					deleteExistingResizedImages(request);	
					return resizeForFile(request, getAllRequiredFormats());
					
				default:
					LOGGER.error(String.format("Unknown status code %s, for img src [%s], nothing to resize", request.getStatus(), request.getUrl()));
					return request;
				
			}
		}catch(Exception e){			
			LOGGER.error(String.format("error resizing image [%s], error: %s", request.getUrl(), e.getMessage()));
			request.setStatus(Status.ERROR);
			return request;
		}
				
	}
	private void verifyInputs(ImgRequest request) throws InvalidInputsException {
		if(!request.getProcessedImg().isPresent() || !request.getProcessedImg().get().getImgFile().isPresent()){			
			throw new InvalidInputsException(String.format("No file reference to process ! [%s, %s]", request.getStatus(), request.getUrl())); 			
		}
	}
	private void deleteExistingResizedImages(ImgRequest request) throws IOException {
		
		ProcessedImg processedImage = request.getProcessedImg().get();
		Path filePath = processedImage.getImgFile().get();
		
		Path resizedFolder = Paths.get(processedImage.generateResizedFolderName(filePath.getParent().toString()));
		if(!resizedFolder.toFile().exists()){
			return;
		}
		
		try (final DirectoryStream<Path> stream = Files.newDirectoryStream(resizedFolder)) {
			for(Path path:stream){
				Files.deleteIfExists(path);
			}										  
		}				
	}
	
	/*
	 * create resized files from source image 
	 * */
	private ImgRequest resizeForFile(ImgRequest request, Map<Integer, Set<String>> sizesAndFormats) throws Exception{
		
		if(sizesAndFormats.isEmpty()){
			return request;
		}
		
		ProcessedImg processedImage = request.getProcessedImg().get();
		Path filePath = processedImage.getImgFile().get();
		
		File srcImage = filePath.toFile();
		BufferedImage bimg = ImageIO.read(srcImage);
		if(bimg == null){
			throw new IOException(String.format("Error reading downloaded file from disk ! [%s, %s]", filePath.toString(), request.getUrl())); 			
		}
		
		if( bimg.getHeight() <= iDConfiguration.getImageResizeMinHeight() || bimg.getWidth() <= iDConfiguration.getImageResizeMinWidth() ){
			//don't need to proceed
			return request;
		}		
		
		Path resizedFolder = Paths.get(processedImage.generateResizedFolderName(filePath.getParent().toString()));
		verifyResizeFolderExists(resizedFolder);
		
		/*note: resize widths are sorted into descending order*/		
		Set<Integer> keys = sizesAndFormats.keySet();
		List<Integer> sizes = new ArrayList<>(keys.size());
		sizes.addAll(keys);
		sizes.sort((a,b)->b.compareTo(a));
					
		Optional<BufferedImage> resized = Optional.of(bimg);
		BufferedImage previousResized = null;
		for(Integer width:sizes){
				
				if(!sizesAndFormats.containsKey(width)){
					continue;
				}
			
				if(resized.get().getWidth() != width){
					/*
					 * note: for each resize call, I am using the previously resized version of the image. This is an attempt at
					 * efficiency - I am assuming that for the most case:
					 *  - the original image will be bigger than the required resize widths
					 *  - resizing a small image to a smaller one is cheaper than resizing the original image to a smaller one
					 *  - at the widths this tool is using, resizing an image over and over (rather than using the original image) will not affect end quality *too* much
					 * These are assumptions, and may be invalid !
					 * */
					resized = imageResizer.resize(resized.get(), width);
				}
								
				if(!resized.isPresent()){
					LOGGER.error(String.format("Unexpected Error in resizing image [%s, %d]", filePath.toFile(), width));
					//do nothing otherwise
					continue;					
				}
				
				for(String format:sizesAndFormats.get(width)){
					writeToFile(resized.get(), format, ProcessedImg.generateResizedFullFileName(resizedFolder.toString(), width, format));											
				}
				flushBuffer(previousResized);					
				previousResized = resized.get();						
		}		
				
		return request;
	}

	private void flushBuffer(@Nullable BufferedImage img){
		if(img != null){
			/* makes it easier for GC to collect this soon to be unreferenced memory			 
			 * note: this is overkill for this test tool, but if we were resizing a lot of larger sizes & formats in this class,
			 * we will probably trigger a GC at some point while still executing the main loop*/
			img.flush();
		}
	}
	
	private void writeToFile(BufferedImage image, String format, String destination) throws IOException {
		try (FileOutputStream file = new FileOutputStream(destination)) {
			ImageIO.write(image, format, file);
		}
	}
	
	/*
	 * verify existing resized files match what is in the settings - are there new 
	 * formats, sizes added to the config since last run?
	 * remove invalid formats, create missing formats
	 * */
	private ImgRequest verifyResizedVersions(ImgRequest request) throws IOException, Exception {
		
		ProcessedImg processedImage = request.getProcessedImg().get();
		Path filePath = processedImage.getImgFile().get();
		
		Path resizedFolder = Paths.get(processedImage.generateResizedFolderName(filePath.getParent().toString()));
		if(!resizedFolder.toFile().exists()){
			//no existing resizes - redo from start
			return resizeForFile(request, getAllRequiredFormats());
		}
		
		Map<String, Path> existingFiles = new HashMap<>();
		Map<Integer, Set<String>> missingFormats = new HashMap<>(); 
		try (final DirectoryStream<Path> stream = Files.newDirectoryStream(resizedFolder, path -> !Files.isDirectory(path))) {
			stream.forEach(path->existingFiles.put(path.getFileName().toString(), path));								  
		}
		
		List<Integer> resizeWidths = iDConfiguration.getImageResizeWidths();
		
		for(Integer width:resizeWidths){
			for(String format:iDConfiguration.getImageResizeFormats()){
				String filename = ProcessedImg.generateResizedFileName(width, format);										
				if(!existingFiles.containsKey(filename)){					
					Set<String> formats = missingFormats.get(width);
					if(formats == null){
						formats = new HashSet<>();
						missingFormats.put(width, formats);
					}
					formats.add(format);					
				}else{
					existingFiles.remove(filename);
				}
			}
		}
		
		//old, now unwanted file formats - should be removed
		//note: expensive, blocking call? 
		for(Path unwanted:existingFiles.values()){
			Files.delete(unwanted);
		}		
		
		return resizeForFile(request, missingFormats);			
	} 
	
	private Map<Integer, Set<String>> getAllRequiredFormats(){
		Map<Integer, Set<String>> formats = new HashMap<>();
		for(Integer width:iDConfiguration.getImageResizeWidths()){
			formats.put(width, new HashSet<String>(iDConfiguration.getImageResizeFormats()));
		}
		return formats;
	}
	
	
	private void verifyResizeFolderExists(Path resizedFolder) throws IOException{
		if (!Files.exists(resizedFolder)){
			Files.createDirectory(resizedFolder);
		}
	}
	
	@VisibleForTesting
	void setiDConfiguration(IDConfiguration iDConfiguration) {
		this.iDConfiguration = iDConfiguration;
	}
	
	@VisibleForTesting
	void setImageResizer(ImageResizer imageResizer) {
		this.imageResizer = imageResizer;
	}

}
