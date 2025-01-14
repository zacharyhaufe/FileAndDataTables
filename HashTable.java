package tables;

import java.util.Iterator;
import java.util.List;
import model.DataTable;
import model.Row;
import model.Table;

public class HashTable implements DataTable {

	// Private variables
	private String name;
	private List<String> columns;
	private Row[] row;
	private int size;
	private int fingerprint;

	// recommended not required
	private static final Row SENTINEL = new Row(null, null);

	// fill out with primes congruent to 3 mod 4
	private static final int[] PRIMES = {7, 19, 43, 83, 167, 331, 683, 991, 1999};

	public HashTable(String name, List<String> columns) {
        this.name = name;
        this.columns = columns;
		this.row = new Row[PRIMES[0]];
    }

	@Override
	public void clear() {
		size = 0;
		row = new Row[PRIMES[0]];
		fingerprint = 0;
	}

	private int hashFunction(String key) {
		// Constants for 32-bit FNV hash
		final int FNV_OFFSET_BASIS = 0x811c9dc5; // Decimal Value: 2166136261
		final int FNV_PRIME = 0x01000193; // Decimal Value: 16777619
		
		// Salt the key
		key += "Haufe";
		
		int hash = FNV_OFFSET_BASIS;
	
		for (int i = 0; i < key.length(); i++) {
			
			// XOR the hash with the current current character of key
			hash ^= key.charAt(i);
			
			// Multiply by the FNV prime
			hash *= FNV_PRIME;          
		}
	
		// hash mod capacity
		// Use math.floorMod method 
		return Math.floorMod(hash, capacity());
	}

	@Override
	public List<Object> put(String key, List<Object> fields) {
		
		// Guard condition for an invalid key
		if (key == null) {
			throw new IllegalArgumentException("Key cannot be null.");
		}
		
		// Guard condition for fields that are too wide or too narrow
		if (fields.size() + 1 != degree()) {
			throw new IllegalArgumentException("Amount of fields do not match the degree.");
		}
		
		// Trigger rehash if the load factor becomes greater than 75%
		if (loadFactor() >= 0.75) {
			rehash();
		}

		// Hash the key

		int hKey = hashFunction(key);
		int capacity = capacity();
		int sentinelIndex = -1; // track first sentinel if encountered

		for (int j = 0; j < capacity; j++) {
		int sign = (j % 2 == 0) ? 1 : -1; // Even numbers go positive, odd numbers go negative
		int index = Math.floorMod(hKey + sign * (int) Math.pow(j, 2), capacity);

//		if (index < 0) { 
//			index += capacity; // Ensure the index is positive by adjusting negative mod results
//		}
		
		if (row[index] == null) {
			// If a sentinel was found earlier, use its index to insert the new row
			if (sentinelIndex != -1) {
				index = sentinelIndex;
			}
			row[index] = new Row(key, fields);
			fingerprint += row[index].hashCode();
			size++;
			return null;
		
		} else if (row[index] == SENTINEL && sentinelIndex == -1) {
			sentinelIndex = index; // Save sentinel index for possible reuse
		
		} else if (row[index].key() != null && row[index].key().equals(key)) {
            List<Object> oldFields = row[index].fields();
            fingerprint -= row[index].hashCode();
            row[index] = new Row(key, fields);
            fingerprint += row[index].hashCode();
            return oldFields;
		}
	}
		

    // If we get here, an error occurred
    throw new IllegalStateException("Unexpected fall-through: no available slot found");
}

		// ONCE WE HAVE PUT, COPY & PASTE AND ADJUST TO GET AND REMOVE

	@Override
	public List<Object> get(String key) {
		
		// Guard condition for an invalid key
		if (key == null) {
			throw new IllegalArgumentException("Key cannot be null or blank.");
		}
		
		// Hash the key
		int hKey = hashFunction(key);
    	int capacity = capacity();

    	for (int j = 0; j < capacity; j++) {
    		int sign = (j % 2 == 0) ? 1 : -1; // Even numbers go positive, odd numbers go negative
    		int index = (hKey + sign * (int) Math.pow(j, 2)) % capacity; // let i be the result of (h +- j^2) modded by the capacity)

        if (index < 0) {
        	index += capacity; // Ensure positive index if mod result is negative
        }

        // If we encounter a miss, return null
        if (row[index] == null) {
            return null;
        
        // Skip over deleted entries
        } else if (row[index] == SENTINEL) {
        	continue;
        // Hit: key found, return fields
        } else if (row[index].key().equals(key)) { 
            return row[index].fields();
        }
    }

    return null;  // Key not found after probing
}

	@Override
	public List<Object> remove(String key) {
		
		// Guard condition for an invalid key
		if (key == null) {
			throw new IllegalArgumentException("Key cannot be null or empty");
		}
		
		// Hash the key
		int h = hashFunction(key);
		int capacity = capacity();
	
		for (int j = 0; j < capacity; j++) {
			int sign = (j % 2 == 0) ? 1 : -1;
			int index = (h + sign * (int) Math.pow(j, 2)) % capacity; // let i be the result of (h +- j^2) modded by the capacity)
	
			if (index < 0) {
				index += capacity; // Ensure positive index
			}
	
			if (row[index] == null) {  // Miss: empty slot means key is not present
				return null; // Key not found
			// Skip over deleted entries
			} else if (row[index] == SENTINEL) { 
				continue;
			} else if (row[index] != SENTINEL && row[index].key().equals(key)) {  // Hit: key found, remove it
		          List<Object> oldFields = row[index].fields();
		          fingerprint -= row[index].hashCode();
		          row[index] = SENTINEL;  // Mark this slot as deleted with the sentinel
		          size--;  // Decrease size correctly
		          return oldFields;  // Return the removed fields
			}
		}
	
		return null;  // Key not found after probing
	}

	@Override
	public int degree() {
		return columns.size();
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public int capacity() {
		return row.length;
	}

	@Override
	public int hashCode() {
		return fingerprint;
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj instanceof Table)) {
			if (this.hashCode() == obj.hashCode())
				return true;
			}
			return false;
	}

	@Override
	public Iterator<Row> iterator() {
		return new Iterator<>() {
			private int currentIndex = 0;

			@Override
			public boolean hasNext() {
				while (currentIndex < row.length && (row[currentIndex] == null || row[currentIndex] == SENTINEL)) {
					currentIndex++;
				}
				return currentIndex < row.length;
			}

			@Override
			public Row next() {
				if (!hasNext()) {
					throw new IllegalStateException("No more elements");
				}
				return row[currentIndex++];
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

	private void rehash() {
		// let backup/copy reference = old array reference
		Row[] oldTable = row;
		
		int newSize = getNextPrime(capacity());
		// reassign table array reference = new larger empty array
		row = new Row[newSize];
		
		// reinitialize size / fingerprint
		size = 0;
		fingerprint = 0;
		// for each index in the backup/copy
		for (Row r : oldTable) { // for each index in the old table
			if (r != null && r != SENTINEL) { // if it isn't null or a sentinel
				put(r.key(), r.fields()); // put the old elements in the new table
			}
		}
	}
	
	// Helper method to get next prime from the array of PRIMES
	private int getNextPrime(int currentCapacity) {
		for (int i = 0; i < PRIMES.length; i++) {
			if (PRIMES[i] > currentCapacity) {
				return PRIMES[i];
			}
		}
		return 0; // Return 0 if above loop unexpectedly fails
	}
 
}
		
