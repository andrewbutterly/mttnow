package org.ab.imagedownloader.urlprocessor.parse;

import java.util.List;

import org.ab.imagedownloader.obj.URLRequest;

public interface ImgExtractor {

	List<String> getImgSrcFromDoc(URLRequest request);
	
}
