package org.ab.imagedownloader;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Configuration class - loads from application.properties
 * */
@ParametersAreNonnullByDefault
@Configuration
@PropertySource("classpath:application.properties")
public class IDConfiguration {
	
	@Value("#{'${url.supported_schemes}'.split(',')}")
    private List<String> supportedSchemes;
		
	@Value("${url.download.timeout}")
    private int urlDownloadTimeout;
	
	@Value("${image.downloaders.pool_size}")
    private int imageDownloadPool;
	
	@Value("${image.downloaders.timeout}")
    private int imageDownloadTimeout;
	
	@Value("${image.resizers.pool_size}")
    private int imageResizePool;
		
	@Value("#{'${image.resizers.formats}'.split(',')}")
    private List<String> imageResizeFormats;
	
	@Value("#{'${image.resizers.widths_px}'.split(',')}")
    private List<Integer> imageResizeWidths;	
	
	@Value("${image.resizers.min.width_px}")
    private int imageResizeMinWidth;
	
	@Value("${image.resizers.min.height_px}")
    private int imageResizeMinHeight;
		
	@Value("${image.resizers.max_filesize_bytes}")
    private long imageResizeMaxFileSize;

	
	@Nullable
	public String[] getSupportedSchemes() {
		return supportedSchemes == null ? null : supportedSchemes.toArray(new String[supportedSchemes.size()]);
	}
	
	public int getUrlDownloadTimeout() {
		return urlDownloadTimeout;
	}

	public int getImageDownloadPool() {
		return imageDownloadPool;
	}

	public int getImageDownloadTimeout() {
		return imageDownloadTimeout;
	}

	public int getImageResizePool() {
		return imageResizePool;
	}

	public List<String> getImageResizeFormats() {
		return imageResizeFormats;
	}

	public List<Integer> getImageResizeWidths() {
		return imageResizeWidths;
	}

	public int getImageResizeMinWidth() {
		return imageResizeMinWidth;
	}

	public int getImageResizeMinHeight() {
		return imageResizeMinHeight;
	}

	public long getImageResizeMaxFileSize() {
		return imageResizeMaxFileSize;
	}
		
}
