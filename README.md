## Introduction

This project is an attempt at the MTTNow Image downloader test.

Given a URL to a HTML file and output directory, it will parse the HTML file for <img/> tags, and download and resize any images referenced in those tags.
Target formats, sizes, and other variables are taken from a properties file in ./resources/application.properties

NOTE: Due to running out of time I have not written any load testing, and only one very basic and limited acceptance test. My apologies for this.

## Dependencies

 - Gradle 2.9
 - Java 1.8
 - JRE HotSpot, or JRE supporting JAI

## A specific JRE !?!

This tool uses the Java Advanced Imaging libraries to change image formats. These libraries are not included in some standard Java Runtime Environments by default - for example OpenJDK.
Generally the JAI libraries are specific to a target Operating System, and have to be installed. They also require a licensing agreement. 
It would be possible to download and install them on a target machine as part of running this tool, but this would break ease of testing! 

If this tool was going to be developed for "real-life" use, I would drop the use of both JAI and also the resizing library used, and replace them both with a thin wrapper around a 3rd party native tool installed in the target OS. Existing native tools like ImageMagik provide a better quality, faster approach for image resizing/formatting than anything currently possible in a pure Java approach. 
I have not done this for this version of the tool though as it would make testing more difficult!

As a stopgap for this test project, using an Oracle JRE (or JRE supporting Advanced Imaging) will allow the code to be run and tested with the understanding that a much better solution is possible.
The use of this limited image reformatter has also limited the supported image types to the major formats.

## How to build and run

 - build and run unit tests

> ./gradlew clean build 
> ./gradlew clean test

 - run acceptance tests
 
> ./gradlew acceptanceTest

 - Building on the command line from the project root will create a fat jar in ./build/libs/mttnow-test-ab-0.0.1.jar
 - To run it on the command line from project root
 
> java -jar ./build/libs/mttnow-test-ab-0.0.1.jar <URL To HTTP File> <Absolute or Relative Path to Output Folder>

 - local HTML files are also permitted.
 - Examples:

> java -jar ./build/libs/mttnow-test-ab-0.0.1.jar 'http://www.mttnow.com/' ./output

> java -jar ./build/libs/mttnow-test-ab-0.0.1.jar ./test/index.html /home/andrew/work/output

## Output

This tool preserves the hierarchy of downloaded files when saving and resizing them. It also creates a unique directory per unique image file
 - This is to prevent overwriting with similar filenames and directory structures across different website runs
 - for example, if downloading http://www.example.com/images/horse.jpg to './output' : 
 	- the tool will save the image to the folder "./output/www.example.com/images/horse.jpg/", with a filename of 'horse.jpg_TIMESTAMP'
 	- resized images are stored in a subfolder - e.g. "./output/www.example.com/images/horse.jpg/resized/..."
 	- this is to allow an image serving process to more easily map a requested image to a resized version and serve it later on
 	- e.g a mobile client request for 'http://www.example.com/images/horse.jpg' might return the file './output/www.example.com/images/horse.jpg/resized/100.png'


## Design choices 	

This is a Spring Boot application. Spring was chosen as it allows for fast development.
The use of Spring allowed for the easy use of dependency injection throughout the code. 
Where practical, interfaces rather than concrete classes are referenced in business logic, using Spring to inject the Concrete class. This separation of concerns allows for cleaner code.

Downloads and Resizing tasks are run via thread pools set up for each task type. This was to allow for efficient completion without exhausting the target machine. 
A request is passed from executor to executor in chaining calls. Long running tasks (those run via pools) are asynchronous - the calling thread is returned immediately for other work.

Generally, I have tried to avoid the passing of null references (using an Optional wrapper object instead).
Also, I have tried to avoid using exceptions for logic flow, unless it is an edge case. Exceptions are relatively expensive to instantiate in Java.

Groovy, Spock and Cukes were chosen for testing as they allow for easy testing with less boilerplate than other testing frameworks.


## A 'real-life' version of the tool

A 'real-world-usage' of this tool would require a few changes:

 - Drop the command line approach and refactor as a non blocking microservice with an API
 	- Assuming it is to be used in a high traffic environment
 	- Receive requests from non-blocking network messages, or 'fire and forget' API calls. 
 	- Add a persistence layer to the processing queues (to buffer load and protect against duplicate requests or outages)
 	- Allow for many instances of the service to run concurrently, with some form of round robin / load balancing of the incoming request calls
 	- For an instance, getting the optimal thread pool size to get the best throughput for {downloading, resizing} on a target machine would require more robust testing
 	- Persist binary files to a decoupled hosting service that supports direct delivery of content without having to proxy traffic though this service. AWS S3 or similar, etc. (perhaps via a CDN?)
 	
 - In the HTML download and parser logic
 	- Several websites tested using this tool use JavaScript to build and dynamically render HTML pages - in particular image content
 	- The current approach of processing static HTML code for image content is not completely effective for this reason. Many images/UI elements are not displayed on popular websites until JS has run.
 	- A better (but more computationally expensive) approach would be to attempt to evaluate and fully render a page as part of the application, and then inspect the results. 
 	 	- This could necessitate some form of fakery in a spider - spoofing a popular User-Agent and user cookies in calls, etc.
 	 	- It would also necessitate a consideration of security - as JS & CSS would be executed 

 - In the "Download" logic
 	- Use of ETAG and File content type to better check for changes to a remote copy of an image
 	- Where there are multiple images to download from the same remote HTTP server, HTTP pipelining of the requests would reduce repeated connection handshaking for each image
 		- Some testing would be required to discover the optimal number of threads Vs 'queue size' of images being pipelined for each downloading thread
 		- Would require a graceful reversion to previous behaviour if the remote HTTP server did not support pipelining 
 	
 - In the "Resizer" logic
 	- As discussed in an above section, the dropping of a pure Java approach to image formatting and resizing.
 	- The use of an installed, and operating system specific, tool to perform these functions in native code via wrapper calls
 	
 - General logging, retries, results reporting, settings
 	- For the most part, the current version just logs errors
 		- for example, it does not retry downloads that failed due to simple timeouts, etc. 
 	- A full tool might consider persisting results (positive or negative) to some sort of results / retry store.
 	- The application.properties file has settings for items such as HTTP timeout, and maximum file size. 
 		- The current values for these are not based on testing and are just rule of thumb values
 
 
## Misc notes

- Local HTML files might have relative URLs to IMG content. The tool will not respond to this - it will fail at downloading these files. If I had more time I would correct this. 
 
- There is a ~40MB maximum image size. This seemed sensible. 
 
- Load testing - there is no load testing :). This is a combination of lack of time and the "single use" nature of this version of the tool. A microservice based version of the tool would require extensive load testing 

- The single acceptance test is also far, far too light and brittle. This is due to running out of time.
	 - it i had more time i would serve files from a local HTTP server running as part of the acceptance test. Also there would be many different file formats and IMG URL formats.

- Any query contents on the end of an image URL will be ignored in this version of the tool 
	 - for example the following image URLs will be regarded as the same: 
	 - http://example.com/1.jpg
	 - http://example.com/1.jpg?blahblahblah
	 - This is a design assumption, but may be invalid! The web is a messy place and it is possible that a query string can be used for authentication, or to differentiate images on a web host

 
