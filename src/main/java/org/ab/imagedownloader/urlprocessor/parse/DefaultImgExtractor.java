package org.ab.imagedownloader.urlprocessor.parse;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.ab.imagedownloader.IDConfiguration;
import org.ab.imagedownloader.obj.URLRequest;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
/**
 * Impl of ImageExtractor. Uses jSoup lib to load and parse HTML DOC.
 * 
 * note: This work could be done by a regex - but after looking at popular options online 
 * i thought it would be less brittle to use a robust 3rd party library   
 * */
@ParametersAreNonnullByDefault
@Service
public class DefaultImgExtractor implements ImgExtractor {
		
	private static final Logger LOGGER = Logger.getLogger(DefaultImgExtractor.class);
	
	@Autowired
	private IDConfiguration iDConfiguration;
	
	@Override
	public List<String> getImgSrcFromDoc(URLRequest request){
		
		try{
			Document document;
			switch(request.getUrlType()){
				case LOCAL_FILE:
					document = parseLocalFile(request.getUrl()); 
					break;
				case HOSTED_FILE:
					document = parseRemoteFile(request.getUrl());
					break;
				default:
					LOGGER.error(String.format("unknown url type passed into request (%s, %s)", request.getUrlType(), request.getUrl()));
					return Collections.emptyList();
			}
								
			if(document == null){
				LOGGER.error(String.format("unknown error parsing URL request (%s, %s)", request.getUrlType(), request.getUrl()));
				return Collections.emptyList();
			}					
						
			return document.select("img").stream().map(element->element.attr("src")).collect(Collectors.<String>toList());
						
		}catch(Exception e){
			LOGGER.error(String.format("error parsing file request (%s, %s, %s). error: %s", 
					request.getUrlType(), request.getUrl(), request.getOutputFolder(), e.getMessage()));
			//do nothing otherwise ! more advanced version might throw a custom error, or try to recover more gracefully
			return Collections.emptyList();
		}					
	}
	
	@VisibleForTesting
	Document parseLocalFile(String url) throws IOException {
		//note: no base uri available in this case... future version might be more clever here
		return Jsoup.parse(new File(url), "UTF-8", "");
	}
	
	@VisibleForTesting
	Document parseRemoteFile(String url) throws IOException {		
		return Jsoup.connect(url).timeout(iDConfiguration.getUrlDownloadTimeout()).get(); 
	}

	@VisibleForTesting
	void setiDConfiguration(IDConfiguration iDConfiguration) {
		this.iDConfiguration = iDConfiguration;
	}
	
	
}
