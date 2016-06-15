package org.ab.imagedownloader.utils;

import java.awt.image.BufferedImage;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import org.imgscalr.Scalr;
import org.springframework.stereotype.Component;
/**
 * Using Scalr library.
 * */
@ParametersAreNonnullByDefault
@Component
public class DefaultImageResizer implements ImageResizer {

	@Override
	public Optional<BufferedImage> resize(BufferedImage srcImg, int destinationWidth) {
				
		Scalr.Mode scaleMode = Scalr.Mode.FIT_TO_WIDTH;				
		BufferedImage destImage = Scalr.resize(srcImg, scaleMode, destinationWidth);
		
		return destImage == null ? Optional.empty() : Optional.of(destImage); 
	}
	
}
