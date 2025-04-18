package algo.transports.repositories;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class CSVRepository {
    // This class is responsible for reading and writing CSV files
    // It will use the OpenCSV library to handle CSV files
    // The class will have methods to read and write CSV files

    // Method to read a CSV file and return a list of string arrays
    public static List<String[]> readCSV(Path filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath.toString()))) {
            List<String[]> data = reader.readAll();
            data.remove(0); // Remove the header row
            return data;
        } catch (IOException | CsvException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            return null;
        }
    }

    // Method to write a list of String arrays to a CSV file
//     public void writeCSV(String filePath, List<String[]> data) {
//         try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
//             writer.writeAll(data);
//         } catch (IOException e) {
//             System.err.println("Error writing to CSV file: " + e.getMessage());
//         }
//     }
}
