package algo.transit.repositories;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class CSVRepository {
    // Reads a CSV file and returns a list of string arrays
    public static List<String[]> readCSV(Path filePath) {
        try (CSVReader reader = new CSVReader(new BufferedReader(new FileReader(filePath.toString()), 8192 * 16))) {
            List<String[]> data = reader.readAll();
            data.removeFirst();
            return data;
        } catch (IOException | CsvException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            return null;
        }
    }

    // Writes a list of String arrays to a CSV file
//     public void writeCSV(String filePath, List<String[]> data) {
//         try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
//             writer.writeAll(data);
//         } catch (IOException e) {
//             System.err.println("Error writing to CSV file: " + e.getMessage());
//         }
//     }
}
