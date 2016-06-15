package org.ab.imagedownloader.urlprocessor.process

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.FileOutputStream;
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.util.Optional

import javax.imageio.ImageIO;

import org.ab.imagedownloader.IDConfiguration
import org.ab.imagedownloader.obj.ImgRequest
import org.ab.imagedownloader.obj.ProcessedImg
import org.ab.imagedownloader.obj.URLRequest
import org.ab.imagedownloader.obj.ImgRequest.Status
import org.ab.imagedownloader.utils.ImageResizer

import spock.lang.Specification;

class DefaultResizerSpec extends Specification {

	DefaultResizer resizer
	
	IDConfiguration config = Mock(IDConfiguration)
	ImageResizer library = Mock(ImageResizer)
	
	//Path imgFile 
	//ProcessedImg processing = new ProcessedImg("http://example.com/images/1.jpg")
	//ImgRequest request
	
	def setup(){
		
		System.properties['java.io.tmpdir'] = "build/test"
				
		resizer = new DefaultResizer()
		resizer.setiDConfiguration(config)
		resizer.setImageResizer(library)
		/*		
		imgFile = Paths.get("build/test/example.com/image.jpg/image.jpg_1234567890")
		processing.setImgFile(Optional.of(imgFile))
		request = new ImgRequest(processing.getUrl(), "build/test", Optional.of(processing), ImgRequest.Status.OK)
		*/
	}
	def cleanupSpec() {
		new File("build/test").deleteDir()
	}
	
	def "DefaultResizer - new version of image, remove old resized, create new ones"(){
		given:
		new File("build/test/example.com/imageChangedSmall.jpg/resized/").mkdirs()
		
		Image img = new BufferedImage ( 500, 500, BufferedImage.TYPE_INT_ARGB )
		ImageIO.write(img, "jpg", new FileOutputStream("build/test/example.com/imageChangedSmall.jpg/imageChangedSmall.jpg_1234567890"));
		
		new File("build/test/example.com/imageChangedSmall.jpg/resized/300.jpg").createNewFile()
		new File("build/test/example.com/imageChangedSmall.jpg/resized/400.jpg").createNewFile()
		new File("build/test/example.com/imageChangedSmall.jpg/resized/300.png").createNewFile()
		new File("build/test/example.com/imageChangedSmall.jpg/resized/400.png").createNewFile()
		
		Path imgFile = Paths.get("build/test/example.com/imageChangedSmall.jpg/imageChangedSmall.jpg_1234567890")
		ProcessedImg processing = new ProcessedImg("http://example.com/imageChangedSmall.jpg")
		processing.setImgFile(Optional.of(imgFile))
		
		when:
		resizer.process(new ImgRequest("http://example.com/imageChangedSmall.jpg", "build/test", Optional.of(processing), ImgRequest.Status.OK_NEW_FILE_VERSION))
		
		then:
		1 * config.getImageResizeWidths() >> [100, 200]
		2 * config.getImageResizeFormats() >> ['jpg', 'png']
		1 * config.getImageResizeMinHeight() >> 10
		1 * config.getImageResizeMinWidth() >> 10
		1 * library.resize(_ as BufferedImage, 200)  >> Optional.of(new BufferedImage ( 200, 200, BufferedImage.TYPE_INT_ARGB ))
		1 * library.resize(_ as BufferedImage, 100)  >> Optional.of(new BufferedImage ( 100, 100, BufferedImage.TYPE_INT_ARGB ))
			
		0 * _
		
		!new File("build/test/example.com/imageChangedSmall.jpg/resized/300.jpg").exists()
		!new File("build/test/example.com/imageChangedSmall.jpg/resized/400.jpg").exists()
		!new File("build/test/example.com/imageChangedSmall.jpg/resized/300.png").exists()
		!new File("build/test/example.com/imageChangedSmall.jpg/resized/400.png").exists()
		new File("build/test/example.com/imageChangedSmall.jpg/resized/100.jpg").exists()
		new File("build/test/example.com/imageChangedSmall.jpg/resized/200.jpg").exists()
		new File("build/test/example.com/imageChangedSmall.jpg/resized/100.png").exists()
		new File("build/test/example.com/imageChangedSmall.jpg/resized/200.png").exists()
	}
	
	def "DefaultResizer - new version of image, too small to resize"(){
		given:			
		new File("build/test/example.com/imageChangedSmall.jpg/resized/").mkdirs()
		
		Image img = new BufferedImage ( 9, 9, BufferedImage.TYPE_INT_ARGB )
		ImageIO.write(img, "jpg", new FileOutputStream("build/test/example.com/imageChangedSmall.jpg/imageChangedSmall.jpg_1234567890"));			
		
		new File("build/test/example.com/imageChangedSmall.jpg/resized/300.jpg").createNewFile()
		new File("build/test/example.com/imageChangedSmall.jpg/resized/400.jpg").createNewFile()
		new File("build/test/example.com/imageChangedSmall.jpg/resized/300.png").createNewFile()
		new File("build/test/example.com/imageChangedSmall.jpg/resized/400.png").createNewFile()		
		
		Path imgFile = Paths.get("build/test/example.com/imageChangedSmall.jpg/imageChangedSmall.jpg_1234567890")
		ProcessedImg processing = new ProcessedImg("http://example.com/imageChangedSmall.jpg")
		processing.setImgFile(Optional.of(imgFile))
		
		when:
		resizer.process(new ImgRequest("http://example.com/imageChangedSmall.jpg", "build/test", Optional.of(processing), ImgRequest.Status.OK_NEW_FILE_VERSION))
		
		then:
		1 * config.getImageResizeWidths() >> [100, 200]
		2 * config.getImageResizeFormats() >> ['jpg', 'png']
		1 * config.getImageResizeMinHeight() >> 10		
		
		0 * _
		
		!new File("build/test/example.com/imageChangedSmall.jpg/resized/300.jpg").exists()
		!new File("build/test/example.com/imageChangedSmall.jpg/resized/400.jpg").exists()
		!new File("build/test/example.com/imageChangedSmall.jpg/resized/300.png").exists()
		!new File("build/test/example.com/imageChangedSmall.jpg/resized/400.png").exists()
	}
	
	def "DefaultResizer - existing image, one out of date format"(){
		given:
		new File("build/test/example.com/imageNotChanged.jpg/resized").mkdirs()
		new File("build/test/example.com/imageNotChanged.jpg/imageNotChanged.jpg_1234567890").createNewFile()
		new File("build/test/example.com/imageNotChanged.jpg/resized/100.jpg").createNewFile()
		new File("build/test/example.com/imageNotChanged.jpg/resized/200.jpg").createNewFile()
		new File("build/test/example.com/imageNotChanged.jpg/resized/100.png").createNewFile()
		new File("build/test/example.com/imageNotChanged.jpg/resized/200.png").createNewFile()
		new File("build/test/example.com/imageNotChanged.jpg/resized/200.gif").createNewFile()
		
		Path imgFile = Paths.get("build/test/example.com/imageNotChanged.jpg/imageNotChanged.jpg_1234567890")
		ProcessedImg processing = new ProcessedImg("http://example.com/imageNotChanged.jpg")
		processing.setImgFile(Optional.of(imgFile))
		
		when:
		resizer.process(new ImgRequest("http://example.com/imageNotChanged.jpg", "build/test", Optional.of(processing), ImgRequest.Status.OK_FILE_NOT_CHANGED))
		
		then:		
		1 * config.getImageResizeWidths() >> [100, 200]
		2 * config.getImageResizeFormats() >> ['jpg', 'png']
		0 * _
		!new File("build/test/example.com/imageNotChanged.jpg/resized/200.gif").exists()
		new File("build/test/example.com/imageNotChanged.jpg/resized/100.jpg").exists()
		new File("build/test/example.com/imageNotChanged.jpg/resized/200.jpg").exists()
		new File("build/test/example.com/imageNotChanged.jpg/resized/100.png").exists()
		new File("build/test/example.com/imageNotChanged.jpg/resized/200.png").exists()
	}
	
	def "DefaultResizer - bad inputs (image state object)"(){
			
		given:
		ProcessedImg processing = new ProcessedImg("http://example.com/images/1.jpg")
		
		when:
		resizer.process(new ImgRequest(processing.getUrl(), "build/test", Optional.empty(), ImgRequest.Status.OK))
		then:
		0 * _
		
		when:
		resizer.process(new ImgRequest(processing.getUrl(), "build/test", Optional.of(processing), ImgRequest.Status.OK))
		then:
		0 * _
		
	}
	
	def "DefaultResizer - error in previous link in chain"(){
		
	when:
	resizer.process(new ImgRequest(null, null, null, ImgRequest.Status.ERROR))
	
	then:
	0 * _
	
}
	
	
}
