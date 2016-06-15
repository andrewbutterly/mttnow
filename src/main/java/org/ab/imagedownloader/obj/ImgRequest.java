package org.ab.imagedownloader.obj;

import java.util.Optional;
/**
 * info and state for single image URL in the download 'queue'
 * */
public class ImgRequest {

	public enum Status{
		OK,
		OK_NEW_FILE_VERSION,
		OK_FILE_NOT_CHANGED,
		ERROR
	}
	
	private String url;
	private String outputFolder;
	private Optional<ProcessedImg> processedImg;
	private Status status;
	
	public ImgRequest(String url, String outputFolder, Status status) {
		this(url, outputFolder, Optional.empty(), status);
	}
	
	public ImgRequest(String url, String outputFolder, Optional<ProcessedImg> processedImg, Status status) {
		super();
		this.url = url;
		this.outputFolder = outputFolder;		
		this.status = status;
		this.processedImg = processedImg;
	}
	
	public String getUrl() {
		return url;
	}

	public String getOutputFolder() {
		return outputFolder;
	}

	public Optional<ProcessedImg> getProcessedImg() {
		return processedImg;
	}

	public void setProcessedImg(Optional<ProcessedImg> processedImg) {
		this.processedImg = processedImg;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "ImgRequest [url=" + url + ", outputFolder=" + outputFolder
				+ ", processedImg=" + processedImg + ", status=" + status + "]";
	}
}
