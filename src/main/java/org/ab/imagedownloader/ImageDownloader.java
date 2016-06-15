package org.ab.imagedownloader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.PostConstruct;

import org.ab.imagedownloader.obj.URLRequest;
import org.ab.imagedownloader.urlprocessor.URLProcessor;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;

/**
 * Images Downloader.
 * 
 * Check inputs and output directory. Forward request
 * */
@ParametersAreNonnullByDefault
@Service
public class ImageDownloader {
	
	private static final Logger LOGGER = Logger.getLogger(ImageDownloader.class);
		
	@Autowired
	private IDConfiguration iDConfiguration;
	
	@Autowired
	private URLProcessor urlProcessor;
	
	private UrlValidator urlValidator;
	
	@PostConstruct
	private void init(){
		urlValidator = new UrlValidator(iDConfiguration.getSupportedSchemes());		
	}
		
	public void process(String url, String outputFolder){
		
		Optional<URLRequest> pageURL = null;
		try{
			if(StringUtils.isBlank(url) || StringUtils.isBlank(outputFolder)){
				logValidationError(url, outputFolder, "Missing inputs");							
				return;
			}
			validateOrCreateOutFolder(outputFolder);
			
			pageURL = validateAndGetURLRequest(url, outputFolder);			
			if(!pageURL.isPresent()){
				LOGGER.error(String.format("bad URL provided [%s], returning", url));
				return;
			}
									
		}catch(Exception e){
			logValidationError(url, outputFolder, e.getMessage());
			return;
		}	
		
		try{
			urlProcessor.process(pageURL.get());					
		}catch(Exception e){
			LOGGER.error(String.format("error processing URL file [%s], error: %s", pageURL.get(), e.getMessage()));
		}
				
	}
		
	private Optional<URLRequest> validateAndGetURLRequest(String url, String outputFolder) throws MalformedURLException {			
		if(!urlValidator.isValid(url.toLowerCase())){
			//is a local file rather than a HTTP link ?
			Path file = Paths.get(url);
			if (!Files.exists(file) || Files.isDirectory(file)){
				//error !					
				logValidationError(url, null, "Invalid URL or local file reference");			
				return Optional.empty();
			}else{
				return Optional.of(new URLRequest(url, URLRequest.URLType.LOCAL_FILE, outputFolder));
			}
		
		}				
		return Optional.of(new URLRequest(url, URLRequest.URLType.HOSTED_FILE, outputFolder));			
	}	
	
	private void validateOrCreateOutFolder(String outputFolder) throws IOException {
		if(Files.isDirectory(Paths.get(outputFolder))){
			return;
		}		
		Files.createDirectory(Paths.get(outputFolder));
	}
	
	private void logValidationError(@Nullable String url, @Nullable String outputFolder, String errorMessage){					
		LOGGER.error(String.format("Error: Bad inputs provided! [%s, %s], error: %s ", url, outputFolder, errorMessage));		
	}
		   
    @VisibleForTesting
	void setiDConfiguration(IDConfiguration iDConfiguration) {
		this.iDConfiguration = iDConfiguration;
	}

    @VisibleForTesting
	void setUrlProcessor(URLProcessor urlProcessor) {
		this.urlProcessor = urlProcessor;
	}
		
}
