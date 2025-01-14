package tables;

import java.util.Iterator;
import java.util.List;

import model.DataTable;
import model.Row;
import model.Table;

public class SymbolTable implements DataTable {
	
	// Store private data members
	private String name;
	private List<String> columns;
	private Row[] rows;
	private int size;
	private int fingerprint;

	
	public SymbolTable(String name, List<String> columns) {
		this.name = name;
		this.columns = columns;
		this.rows = new Row[52];
	}

	@Override
	public void clear() {
		rows = new Row[52];
		size = 0;
		fingerprint = 0;
	}

	@Override
	public List<Object> put(String key, List<Object> fields) {
		// Guard conditions throwing exceptions if row is 
		// too wide/narrow or if it is not a character.
		if (key.length() != 1 || !Character.isLetter(key.charAt(0))) {
			throw new IllegalArgumentException("Key must be a single letter.");
		}
		
		// Guard condition to check if the list has the right amount of fields
		if ( 1+fields.size() != degree()) {
			throw new IllegalArgumentException("Amount of fields do not match the degree");
		}

		// Map the key to an index
		int index = Character.isUpperCase(key.charAt(0)) ? key.charAt(0) - 'A' : key.charAt(0) - 'a' + 26;
		
		// Check if the position is already filled
		Row oldRow = rows[index];
		Row newRow = new Row(key, fields);

		// If there was already a row, update the fingerprint 
		// by removing the old one and adding the new one
		if (oldRow != null) { // Hit
			fingerprint -= oldRow.hashCode();
			rows[index] = newRow;
			fingerprint += newRow.hashCode();
			return oldRow.fields();
		} else { // Miss
		// Otherwise, just add the new row
		rows[index] = newRow;
		fingerprint += newRow.hashCode();
		size++;
		}
		return null; // Return null if there was no old row
	}

	@Override
	public List<Object> get(String key) {
		// Guard conditions
		if (key.length() != 1 || !Character.isLetter(key.charAt(0))) {
			throw new IllegalArgumentException("Key must be a single letter.");
		}
		
		// Map key to an index
		int index = Character.isUpperCase(key.charAt(0)) ? key.charAt(0) - 'A' : key.charAt(0) - 'a' + 26;

		Row row = rows[index];
		return row != null ? row.fields() : null;
	}

	@Override
	public List<Object> remove(String key) {
		// Guard conditions
		if (key.length() != 1 || !Character.isLetter(key.charAt(0))) {
			throw new IllegalArgumentException("Key must be a single letter.");
		}
		// Find index by character arithmetic
		int index = Character.isUpperCase(key.charAt(0)) ? key.charAt(0) - 'A' : key.charAt(0) - 'a' + 26;
	

		Row row = rows[index];
		if (row != null) {
			fingerprint -= row.hashCode();
			rows[index] = null;
			size--;
			return row.fields();
		}
		
		return null;
	}

	@Override
	public int degree() {
		return (columns.size()); // 1 indicates the the key
	}

	@Override
	public int size() {
		return size; // # of rows (Amortized)
	}

	@Override
	public int capacity() {
		return rows.length; // Total number of possible rows (52)
	}

	@Override
	public int hashCode() {
		return fingerprint;
	}

	
	// use instanceof
	// 
	@Override
	public boolean equals(Object obj) {
		if ((obj instanceof Table)) {
		if (this.hashCode() == obj.hashCode())
			return true;
		}
		return false;
		
		// if (obj == null) {
		// 	return false;
		// }
		// Object otherTable = (Object) obj;

    	 // Compare hash codes (fingerprints)
    	// if (this.hashCode() == otherTable.hashCode()) {
        // 	return true;
   	 	// }
    	// return false;
}
	@Override
	public Iterator<Row> iterator() {
		return new Iterator<>() {
			private int currentIndex = 0;

			@Override
			public boolean hasNext() {
				while (currentIndex < rows.length && rows[currentIndex] == null) {
					currentIndex++;
				}
				return currentIndex < rows.length;
			}

			@Override
			public Row next() {
				if (!hasNext()) {
					throw new IllegalStateException("No more elements");
				}
				return rows[currentIndex++];
			}
		};
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public List<String> columns() {
		return this.columns;
	}

	@Override
	public String toString() {
		return toPrettyString();
	}
}
