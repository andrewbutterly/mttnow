package org.ab.imagedownloader.obj;
/**
 * URL to HTML file for download
 * */
public class URLRequest {

	public enum URLType{
		LOCAL_FILE,
		HOSTED_FILE
	} 
	
	private String url;
	private String outputFolder;
	private URLType urlType;	
	
	public URLRequest(String url, URLType urlType, String outputFolder) {
		super();
		this.url = url;		
		this.urlType = urlType;
		this.outputFolder = outputFolder;		
	}

	public String getUrl() {
		return url;
	}

	public String getOutputFolder() {
		return outputFolder;
	}

	public URLType getUrlType() {
		return urlType;
	}
	
	@Override
	public String toString() {
		return "URLRequest [url=" + url + ", outputFolder=" + outputFolder
				+ ", urlType=" + urlType + "]";
	}	

}
