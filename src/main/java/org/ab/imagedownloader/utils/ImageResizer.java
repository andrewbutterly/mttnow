package org.ab.imagedownloader.utils;

import java.awt.image.BufferedImage;
import java.util.Optional;

public interface ImageResizer {

	Optional<BufferedImage> resize(BufferedImage bimg, int destinationWidth);
		
}
