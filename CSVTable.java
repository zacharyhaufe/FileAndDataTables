package tables;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import model.DataTable;
import model.FileTable;
import model.Row;

public class CSVTable implements FileTable {

//	private String name;
//	private List<String> columns;
//	private int size;
//	private int fingerprint;
//	private int degree;
	
	private static final Path base = Paths.get("db", "tables");
	private final Path file;
	
	public CSVTable(String name, List<String> columns) {
		try {
			// this.columns = new ArrayList<>(columns);
			// Create base directories
			Files.createDirectories(base);
			// Create new file named after table name
			file = base.resolve(name + ".csv");
			if (Files.notExists(file)) {
			Files.createFile(file);
			}
			// Write column names in header of file
			try (BufferedWriter writer = Files.newBufferedWriter(file)) {
			String header = String.join(",", columns);
			writer.write(header);
			writer.newLine();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
		
	public CSVTable(String name) {
		// Reopens existing file if possible
		try {
			Files.createDirectories(base);
			file = base.resolve(name + ".csv");
			if (Files.notExists(file)) {
				throw new RuntimeException();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
		
	@Override
	public void clear() {	
		try {
			
	        List<String> lines = Files.readAllLines(file);
	        
	        if (!lines.isEmpty()) {
	            // Write only the header line back to the file, clearing all data rows
	            Files.write(file, Collections.singletonList(lines.get(0)), StandardCharsets.UTF_8);
	        }

	    } catch (IOException e) {
	        throw new RuntimeException("Failed to clear the table", e);
	    	}
		}

	@Override
	public List<Object> put(String key, List<Object> fields) {
			try {
				// Guard condition for invalid key
				if (key == null || key.isEmpty()) {
					throw new IllegalArgumentException("Invalid Key");
				}
				// Guard condition for fields that are too wide or too narrow
				if ((fields.size() + 1) != degree()) {
					throw new IllegalArgumentException("Amount of fields do not match the degree.");
				}
			
				// read all lines from the CSV file into a list of lines
				List<String> lines = Files.readAllLines(file);
				
				List<Object> oldFields = null; // Store old fields in case of hit
				
			
				// make a new Row object with key and fields (as in previous modules)
				var newRow = new Row(key, fields);
				
				// for each line number in list excluding the header line:
				for (int i = 1; i < lines.size(); i++) { 
					String line = lines.get(i); // Get current CSV line
					
					// Decode the CSV line
					var decodedRow = decode(line); // Decode it into a list of fields
				
					// If the key (0th element) of the decoded row matches the given key
					// it is a hit.
					if (decodedRow.key().equals(key)) {
						// Capture old fields 
						oldFields = decodedRow.fields();
						
						// Remove old row from list of lines
						lines.remove(i);
						
						// Insert row using move to front heuristic
						lines.add(1, encode(newRow));
						
						Files.write(file, lines);
						
						return oldFields;
					}
				}
				// Miss case: no matching key so just add new row at end of file
				lines.add(encode(newRow));
				
				Files.write(file, lines);
				
				return null;	
			 
				
				} catch (IOException e) {
					throw new RuntimeException(e);
					}
			}
	
	@Override
	public List<Object> get(String key) {		
		try {
			// read all lines from the CSV file into a list of lines
			List<String> lines = Files.readAllLines(file);
			
			for (int i = 1; i < lines.size(); i++) {
				String line = lines.get(i); // Get the current CSV line
				Row decodedRow = decode(line); // Decode it into a list of fields
				
				
				if (key.equals(decodedRow.key())) { // hit
					String hitRow = lines.remove(i);
					lines.add(1, hitRow); // move to front
					
					Files.write(file, lines);
					
					return decodedRow.fields();	
				}
			}
			return null;			
		}
		catch (IOException e) {
			throw new RuntimeException(e);
			
		}	
	}

	@Override
	public List<Object> remove(String key) {
		try {
	        List<String> lines = Files.readAllLines(file);

	        for (int i = 1; i < lines.size(); i++) {
	            String line = lines.get(i); // Get the current CSV line
	            Row decodedRow = decode(line); // Decode it into a list of fields

	            // Check if the key matches
	            if (key.equals(decodedRow.key())) {
	                // Hit: Remove the row
	                lines.remove(i); // Remove the matching row from the list
	                
	        

	                // Write the updated list of lines back to the file
	                Files.write(file, lines);
	                
	                return decodedRow.fields(); // Return fields without key
	            }
	        }

	        // If no matching key was found, fall through (miss)
	        return null;

	    } catch (IOException e) {
	        throw new RuntimeException(e);
	       
	    }
	}

	@Override
	public int degree() {
		List<String> columnNames = columns();
		if (columnNames != null) {
			return columnNames.size();
		}
		return 0;
	}

	@Override
	public int size() {
		try {
		List<String> lines = Files.readAllLines(file);
		
		return lines.size() - 1; // don't count headers
		
		} catch (IOException e) {
			throw new RuntimeException(e);
			
		}
	}

	@Override
	public int hashCode() {
		int fingerprint = 0;
		
		try {
			List<String> lines = Files.readAllLines(file);
			
			// read all lines into list
			 for (int i = 1; i < lines.size(); i++) {
		            String line = lines.get(i); // Get the current CSV line
		            Row decodedRow = decode(line); // Decode it into a list of fields
		            
		            fingerprint += decodedRow.hashCode();
			 }
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		return fingerprint;
	}

	@Override
	public boolean equals(Object obj) {
		// Check if the object is the same instance
	    if (this == obj) {
	        return true;
	    }

	    // Check if obj is null or not an instance of CSVTable
	    if (obj == null || !(obj instanceof CSVTable)) {
	        return false;
	    }

	    // Cast obj to CSVTable for next step
	    CSVTable otherTable = (CSVTable) obj;

	    // Check if fingerprint matches 
	    return this.hashCode() == otherTable.hashCode();
	}

	@Override
	public Iterator<Row> iterator() {
		
		List<Row> rows = new ArrayList<>();
		
	    try {
	        // Read all lines from the CSV file into a list of strings
	        List<String> lines = Files.readAllLines(file);
	      
	        // Iterate through the rows (excluding the header)
	        for (int i = 1; i < lines.size(); i++) {
	            String line = lines.get(i); // Get the current CSV line
	            Row decodedFields = decode(line); // Decode the CSV line into a list of fields

	            // Extract the key from the first field
	            String key = decodedFields.key().toString();

	            // Extract the remaining fields (excluding the key)
	            List<Object> fields = decodedFields.fields();

	            // Create a new Row object with the key and fields
	            Row row = new Row(key, fields);

	            // Add the Row object to the list of rows
	            rows.add(row);
	            
	  
	        }

	    } catch (IOException e) {
	        throw new RuntimeException(e);
	    }

	    // Return the iterator of the list
	    return rows.iterator();
	}

	@Override
	public String name() {
		String name = file.getFileName().toString();
		if (name.endsWith(".csv")) {
			return name.substring(0, name.length() - 4);
		}
		return name;
	}

	@Override
	public List<String> columns() {
		try {
			List<String> lines = Files.readAllLines(file);
			
			// if the lines are not empty
			if (!lines.isEmpty()) {
			List<String> columnNames = new ArrayList<>();
			String topLine = lines.get(0);
			columnNames = Arrays.asList(topLine.split(","));
			
			return columnNames;	
		}
		
		return Collections.emptyList();
		
		} catch (IOException e) {
		throw new RuntimeException(e);
	}
	}

	@Override
	public String toString() {
		return toPrettyString();
	}
	
	// Helper method to encode a row to a CSV String
	public String encode(Row row) {
		
		// Build string of encoded key and fields
		StringBuilder encodedRow = new StringBuilder();
		
		String key = row.key();
		encodedRow.append("\"").append(key.replace("\"", "\"\"")).append("\""); // Enclose key in quotes and escape quotes
		List<Object> fields = row.fields();
		
		// For each field in fields
		for (Object field : fields) {
			encodedRow.append(","); // comma delimiter
			
			if (field == null) {
				// Encode null as empty string
				encodedRow.append("null");
			} else if (field instanceof String) {
				// Encode strings in double quotes and escape internal double quotes
				encodedRow.append("\"").append(field.toString().replace("\"", "\"\"")).append("\""); // Enclose Strings in quotes and escape any quotes
			} else if (field instanceof Boolean || field instanceof Integer || field instanceof Double) {
				// Encode boolean, integer, and doubles as is.
				encodedRow.append(field.toString());
			}  else {
				// Any other types are enclosed in quotes
				encodedRow.append("\"").append(field.toString()).append("\"");
			}
		}
		
		return encodedRow.toString();
	}
	
	// Helper method to decode a row from a CSV string
	public Row decode(String csvRow) {
		String[] fields = csvRow.split(","); // split fields on commas
		
		String key = fields[0].trim(); // 0th field is the key
		if (key.startsWith("\"") && key.endsWith("\"")) {
			key = key.substring(1, key.length() -1).replace("\"\"", "\""); // Remove quotes and escape quotes
		}
		List<Object> decodedFields = new ArrayList<>();
		
		for (int i = 1; i < fields.length; i++) {
			String field = fields[i].trim(); // trim whitespace
			
			if (field.equals("null")) {
				// Empty string represents null
				decodedFields.add(null); 
			} else if (field.startsWith("\"") && field.endsWith("\"")) {
				// Decode string field
				String decodedString = field.substring(1, field.length() - 1).replace("\"\"", "\"");
				decodedFields.add(decodedString);
			} else if (field.equalsIgnoreCase("true") || field.equalsIgnoreCase("false")) {
				// Decode boolean
				decodedFields.add(Boolean.parseBoolean(field));
			} else if (field.contains(".")) {
	            // Decode floating point field
	            decodedFields.add(Double.parseDouble(field));
	        } else {
	            // Decode integer field
	            decodedFields.add(Integer.parseInt(field));
	        }
	    }
		return new Row(key, decodedFields);
	}
	
public static CSVTable factory(DataTable hashTable) {
	try {
	Path file = CSVTable.base.resolve(hashTable.name() + ".csv");

	Files.createDirectories(CSVTable.base);
	
	// make csv file
	if (!Files.exists(file)) {
		Files.createFile(file);
		}
	
	// instance of hashTable and csvTable
	CSVTable csvTable = new CSVTable(hashTable.name(), hashTable.columns());

	// use for each 
		for (var row : hashTable) {
			csvTable.put(row.key(), row.fields());
		}
	return csvTable;

	} catch (IOException e) {
		throw new RuntimeException(e);
	}
}
	
}
