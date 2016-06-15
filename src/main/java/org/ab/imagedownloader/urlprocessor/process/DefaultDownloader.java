package org.ab.imagedownloader.urlprocessor.process;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.PostConstruct;

import org.ab.imagedownloader.IDConfiguration;
import org.ab.imagedownloader.obj.ImgRequest;
import org.ab.imagedownloader.obj.ImgRequest.Status;
import org.ab.imagedownloader.obj.ProcessedImg;
import org.ab.imagedownloader.obj.URLRequest;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;

/**
 * Downloads image URL
 * 
 * If file already exists on disk - use HTTP HEAD to check remote file size & header "last-modified" if available
 * note: a clever version of this would use {file mime type, ETAG} as well as the size & lastMod to validate the file  
 * */
@ParametersAreNonnullByDefault
@Service
public class DefaultDownloader implements Downloader {

	private static final Logger LOGGER = Logger.getLogger(DefaultDownloader.class);
		
	@Autowired
	private IDConfiguration iDConfiguration;
		
	private CloseableHttpClient client;
	private RequestConfig requestConfig;
	
	@PostConstruct
	private void init(){
		client = HttpClients.createDefault();		
		
		requestConfig = RequestConfig.custom()
			    .setConnectionRequestTimeout(iDConfiguration.getImageDownloadTimeout())
			    .setConnectTimeout(iDConfiguration.getImageDownloadTimeout())
			    .setSocketTimeout(iDConfiguration.getImageDownloadTimeout())
			    .build();
	}
	
	@Override
	public ImgRequest process(String imgURL, URLRequest request) {
							
		ProcessedImg processedImage;
		try {
			processedImage = new ProcessedImg(imgURL);			
		} catch (MalformedURLException e) {
			LOGGER.error(String.format("Bad img URL provided for download (despite previous check!) [%s, %s]", imgURL, request.getUrl()));
			return new ImgRequest(imgURL, request.getOutputFolder(), Status.ERROR);
		}
					
		String filePath = processedImage.generateLocalFilePath();
		Status status = Status.OK;
		try{
		
			Optional<Path> localCopy = getLocalFile(request.getOutputFolder(), filePath, processedImage);				
			if(localCopy.isPresent()){
				if(!isHostedFileUpdated(localCopy.get(), processedImage)){
					processedImage.setImgFile(localCopy);
					return new ImgRequest(imgURL, request.getOutputFolder(), Optional.of(processedImage), Status.OK_FILE_NOT_CHANGED);
					
				} else {
					Files.delete(localCopy.get());					
					status = Status.OK_NEW_FILE_VERSION;
				}				
			}
			
			//get the remote copy			
			buildLocalPath(request.getOutputFolder(), filePath);
			Path fullFilePath = Paths.get(processedImage.generateLocalFileCanonicalPath(request.getOutputFolder()));
			downloadFile(processedImage, fullFilePath);		
			if(!processedImage.getImgFile().isPresent()){
				//failed for some reason that was not thrown in download method ! 
				LOGGER.error(String.format("File downloading failed for an unknown reason [%s, %s]", imgURL, request.getUrl()));
				status = Status.ERROR;			
			}
			
			return new ImgRequest(imgURL, request.getOutputFolder(), Optional.of(processedImage), status);				
					
		}catch(Exception e){
			/**edge case: on these error cases, do not try to clean up 'old' versions of the file from previous runs.
			 * assumption: better to have some content, even if it is an out of date version of the file*/			
			LOGGER.error(String.format("Error downloading image file [%s, %s]. will not remove older versions/resizes of file. error: %s", 
					imgURL, request.getUrl(), e.getMessage()));
			return new ImgRequest(imgURL, request.getOutputFolder(), Status.ERROR);
		}			
	}
	private void downloadFile(ProcessedImg processedImage, Path fullFilePath) throws IOException, URISyntaxException {
						
		HttpGet get = new HttpGet(processedImage.getUrl());
		get.setConfig(requestConfig);
		try (CloseableHttpResponse response = client.execute(get)) {
		    HttpEntity entity = response.getEntity();
		    if (entity != null) {		    	
		    	if( entity.getContentLength() >= iDConfiguration.getImageResizeMaxFileSize() ){
		    		throw new IOException(String.format("Downloaded file is too large to resize. aborting [%s]", processedImage.getUrl()));		    		
		    	}		    			    			   
		    	
		        try (FileOutputStream outstream = new FileOutputStream(fullFilePath.toFile())) {
		            entity.writeTo(outstream);			            				            
		            processedImage.setImgFile(Optional.of(fullFilePath));		            		           
		        }
		    }
		}	
	}
	
	private boolean isHostedFileUpdated(Path localCopy, ProcessedImg processedImage) throws IOException, URISyntaxException {
		
		long localSize = localCopy.toFile().length();
		Optional<Date> localLastMod = processedImage.getLocalFileTimestamp(localCopy);
				
		HttpHead head = new HttpHead(processedImage.getUrl());
		head.setConfig(requestConfig);
		try (CloseableHttpResponse response = client.execute(head)) {
			if (response == null) {
				//note: have to assume worst case and retry download ! 
				//TODO - reuse download logic here - download and check size with two local versions ? still cheaper than full resizing...
				LOGGER.error(String.format("error connecting to image URL [%s]: does server support HTTP HEAD ?", processedImage.getUrl()));
				return true;
		    }			
												
			//some HTTP servers do not return Last-Modified for HTTP HEAD. worth trying though...
			Header lastMod = response.getFirstHeader("Last-Modified");
			if(localLastMod.isPresent() && lastMod != null && lastMod.getValue() != null){
				Date remoteLastMod = DateUtils.parseDate(lastMod.getValue());
				if(remoteLastMod!=null && localLastMod.get().after(remoteLastMod)){
					return false;		
				}
			}
			
			//note: assumption - same file name and size == same file
			Header contentLength = response.getFirstHeader("Content-Length");
			if(contentLength != null && contentLength.getValue() != null){
				try{
					Long contentLen = Long.parseLong(contentLength.getValue());
					if(contentLen != null && contentLen == localSize){
						return false;											
					}
				}catch(NumberFormatException e){
					//do nothing
				}
			}
			
			return true;					
		}				
	} 
	
	private void buildLocalPath(String root, String localFilePath) throws IOException {
		
		Path fullPath = Paths.get(root + java.io.File.separator + localFilePath);
		if(Files.exists(fullPath)){
			if(!Files.isDirectory(fullPath)){
				throw new IOException(
						String.format("error creating local directories for to image [%s, %s]. there is an existing file of that name !", 
								root, localFilePath));	
			}
			return;
		}
		Path result = Files.createDirectories(fullPath);
		if (result == null){
			throw new IOException(String.format("error creating local directories for to image [%s, %s]", root, localFilePath));	
		}
	}
	

	private Optional<Path> getLocalFile(String root, String localFilePath, ProcessedImg processedImage) throws IOException {
		
		String filename = processedImage.generateLocalFileName();
		
		Path dir = Paths.get(root + java.io.File.separator + localFilePath);
		if(!Files.isDirectory(dir)){
			return Optional.empty();
		}		

		try (final DirectoryStream<Path> stream = Files.newDirectoryStream(dir, 
				path -> !Files.isDirectory(path) && path.getFileName().toString().startsWith(filename))) {
			for (Path path : stream) {				
				if(processedImage.matchFilename(path)){
					return Optional.of(path);					
				}				
			}		    
		}
		
		return Optional.empty();					
	}

	@VisibleForTesting
	void setiDConfiguration(IDConfiguration iDConfiguration) {
		this.iDConfiguration = iDConfiguration;
	}

	@VisibleForTesting
	void setClient(CloseableHttpClient client) {
		this.client = client;
	}
		
}
