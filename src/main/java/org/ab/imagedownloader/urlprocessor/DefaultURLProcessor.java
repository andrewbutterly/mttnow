package org.ab.imagedownloader.urlprocessor;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.PostConstruct;

import org.ab.imagedownloader.IDConfiguration;
import org.ab.imagedownloader.obj.ImgRequest;
import org.ab.imagedownloader.obj.URLRequest;
import org.ab.imagedownloader.urlprocessor.parse.ImgExtractor;
import org.ab.imagedownloader.urlprocessor.process.Downloader;
import org.ab.imagedownloader.urlprocessor.process.Resizer;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;

/**
 * Process a URL: blocking call to download HTML file and extract img tag urls.
 * Submits {download, resizing} to processing pools 
 * */
@ParametersAreNonnullByDefault
@Service
public class DefaultURLProcessor implements URLProcessor {
	
	private static final Logger LOGGER = Logger.getLogger(DefaultURLProcessor.class);
		
	@Autowired
	private IDConfiguration iDConfiguration;
	
	@Autowired
	private ImgExtractor imgExtractor;
	
	@Autowired
	private Downloader imageDownloader;	
	
	@Autowired
	private Resizer resizer;
	
	private ExecutorService dlPool;
	private ExecutorService resizePool;
	private UrlValidator urlValidator;
	
	@PostConstruct
	private void init(){
		dlPool = Executors.newFixedThreadPool(iDConfiguration.getImageDownloadPool());
		resizePool = Executors.newFixedThreadPool(iDConfiguration.getImageResizePool());
		urlValidator = new UrlValidator(iDConfiguration.getSupportedSchemes());		
	}
	
	@Override
	public void process(URLRequest request){
		LOGGER.info(String.format("request recieved (%s, %s, %s) - beginning processing", request.getUrl(), request.getUrlType(), request.getOutputFolder()));
		
		List<String> imgUrls = imgExtractor.getImgSrcFromDoc(request);
		if(imgUrls.isEmpty()){
			LOGGER.info(String.format("no images processed from url (%s). returning", request.getUrl()));
			return;
		}
		
		//validate URLS and remove any duplicates
		Set<String> urls = validateURLsAndStripDuplicates(imgUrls);		
		LOGGER.info(String.format("found %d unique images to process from url (%s)", urls.size(), request.getUrl()));
		
		urls.stream()					
				.map(imgURL->processDownload(imgURL, request))						
				.map(future -> future.thenCompose(this::processResize))
				.map(future -> future.join())
				.collect(Collectors.toList());
				;	
		
		//blocking for all tasks to finish		
		dlPool.shutdown();
		resizePool.shutdown();
		
		LOGGER.info(String.format("processed %d images to process from url (%s)", urls.size(), request.getUrl()));
		
	}		
	private Set<String> validateURLsAndStripDuplicates(List<String> urls){			
		return urls.stream()
			.filter(urlValidator::isValid)
			.map(this::stripQueryStringFromURL)
			.collect(Collectors.toSet());				
	}
	/*note: removing the query string is an assumption - have to check*/
	private String stripQueryStringFromURL(String url){
		int index = url.indexOf("?");		
		return index<1 ? url : url.substring(0, index);		
	} 
	
	private CompletableFuture<ImgRequest> processDownload(String imgURL, URLRequest request){	
		return CompletableFuture.supplyAsync(()-> imageDownloader.process(imgURL, request), dlPool);		
	}
	
	private CompletableFuture<ImgRequest> processResize(ImgRequest request){
		return CompletableFuture.supplyAsync(()-> resizer.process(request), resizePool);
	}

	@VisibleForTesting
	void setiDConfiguration(IDConfiguration iDConfiguration) {
		this.iDConfiguration = iDConfiguration;
	}

	@VisibleForTesting
	void setImgExtractor(ImgExtractor imgExtractor) {
		this.imgExtractor = imgExtractor;
	}

	@VisibleForTesting
	void setImageDownloader(Downloader imageDownloader) {
		this.imageDownloader = imageDownloader;
	}

	@VisibleForTesting
	void setResizer(Resizer resizer) {
		this.resizer = resizer;
	}
	
}
