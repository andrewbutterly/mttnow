package org.ab.imagedownloader.urlprocessor.parse

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.ab.imagedownloader.IDConfiguration
import org.ab.imagedownloader.obj.URLRequest
import org.ab.imagedownloader.obj.URLRequest.URLType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

import spock.lang.Specification;

class DefaultImgExtractorSpec extends Specification {

	DefaultImgExtractor extractor
	
	IDConfiguration config = Mock(IDConfiguration)
	
	URLRequest request
	
	Document doc = Mock(Document)
	
	def setup(){
				
		config.getUrlDownloadTimeout() >> 1000
				
		request = new URLRequest("http://example.com/index.html", URLType.HOSTED_FILE, "outputFolder")		
	}
		
	def "DefaultImgExtractor - good response from parser"(){
		given:
		Element element = Mock(Element)
		element.attr("src") >>> ["http://example.com/1.jpg", "http://example.com/2.jpg"]

		Elements soupElements = Mock(Elements)
		soupElements.stream() >> [element, element].stream()	
		
		doc.select("img") >> soupElements
		
		DefaultImgExtractor extractor = new ImgExtractorDocResponse()
		extractor.setiDConfiguration(config)
		
		when:
		List<String> images = extractor.getImgSrcFromDoc(request)
		
		then:
		images.size() == 2
		images.contains("http://example.com/1.jpg")
		images.contains("http://example.com/2.jpg")
	}
	
	class ImgExtractorDocResponse extends DefaultImgExtractor{
		@Override
		Document parseLocalFile(String url) throws IOException {
			return doc
		}
		@Override
		Document parseRemoteFile(String url) throws IOException {
			return doc
		}
	}
	
	def "DefaultImgExtractor - bad response from parser"(){
		given:
		ImgExtractorBadResponse extractor = new ImgExtractorBadResponse();
		extractor.setiDConfiguration(config)
		
		when:
		List<String> images = extractor.getImgSrcFromDoc(request)
		
		then:
		images.isEmpty()			
	}
	
	class ImgExtractorBadResponse extends DefaultImgExtractor{
		@Override
		Document parseLocalFile(String url) throws IOException {
			return null
		}
		@Override
		Document parseRemoteFile(String url) throws IOException {
			return null
		}
	}
	
}
