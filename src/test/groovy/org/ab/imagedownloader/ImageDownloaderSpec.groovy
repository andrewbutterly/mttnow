package org.ab.imagedownloader

import java.util.Optional;

import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

import org.ab.imagedownloader.obj.URLRequest
import org.ab.imagedownloader.urlprocessor.DefaultURLProcessor

@RestoreSystemProperties
class ImageDownloaderSpec extends Specification {

	ImageDownloader downloader = new ImageDownloader();
	
	def config = Mock(IDConfiguration)
	def urlProcessor = Mock(DefaultURLProcessor)
	
	def setup(){
		
		config.getSupportedSchemes() >> ["http"]
		
		downloader = new ImageDownloader();
		downloader.setiDConfiguration(config);
		downloader.setUrlProcessor(urlProcessor);
		downloader.init()
		
		System.properties['java.io.tmpdir'] = "build/test"
		new File("build/test").mkdirs()
		new File("build/test/index.html").createNewFile()
	}
	def cleanupSpec() {
		new File("build/test").deleteDir()
	}
	
	def "Download image from hosted file"(){
		
		given:
		def url = "HTTP://EXAMPLE.COM"
		def outputFolder = "build/test"
			
		when:
		downloader.process(url, outputFolder)
				
		then:
		1 * urlProcessor.process({
			it.getUrl().equals("HTTP://EXAMPLE.COM") && it.getOutputFolder().equals("build/test") && 
			it.getUrlType().equals(URLRequest.URLType.HOSTED_FILE)
		} as URLRequest)			
	}
	
	def "Download image from local file"(){
		
		given:
		def url = "build/test/index.html"
		def outputFolder = "build/test"
			
		when:
		downloader.process(url, outputFolder)
				
		then:
		1 * urlProcessor.process({
			it.getUrl().equals("build/test/index.html") && it.getOutputFolder().equals("build/test") &&
			it.getUrlType().equals(URLRequest.URLType.LOCAL_FILE)
		} as URLRequest)
	}
	
	def "Download image: bad input - missing local HTML file"(){
		
		given:
		def url = "somegarbage"
		def outputFolder = "build/test"
			
		when:
		downloader.process(url, outputFolder)
				
		then:
		0 * urlProcessor.process(_)
	}
	
	def "Download image: bad input - local HTML file is a directory"(){
		
		given:
		def url = "build/test"
		def outputFolder = "build/test"
			
		when:
		downloader.process(url, outputFolder)
				
		then:
		0 * urlProcessor.process(_)
	}
	
	def "Download image: bad input - url"(){
		
		given:
		def url = null
		def outputFolder = "build/test"	
			
		when:
		downloader.process(url, outputFolder)
				
		then:
		0 * urlProcessor.process(_)
	}
	def "Download image: bad input - output folder"(){
		given:
		def url = "http://example.com"
		def outputFolder = ""
		
		when:
		downloader.process(url, outputFolder)
		
		then:
		0 * urlProcessor.process(_)
		
	}
}
