package org.ab.imagedownloader.urlprocessor

import org.ab.imagedownloader.obj.URLRequest

import spock.lang.Specification;
import org.ab.imagedownloader.IDConfiguration
import org.ab.imagedownloader.urlprocessor.parse.DefaultImgExtractor
import org.ab.imagedownloader.urlprocessor.process.DefaultDownloader
import org.ab.imagedownloader.urlprocessor.process.DefaultResizer
import org.ab.imagedownloader.obj.ImgRequest

class DefaultURLProcessorSpec extends Specification {
	
	DefaultURLProcessor processor

	IDConfiguration config = Mock(IDConfiguration)
	def imgExtractor = Mock(DefaultImgExtractor)
	def imageDownloader = Mock(DefaultDownloader)
	def resizer = Mock(DefaultResizer)
		
	URLRequest request
	
	def setup(){		
		config.getImageDownloadPool() >> 1
		config.getImageResizePool() >> 1
		config.getSupportedSchemes() >> ["http"]
		
		processor = new DefaultURLProcessor()
				
		processor.setiDConfiguration(config)
		processor.setImgExtractor(imgExtractor)
		processor.setImageDownloader(imageDownloader)
		processor.setResizer(resizer)
		
		processor.init()
		
		request = new URLRequest("http://example.com", URLRequest.URLType.HOSTED_FILE, "outputFolder")
	}
	
	def "URL processor: no images found"(){
		
		when:
		processor.process(request)
		
		then:
		1 * imgExtractor.getImgSrcFromDoc(request) >> []
		0 * _
		
	}
	
	def "URL processor: three images found"(){
		
		when:
		processor.process(request)
		
		then:
		1 * imgExtractor.getImgSrcFromDoc(request) >> ["http://example.com/1", "http://example.com/2", "http://example.com/3"]		
		1 * imageDownloader.process("http://example.com/1", request) >> Mock(ImgRequest)
		1 * imageDownloader.process("http://example.com/2", request) >> Mock(ImgRequest)
		1 * imageDownloader.process("http://example.com/3", request) >> Mock(ImgRequest)
		3 * resizer.process(_ as ImgRequest)
		
	}
}
