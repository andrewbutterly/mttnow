# language: en
Feature: ImageDownloader
	If HTML files are parsed
	IMG references from them are extacted and the files downloaded
	
Scenario: download of images from hosted file
	When The sample test HTML file is passed in with output folder "./build/acceptance-test"
	Then There should be 7 files created in output folder "./build/acceptance-test"
	Then The output folder "./build/acceptance-test" should be cleaned
	