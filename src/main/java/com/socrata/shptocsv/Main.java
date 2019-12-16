package com.socrata.shptocsv;

import com.opencsv.CSVWriter;
import org.geotools.data.DataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Basically, this shows you how you could use GeoTools to read in a shapefile and output it to CSV in a format which
 * could then be easily upserted to a dataset on Socrata. In this example we mainly read in the `.shp`. I also include
 * an example of how you could read in the `.dbf` file but to be honest I'm not entirely familiar with the shapefile
 * format so I'm not sure what you'd find in a `.dbf` file that wouldn't be in the `.shp`.
**/
public class Main {

    public static void main(String[] args) throws IOException {
        String shpFilePath;
        String dbfFilePath;
        if (args.length == 1) {
            shpFilePath = args[0];
            run(shpFilePath);
        }

        if (args.length > 1) {
            shpFilePath = args[0];
            dbfFilePath = args[1];
            run(shpFilePath,dbfFilePath);
        }

        else {
            throw new RuntimeException("Call with path to SHP file as single argument");
        }
        run(shpFilePath, dbfFilePath);
    }

    private static void run(final String shpFilePath) throws IOException {
        LinkedList<LinkedList<String>> entries = extractRecords(shpFilePath);
        writeRecords(entries);
    }

    private static void run(final String shpFilePath, final String dbfFilePath) throws IOException {
            LinkedList<LinkedList<String>> entries = extractRecords(shpFilePath);
            readDBF(dbfFilePath);
            writeRecords(entries);
    }

    private static LinkedList<LinkedList<String>> extractRecords(final String filePath) {
        LinkedList<LinkedList<String>> rows = new LinkedList<>();

        try {
            final DataStore dataStore = openDataStore(filePath);
            final String[] typeNames = dataStore.getTypeNames();

            SimpleFeatureCollection features = dataStore.getFeatureSource(typeNames[0]).getFeatures();

            ReprojectingFeatureCollection rfc = new ReprojectingFeatureCollection(features, DefaultGeographicCRS.WGS84);

            try (FeatureIterator<SimpleFeature> iterator = rfc.features()) {
                LinkedList<String> headers = new LinkedList<>();
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    LinkedList<String> row = new LinkedList<>();
                    LinkedList<String> keys = new LinkedList<>();
                    for (Property p : feature.getProperties()) {
                        keys.add(p.getName().toString());
                        if (p.getValue() == null) {
                            row.add("");
                        } else {
                            // 'Wed May 16 00:00:00 PDT 2018'
                            try {
                                Date date1 = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy").parse(p.getValue().toString());
                                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
                                row.add(df.format(date1));
                            } catch(Exception e) {
                                row.add(p.getValue().toString());
                            }
                        }
                    }
                    if (headers.size() == 0) {
                        headers = keys;
                    }
                    else {
                        if (!keys.equals(headers)) {
                            throw new RuntimeException("Keys vary in number, order or content");
                        }
                    }
                    rows.add(row);
                }
                System.out.println("Features: " + rows.size());
                rows.addFirst(headers);
                return rows;
//                return String.join("\n", rows);
            } finally {
                dataStore.dispose();
            }
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
        return rows;
    }

    private static void writeRecords(LinkedList<LinkedList<String>> records) throws IOException {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter("shapefile.csv"));
            Iterator<LinkedList<String>> iterator = records.iterator();
            while(iterator.hasNext()) {
                LinkedList<String> currentList = iterator.next();
                int listLength = currentList.size();
                String[] currentArray = currentList.toArray(new String[listLength]);
                writer.writeNext(currentArray);
            }
            writer.close();
        } catch(IOException error) {
            throw error;
        }

    }

    private static DataStore openDataStore(final String filePath) throws IOException {
        final File file = new File(filePath);
        return new ShapefileDataStore(file.toURI().toURL());
    }

    private static void readDBF(final String filePath) throws IOException {
        // To be honest I'm not sure what a DBF contains that the SHP file doesn't. But here's an example of how you
        // could read in the values using geotools.
        FileInputStream fis = new FileInputStream( filePath );
        DbaseFileReader dbfReader =  new DbaseFileReader(fis.getChannel(),
                false, StandardCharsets.ISO_8859_1);

        int currRow = 0;
        int limit = 10;
        DbaseFileHeader dbfHeaders = dbfReader.getHeader();
        System.out.println("Here are the headers: " + dbfReader.getHeader().toString());
        while ( dbfReader.hasNext() && currRow < limit ){
            final Object[] fields = dbfReader.readEntry();
            int fieldLength = fields.length;
            System.out.println("DBF row length is: " + fieldLength);
            for (int i = 0; i < fields.length; i++) {
                System.out.println("DBF field " + dbfHeaders.getFieldName(i) + "value is: " + fields[i].toString());
            }
            currRow ++;
        }

        dbfReader.close();
        fis.close();
    }
}
