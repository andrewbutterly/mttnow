package org.ab.imagedownloader.urlprocessor.process;

import org.ab.imagedownloader.obj.ImgRequest;
import org.ab.imagedownloader.obj.URLRequest;

public interface Downloader {

	ImgRequest process(String imgURL, URLRequest request);	
	
}
