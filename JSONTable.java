package tables;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import model.FileTable;
import model.Row;
import model.Table;

public class JSONTable implements FileTable {
	private static final Path base = Paths.get("db", "tables");
	private final Path jsonFile;

	private static final ObjectMapper helper = new ObjectMapper();
	private final ObjectNode tree;

	public JSONTable(String name, List<String> columns) {
		try {
			Files.createDirectories(base);

			jsonFile = base.resolve(name + ".json");
			if (Files.notExists(jsonFile))
				Files.createFile(jsonFile);

			tree = helper.createObjectNode();

			// above code has been provided
			
			var metadata = helper.createObjectNode();
			metadata.put("name",  name); // Store table name
			metadata.set("columns", helper.valueToTree(columns));
			
			// Add metadata to root of JSON tree
			tree.set("metadata", metadata);
			
			// Initialize empty rows array
			tree.set("rows",  helper.createArrayNode());
			
			flush();
			
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public JSONTable(String name) {
		try {
			jsonFile = base.resolve(name + ".json");

			if (Files.notExists(jsonFile))
				throw new IllegalArgumentException("Missing table: " + name);

			tree = (ObjectNode) helper.readTree(jsonFile.toFile());
		}
		// above code has been provided
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void clear() {
		tree.set("rows", helper.createArrayNode());
	    flush();
	}

	@Override
	public void flush() {
		try {
			helper.writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), tree);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		} 
	}
	// above code has been provided
	
	@Override
	public List<Object> put(String key, List<Object> fields) {
		if (key == null || key.isEmpty()) {
			throw new IllegalArgumentException("Key cannot be null or empty");
		}
		if ((fields.size() + 1)!= degree()) {
	        throw new IllegalArgumentException("Amount of fields do not match the table's degree");
	    }
		// access the rows array from the JSON tree
		var rows = (ArrayNode) tree.path("rows");
		
		// check for hit
		for (int i = 0; i < rows.size(); i++) {
			var row = (ObjectNode) rows.get(i);
			if (row.path("key").asText().equals(key)) {
				// extract the old fields
				var oldFieldsNode = (ArrayNode) row.path("fields");
				var oldFields = convertFields(oldFieldsNode); // use convertFields method to handle field types
				
				// update existing row with new fields
				row.set("fields", helper.valueToTree(fields));
				
				flush();
				
				return oldFields;
			}
		}
		// misses: create new row
		var newRow = helper.createObjectNode();
		newRow.put("key", key);
	    newRow.set("fields", helper.valueToTree(fields));
	    rows.add(newRow);
	    
	    flush();
	    
	    return null;
	}
	

	@Override
	public List<Object> get(String key) {
		if (key == null || key.isEmpty()) {
	        throw new IllegalArgumentException("Key cannot be null or empty");
	    }
		
		var rows = (ArrayNode) tree.path("rows");
		
		// search for row with matching key
		for (int i = 0; i < rows.size(); i++) {
			var row = (ObjectNode) rows.get(i);
			if (row.path("key").asText().equals(key)) {
				// extract the old fields
				var fieldsNode = (ArrayNode) row.path("fields");
				var fields = convertFields(fieldsNode); // use convertFields method to handle field types
				
				return fields; // hit condition
			}
		}
		return null; // miss condition
	}

	@Override
	public List<Object> remove(String key) {
		if (key == null || key.isEmpty()) {
	        throw new IllegalArgumentException("Key cannot be null or empty");
	    }
		
		var rows = (ArrayNode) tree.path("rows");
		
		// search for row with matching key
		for (int i = 0; i < rows.size(); i++) {
			var row = (ObjectNode) rows.get(i);
			if (row.path("key").asText().equals(key)) {
				// get fields from the row
				var fieldsNode = (ArrayNode) row.path("fields");
				var fields = convertFields(fieldsNode); // use convertFields method to handle field types
				
	            rows.remove(i);
	            flush();
	            
	            return fields; // hit condition
	        }
	    }
	    return null; //miss condition
	}
		
	@Override
	public int degree() {
		return columns().size();
	}

	@Override
	public int size() {
		return tree.path("rows").size();
	}

	@Override
	public int hashCode() {
		int fingerprint = 0;
		
		try {
			var rows = (ArrayNode) tree.path("rows");
		
			for (var jsonRow : rows) {
				String key = jsonRow.path("key").asText();
				var fieldsNode = (ArrayNode) jsonRow.path("fields");
		        List<Object> fields = convertFields(fieldsNode);
		        
	            // Create a new row and get its hash code
	            Row row = new Row(key, fields);
	            fingerprint += row.hashCode();
	        }
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }

	    return fingerprint;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Table &&
			this.hashCode() == obj.hashCode();
	}
	// above code has been provided
	
	@Override
	public Iterator<Row> iterator() {
		List<Row> rowsList = new ArrayList<>();
		
		// access rows from JSON tree
	    var rows = (ArrayNode) tree.path("rows");
		
	    for (int i = 0; i < rows.size(); i++) {
	        var jsonRow = (ObjectNode) rows.get(i);

	        String key = jsonRow.path("key").asText();


	        ArrayNode fieldsNode = (ArrayNode) jsonRow.path("fields");
	        List<Object> fields = convertFields(fieldsNode); // use convertFields method for handling field types

	        // Create a new row and add it to the list
	        Row row = new Row(key, fields);
	        
	        rowsList.add(row);
	    }
	    // Return the iterator
	    return rowsList.iterator();
	}
	

	@Override
	public String name() {
		return tree.path("metadata").path("name").asText();
	}

	@Override
	public List<String> columns() {
		var columnsNode = tree.path("metadata").path("columns");
	
		List<String> columnNames = new ArrayList<>();
	    for (int i = 0; i < columnsNode.size(); i++) {
	    	columnNames.add(columnsNode.get(i).asText());
	    }
	    return columnNames;
	}

	@Override
	public String toString() {
		return toPrettyString();
	}
	
	private List<Object> convertFields(ArrayNode fieldsNode) {
		List<Object> fields = new ArrayList<>();
	    for (int j = 0; j < fieldsNode.size(); j++) {
	        var fieldNode = fieldsNode.get(j);

	        if (fieldNode.isBoolean()) {
	            fields.add(fieldNode.asBoolean());
	        } else if (fieldNode.isInt()) {
	            fields.add(fieldNode.asInt());
	        } else if (fieldNode.isDouble()) {
	            fields.add(fieldNode.asDouble());
	        } else if (fieldNode.isTextual()) {
	            fields.add(fieldNode.asText());
	        } else {
	            // Handle null or unexpected types
	            fields.add(null);
	        }
	    }
	    return fields;
	}
}