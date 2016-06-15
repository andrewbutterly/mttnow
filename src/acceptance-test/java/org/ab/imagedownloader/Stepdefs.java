package org.ab.imagedownloader;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.springframework.boot.SpringApplication;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class Stepdefs {
	
	@When("^The sample test HTML file is passed in with output folder \"([^\"]*)\"$")
	public void the_sample_test_HTML_file_is_passed_in_with_output_folder(String output) throws Throwable {
	   									
		SpringApplication.run(Application.class, "./src/test/resources/simple.html", output);
		
	}	
	
	@Then("^There should be (\\d+) files created in output folder \"([^\"]*)\"$")
	public void there_should_be_files_created_in_output_folder(int fileCount, String output) throws Throwable  {
	    
		int totalFound = Files.walk(Paths.get(output)).filter(Files::isRegularFile).mapToInt(f->1).sum();
		assertTrue(totalFound == fileCount);
			
	}
	
	@Then("^The output folder \"([^\"]*)\" should be cleaned$")
	public void the_output_folder_should_be_cleaned(String output) throws Throwable {
		//purge output folder
		FileUtils.deleteDirectory(new File(output));
	}
}
