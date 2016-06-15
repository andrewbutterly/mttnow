package org.ab.imagedownloader.urlprocessor.process

import java.util.Optional;
import java.nio.file.Path;

import org.ab.imagedownloader.IDConfiguration
import org.ab.imagedownloader.obj.ProcessedImg
import org.ab.imagedownloader.obj.URLRequest
import org.ab.imagedownloader.obj.ImgRequest.Status;
import org.ab.imagedownloader.obj.URLRequest.URLType;
import org.ab.imagedownloader.obj.ImgRequest;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpHead
import org.apache.http.impl.client.CloseableHttpClient

import spock.lang.Specification;

class DefaultDownloaderSpec extends Specification {
	
	DefaultDownloader downloader

	IDConfiguration config = Mock(IDConfiguration)
	
	URLRequest request
		
	CloseableHttpClient httpClient
	
	def setup(){
			
		System.properties['java.io.tmpdir'] = "build/test"		
		new File("build/test").mkdirs()
						
		httpClient = Mock (CloseableHttpClient)
		
		downloader = new DefaultDownloader()
		downloader.setiDConfiguration(config)		
		downloader.init();
		downloader.setClient(httpClient)
				
		request = new URLRequest("http://example.com/index.html", URLType.HOSTED_FILE, "build/test")
	}
	def cleanupSpec() {
		new File("build/test").deleteDir()
	}	
	
	def "DefaultDownloader - download file"(){
		given:
		config.getImageResizeMaxFileSize() >> 20
		
				
		CloseableHttpResponse getResponse =  Mock(CloseableHttpResponse)
		HttpEntity entity = Mock(HttpEntity)
		entity.getContentLength() >> 10
		getResponse.getEntity() >> entity
		httpClient.execute(_ as HttpGet) >> getResponse
		
		request = new URLRequest("http://existingDLSrcImg.com/index.html", URLType.HOSTED_FILE, "build/test")

		when:
		ImgRequest chain = downloader.process("http://existingDLSrcImg.com/images/ok.jpg", request)
		
		then:
		chain.getUrl().equals("http://existingDLSrcImg.com/images/ok.jpg")
		chain.getOutputFolder().equals("build/test")
		chain.getStatus().equals(ImgRequest.Status.OK)
	}
	
	def "DefaultDownloader - download file too big"(){
		given:
		config.getImageResizeMaxFileSize() >> 20
						
		CloseableHttpResponse getResponse =  Mock(CloseableHttpResponse)
		HttpEntity entity = Mock(HttpEntity)
		entity.getContentLength() >> 30
		getResponse.getEntity() >> entity
		httpClient.execute(_ as HttpGet) >> getResponse
		
		request = new URLRequest("http://existingDLSrcImg.com/index.html", URLType.HOSTED_FILE, "build/test")

		when:
		ImgRequest chain = downloader.process("http://existingDLSrcImg.com/images/tooBig.jpg", request)
		
		then:
		chain.getUrl().equals("http://existingDLSrcImg.com/images/tooBig.jpg")
		chain.getOutputFolder().equals("build/test")
		chain.getStatus().equals(ImgRequest.Status.ERROR)
	}	
		
	def "DefaultDownloader - existing downloaded src image, local copy is newer"(){
		given:		
		new File("build/test/existingDLSrcImg.com/images/localNewer.jpg").mkdirs()
		new File("build/test/existingDLSrcImg.com/images/localNewer.jpg/localNewer.jpg_"+System.currentTimeMillis()).createNewFile()
					
		Header lastMod = Mock (Header)
		lastMod.getValue() >> "Tue, 15 Nov 1990 12:45:26 GMT"
		CloseableHttpResponse response =  Mock(CloseableHttpResponse)
		response.getFirstHeader("Last-Modified") >> lastMod			
		httpClient.execute(_ as HttpHead) >> response		
		request = new URLRequest("http://existingDLSrcImg.com/index.html", URLType.HOSTED_FILE, "build/test")
		
		when:
		ImgRequest chain = downloader.process("http://existingDLSrcImg.com/images/localNewer.jpg", request)
		
		then:
		chain.getUrl().equals("http://existingDLSrcImg.com/images/localNewer.jpg")
		chain.getOutputFolder().equals("build/test")
		chain.getStatus().equals(ImgRequest.Status.OK_FILE_NOT_CHANGED)
		
		chain.getProcessedImg().isPresent()
		ProcessedImg image = chain.getProcessedImg().get()
		image.getUrl().equals("http://existingDLSrcImg.com/images/localNewer.jpg")
		image.getFolderPath().equals("/images/localNewer.jpg")
		image.getFilename().equals("localNewer.jpg")
		
		image.getImgFile().isPresent()
	
	}
	
	def "DefaultDownloader - existing downloaded src image, local copy is same size"(){
		given:
		new File("build/test/existingDLSrcImg.com/images/localSameSize.jpg").mkdirs()
		new File("build/test/existingDLSrcImg.com/images/localSameSize.jpg/localSameSize.jpg_"+System.currentTimeMillis()).createNewFile()
					
		Header contentLen = Mock (Header)
		contentLen.getValue() >> "0"
		CloseableHttpResponse response =  Mock(CloseableHttpResponse)
		response.getFirstHeader("Content-Length") >> contentLen
		httpClient.execute(_ as HttpHead) >> response
		request = new URLRequest("http://existingDLSrcImg.com/index.html", URLType.HOSTED_FILE, "build/test")
		
		when:
		ImgRequest chain = downloader.process("http://existingDLSrcImg.com/images/localSameSize.jpg", request)
		
		then:
		chain.getUrl().equals("http://existingDLSrcImg.com/images/localSameSize.jpg")
		chain.getOutputFolder().equals("build/test")
		chain.getStatus().equals(ImgRequest.Status.OK_FILE_NOT_CHANGED)
		
		chain.getProcessedImg().isPresent()
		ProcessedImg image = chain.getProcessedImg().get()
		image.getUrl().equals("http://existingDLSrcImg.com/images/localSameSize.jpg")
		image.getFolderPath().equals("/images/localSameSize.jpg")
		image.getFilename().equals("localSameSize.jpg")
		
		image.getImgFile().isPresent()
	
	}
	
	def "DefaultDownloader - existing downloaded src image, remote copy is newer"(){
		given:
		config.getImageResizeMaxFileSize() >> 20
		
		new File("build/test/existingDLSrcImg.com/images/remoteNewer.jpg").mkdirs()
		new File("build/test/existingDLSrcImg.com/images/remoteNewer.jpg/remoteNewer.jpg_1234567890").createNewFile()
					
		Header lastMod = Mock (Header)
		lastMod.getValue() >> "Tue, 15 Nov 2016 12:45:26 GMT"
		CloseableHttpResponse headResponse =  Mock(CloseableHttpResponse)
		headResponse.getFirstHeader("Last-Modified") >> lastMod
		httpClient.execute(_ as HttpHead) >> headResponse
		
		
		CloseableHttpResponse getResponse =  Mock(CloseableHttpResponse)
		HttpEntity entity = Mock(HttpEntity)
		entity.getContentLength() >> 1
		getResponse.getEntity() >> entity
		httpClient.execute(_ as HttpGet) >> getResponse		
		
		request = new URLRequest("http://existingDLSrcImg.com/index.html", URLType.HOSTED_FILE, "build/test")

		when:
		ImgRequest chain = downloader.process("http://existingDLSrcImg.com/images/remoteNewer.jpg", request)
		
		then:
		chain.getUrl().equals("http://existingDLSrcImg.com/images/remoteNewer.jpg")
		chain.getOutputFolder().equals("build/test")
		chain.getStatus().equals(ImgRequest.Status.OK_NEW_FILE_VERSION)			
	}
	
	def "DefaultDownloader - target folder is previously existing file"(){
		given:
		new File("build/test/example.org").mkdir()
		new File("build/test/example.org/existing.jpg").createNewFile()
		
		request = new URLRequest("http://example.org/index.html", URLType.HOSTED_FILE, "build/test")
		
		when:
		ImgRequest chain = downloader.process("http://example.org/existing.jpg", request)
		
		then:
		chain.getUrl().equals("http://example.org/existing.jpg")
		chain.getOutputFolder().equals("build/test")
		chain.getStatus().equals(ImgRequest.Status.ERROR)
		
	}
	
	def "DefaultDownloader - bad inputs"(){
		
		when:
		ImgRequest chain = downloader.process("garbageURL", request)
		
		then:
		chain.getUrl().equals("garbageURL")
		chain.getOutputFolder().equals("build/test")
		chain.getStatus().equals(ImgRequest.Status.ERROR)
		
	}
}
