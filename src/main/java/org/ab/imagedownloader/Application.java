package org.ab.imagedownloader;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
/**
 * Entry point for tool
 * */
@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan(basePackages = { "org.ab.*" })
public class Application implements CommandLineRunner {

	private static final Logger LOGGER = Logger.getLogger(Application.class);
	
	@Autowired
	private ImageDownloader imageDownloader;
		
	public static void main(String... args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		
    	if(args == null || args.length <2){
			LOGGER.error("Error: Missing inputs! Usages: ");
			LOGGER.error("java -jar <mttnow-test-ab>.jar <URL> <Output Folder>");			
			return;
		}
    	
    	imageDownloader.process(args[0], args[1]);    	
	}
	
}
