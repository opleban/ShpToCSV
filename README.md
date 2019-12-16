## Shapefile to CSV Example using GeoTools

## Summary
This is a simple example of how you can use the GeoTools library to take a Shapefile and turn it into a CSV file. Please note that this example only transforms the contents of the `.shp` file into a csv. It will read and output the first 10 values of the `.dbf` file but doesn't do anything with them. 

## Get Started
Make sure you have maven installed on your system.
* Run `mvn clean compile assembly:single` to generate a jar file with all dependencies.
* Run `java -jar target/ShpToCsv-1.0-SNAPSHOT-jar-with-dependencies.jar <PATH/TO/SHAPEFILE.shp> <PATH/TO/SHAPEFILE.dbf>`
