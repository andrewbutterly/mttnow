package org.ab.imagedownloader.obj;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.StringUtils;
/**
 * State for image being processed.
 * 
 * Also container for file paths related to the downloaded image
 * */
@ParametersAreNonnullByDefault
public class ProcessedImg {

	private static final String LOCAL_FILE_TS_PATTERN = ".*[0-9]";
	
	private String url;
	private String host;
	private String folderPath;
	private String filename;
	private Optional<Path> imgFile;
	
	public ProcessedImg(String url) throws MalformedURLException {
		super();
		this.url = url;		
		this.imgFile = Optional.empty();
		parseURL();
	}

	private void parseURL() throws MalformedURLException {
		URL validURL = new URL(url);
		host = validURL.getHost();
		folderPath = validURL.getPath();
		if(folderPath == null){
			folderPath = "";
			filename = "";
		}else{
			int index = folderPath.lastIndexOf("/");
			if(index>0){
				filename = folderPath.substring(index+1, folderPath.length());
			}else {
				filename = "";
			}
		}						
	}
		
	public String generateLocalFileCanonicalPath(String root){
		StringBuilder buf = new StringBuilder();
		
		buf.append(root).append(java.io.File.separator)
			.append(generateLocalFilePath())
			.append(generateLocalFileName())
			.append(System.currentTimeMillis());

		return buf.toString();
	}
	public String generateLocalFilePath(){		
		StringBuilder buf = new StringBuilder();
		
		buf.append(host).append(java.io.File.separator);
		if(!StringUtils.isBlank(folderPath)){
			buf.append(folderPath).append(java.io.File.separator);
		}
									
		return cleanPath(buf.toString());		
	}
	public String generateResizedFolderName(String parent){
		return parent + java.io.File.separator + "resized" + java.io.File.separator;		
	}
	private String cleanPath(String s){
		return s.replaceAll("^a-zA-Z\\d/", "-");
	}
	public String generateLocalFileName(){
		return (StringUtils.isBlank(filename) ? "" : cleanPath(filename) ) + "_";
	}
	public static String generateResizedFullFileName(String root, Integer width, String format){
		return root + java.io.File.separator + generateResizedFileName(width, format);
	}
	public static String generateResizedFileName(Integer width, String format){
		return width.toString()+"." + format;
	}
		
	public boolean matchFilename(Path localPath){
		String localFilename = localPath.getFileName().toString();
		if(localFilename.matches(generateLocalFileName()+LOCAL_FILE_TS_PATTERN)){
			return true;	
		}	
		return false;
	}
	
	public Optional<Date> getLocalFileTimestamp(Path localPath){
		String localFilename = localPath.getFileName().toString();
		String generatedImgName = generateLocalFileName();
		if(localFilename.matches(generatedImgName+LOCAL_FILE_TS_PATTERN)){
			int index = localFilename.indexOf(generatedImgName);	
			String ts = localFilename.substring(index+(generatedImgName).length(), localFilename.length());
			if(ts != null){
				try{
					long timeStamp = Long.parseLong(ts);
					return Optional.of(new Date(timeStamp));
				}catch(NumberFormatException ex){
					//do nothing
				}
			}			
		}
		return Optional.empty();
	}
	
	
	public Optional<Path> getImgFile() {
		return imgFile;
	}

	public void setImgFile(Optional<Path> imgFile) {
		this.imgFile = imgFile;
	}

	public String getUrl() {
		return url;
	}

	@Nullable
	public String getHost() {
		return host;
	}

	@Nullable
	public String getFolderPath() {
		return folderPath;
	}

	@Nullable
	public String getFilename() {
		return filename;
	}

	@Override
	public String toString() {
		return "ProcessedImg [url=" + url + ", host=" + host + ", folderPath="
				+ folderPath + ", filename=" + filename + ", imgFile="
				+ imgFile + "]";
	}
	
}
