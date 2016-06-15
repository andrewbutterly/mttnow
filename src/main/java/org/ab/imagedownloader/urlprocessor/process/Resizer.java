package org.ab.imagedownloader.urlprocessor.process;

import org.ab.imagedownloader.obj.ImgRequest;

public interface Resizer {

	ImgRequest process(ImgRequest request);
	
}
