package tables;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;

import model.FileTable;
import model.Row;
import model.Table;

public class BinaryTable implements FileTable {
	private static final Path base = Paths.get("db", "tables");
	private final Path rootDir;

	public BinaryTable(String name, List<String> columns) {
		try {
			rootDir = base.resolve(name); // given
			Files.createDirectories(rootDir); // given
			var data = rootDir.resolve("data");
			Files.createDirectories(data);
			var metadata = rootDir.resolve("metadata");
			Files.createDirectories(metadata);

			// save the columns to a file
			
			var columnsPath = metadata.resolve("columns");
			
			if ((!Files.exists(columnsPath))) {
			Files.createFile(columnsPath);
			}
			
			var sizePath = metadata.resolve("size");
			
			if ((!Files.exists(sizePath))) {
			Files.createFile(sizePath);
			}
			
			var fingerprintPath = metadata.resolve("fingerprint");
			
			if ((!Files.exists(fingerprintPath))) {
				Files.createFile(fingerprintPath);
				}
			
			var outputStream = new DataOutputStream(Files.newOutputStream(columnsPath));
			    // write the number of columns
			    outputStream.writeInt(columns.size());
			    
			    // write each column name as a  string
			    for (String column : columns) {
			        outputStream.writeUTF(column);
			    }
			    outputStream.close();
			} catch (IOException e) {
			    throw new IllegalStateException(e);
			}
	}

	public BinaryTable(String name) {
		rootDir = base.resolve(name);
		if (Files.notExists(rootDir))
			throw new IllegalArgumentException("Missing table: " + name);
	}

	@Override
	public void clear() {
		try {
			// resolve data
			var data = rootDir.resolve("data"); // given
			
			Files.walk(data) // given
				.skip(1) // given
				.sorted(Comparator.reverseOrder()) // given				
				.forEach(path -> path.toFile().delete()); // given
		
			// resolve metadata
			var metadata = rootDir.resolve("metadata");
			
			// create the size file and set to 0
			var sizePath = metadata.resolve("size");
			DataOutputStream dosSize = new DataOutputStream(new FileOutputStream(sizePath.toFile()));
				dosSize.writeInt(0);
				dosSize.close();

			// create the fingerprint file and set to 0
			var fingerprintFile = metadata.resolve("fingerprint");
			DataOutputStream dosFingerprint = new DataOutputStream(new FileOutputStream(fingerprintFile.toFile()));
	            dosFingerprint.writeInt(0);
	            dosFingerprint.close();

			} catch (IOException e) {
				throw new IllegalStateException(e);
		}
	}

	private String digestFunction(String key) {
		try {
			var sha1 = MessageDigest.getInstance("SHA-1"); // given
			sha1.update("Zachary Haufe".getBytes()); // given
			sha1.update(key.getBytes()); // given
			
			byte[] digestBytes = sha1.digest();
			
			return HexFormat.of().formatHex(digestBytes);

		} 
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}	
		
	}
	
	// Private helper method that turns 012abc to data/01/2abc
	private String digestToPath(String digest) {
		return String.format("%s/%s", digest.substring(0, 2), digest.substring(2));
	}
	
	// Private helper method that turns data/de/f789 to def789
	private String pathToDigest(String path) {
		String[] parts = path.split("/");
		return parts[1] + parts[2];
	}
	
	// Helper method to read a row from a file
	private Row readRowFromFile(Path filePath) {
	    List<Object> fields = new ArrayList<>();
	    
	    try {
	    	DataInputStream dis = new DataInputStream(Files.newInputStream(filePath));
	    	
	    	String key = dis.readUTF();
	    	int numFields = dis.readInt();
	    	for (int i = 0; i < numFields; i++) {
	        	String type = dis.readUTF();
	            switch (type) {
	                case "String":
	                    fields.add(dis.readUTF());
	                    break;
	                case "Integer":
	                    fields.add(dis.readInt());
	                    break;
	                case "Double":
	                    fields.add(dis.readDouble());
	                    break;
	                case "Boolean":
	                    fields.add(dis.readBoolean());
	                    break;	 
	                case "null":
	                	fields.add(null);
	                	break;
	                default:
	                    throw new IllegalStateException("Unknown field type: " + type);
	            }	       
	        }
	    	dis.close();
	        return new Row(key, fields);
	    } catch (IOException e) {
	        throw new IllegalStateException(e);
	    }
	    
	}
	
	// Helper method to write a row to a file
	private void writeRowToFile(Path filePath, String key, List<Object> fields) {
		try {
		DataOutputStream dos = new DataOutputStream(Files.newOutputStream(filePath));	
	    	dos.writeUTF(key);
	        dos.writeInt(fields.size());  // Write the number of fields
	        for (Object field : fields) {
	            if (field instanceof String) {
	                dos.writeUTF("String");
	                dos.writeUTF((String) field);
	            } else if (field instanceof Integer) {
	                dos.writeUTF("Integer");
	                dos.writeInt((Integer) field);
	            } else if (field instanceof Double) {
	                dos.writeUTF("Double");
	                dos.writeDouble((Double) field);
	            } else if (field instanceof Boolean) {
	                dos.writeUTF("Boolean");
	                dos.writeBoolean((Boolean) field);
	            } else {
	                dos.writeUTF("null");
	            }
	        }
	        dos.close();
	    } catch (IOException e) {
	        throw new IllegalStateException(e);
	   }
	}


	@Override
	public List<Object> put(String key, List<Object> fields) {
		// Resolve the data and metadata folder
		Path dataDir = rootDir.resolve("data");
		Path metadataDir = rootDir.resolve("metadata");
		int currentSize = size();
		
		// Degree guard condition (identical to previous modules)
		if (key == null || key.isEmpty()) {
	        throw new IllegalArgumentException("Key cannot be null or empty");
	    }
		if (fields.size() + 1 != degree()) {
	        throw new IllegalArgumentException("Field list size does not match the expected number of columns");
	    }
		
		// Digest key and resolve paths
		String digest = digestFunction(key);
		Path rowPath = dataDir.resolve(digestToPath(digest));
		Path fpPath = metadataDir.resolve("fingerprint");
		Path sizePath = metadataDir.resolve("size");
		
		// Ensure the parent directories exist for row, fingerprint, and size files
		try {
		    Files.createDirectories(rowPath.getParent());  
		    Files.createDirectories(fpPath.getParent());   
		    Files.createDirectories(sizePath.getParent()); 
		} catch (IOException e) {
		    throw new IllegalStateException(e);
		}
		
		// Initialize oldFingerprint and oldFields
		int oldFingerprint = 0;
		List<Object> oldFields = null;
		
		// Hit case
		if (Files.exists(rowPath)) { 
			// Read old row
			Row oldRow = readRowFromFile(rowPath);
			oldFields = oldRow.fields();
			// Get old row's fingerprint
			try {
				DataInputStream disFingerprint = new DataInputStream(Files.newInputStream(fpPath));
				oldFingerprint = disFingerprint.readInt();
				disFingerprint.close();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
			
			writeRowToFile(rowPath, key, fields);
			
			// Update Fingerprint
			int newFingerprint = oldFingerprint - oldRow.hashCode() + fields.hashCode();///////
			try {
				DataOutputStream dosFingerprint = new DataOutputStream(Files.newOutputStream(fpPath));
				dosFingerprint.writeInt(newFingerprint);
				dosFingerprint.close();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
			
		// Miss case
		} else {

	        writeRowToFile(rowPath, key, fields);
	        
	        // Updates size
	        int newSize = currentSize + 1;
	        try {
	        DataOutputStream dosSize = new DataOutputStream(Files.newOutputStream(sizePath));
	            dosSize.writeInt(newSize);
	            dosSize.close();
	        } catch (IOException e) {
	            throw new IllegalStateException(e);
	        }
			
	        // Updates fingerprint
	        int newFingerprint = fields.hashCode();////////
	        try {
		        DataOutputStream dosFingerprint = new DataOutputStream(Files.newOutputStream(fpPath));
		            dosFingerprint.writeInt(newFingerprint);
		            dosFingerprint.close();
		        } catch (IOException e) {
		            throw new IllegalStateException(e);
		        }   
	    }
	    return oldFields;
	}	

	@Override
	public List<Object> get(String key) {
		Path dataDir = rootDir.resolve("data");
		if (key == null || key.isEmpty()) {
			throw new IllegalArgumentException("Key cannot be null or empty");
		}
		
		String digest = digestFunction(key);
		
		Path rowPath = dataDir.resolve((digestToPath(digest)));
		
		if (Files.exists(rowPath)) {
			var fields = readRowFromFile(rowPath).fields();            
			return fields;
		
		}
		return null;
	}	
	
	@Override
	public List<Object> remove(String key) {
		if (key == null || key.isEmpty()) {
		    throw new IllegalArgumentException("Key cannot be null or empty");
		    }
		Path dataDir = rootDir.resolve("data");
	    Path metadataDir = rootDir.resolve("metadata");
	    
	    // Digest key and resolve paths
	    String digest = digestFunction(key);
	    Path rowPath = dataDir.resolve(digestToPath(digest));
	    Path fpPath = metadataDir.resolve("fingerprint");
	    Path sizePath = metadataDir.resolve("size");
	    
	    List<Object> oldFields = null;
	    int oldFingerprint = 0;
		
		if (Files.exists(rowPath)) {
			
			Row oldRow = readRowFromFile(rowPath);
			oldFields = oldRow.fields();
			
			try { 
				DataInputStream disFingerprint = new DataInputStream(Files.newInputStream(fpPath));
				oldFingerprint = disFingerprint.readInt();
				disFingerprint.close();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
			
			try { 
				Files.delete(rowPath);
				var rowParent = rowPath.getParent();
				if (Files.list(rowParent).count() == 0) {
					Files.delete(rowParent);
				}
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
			
			int newFingerprint = oldFingerprint - oldRow.hashCode();
			try (DataOutputStream dosFingerprint = new DataOutputStream(Files.newOutputStream(fpPath))) {
	            dosFingerprint.writeInt(newFingerprint);
	            dosFingerprint.close();
	        } catch (IOException e) {
	            throw new IllegalStateException(e);
	        }
			
			int currentSize = 0;
			try {
		        DataInputStream disSize = new DataInputStream(Files.newInputStream(sizePath));
		            currentSize = disSize.readInt();
		            disSize.close();
		        } catch (IOException e) {
		            throw new IllegalStateException(e);
		        }
			
			try {
				DataOutputStream dosSize = new DataOutputStream(Files.newOutputStream(sizePath));
				int newSize = currentSize - 1;
				dosSize.writeInt(newSize);
				dosSize.close();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		    
		} else {
		       return null;
		    }

		    return oldFields;
		}

	@Override
	public int degree() {
		return columns().size();
	}

	@Override
	public int size() {
		try {
			
			var metadataDir = rootDir.resolve("metadata");
			var sizePath = metadataDir.resolve("size");

			DataInputStream dis = new DataInputStream(new FileInputStream(sizePath.toFile()));
			int size = dis.readInt();
	        dis.close();
	        
	        return size;
			
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		
		
	}
	
	@Override
	public int hashCode() {
	    int fingerprint = 0;

	    for (Row row : this) {
	        fingerprint += row.hashCode();
	    }
	    return fingerprint;
	}
	

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Table &&
			this.hashCode() == obj.hashCode();
	}

	@Override
	public Iterator<Row> iterator() {
	    try {

	    var data = rootDir.resolve("data");
        return Files.walk(data)
                .filter(path -> Files.isRegularFile(path)) // Filter only regular files
                .map(path -> {
                	return readRowFromFile(path);
                
                })
                .iterator();
    } catch (IOException e) {
        throw new IllegalStateException(e);
    }
}

	@Override
	public String name() {
		return rootDir.getFileName().toString();
	}

	@Override
	public List<String> columns() {
		try {
	        // Resolve the columns file in the metadata directory
	        var columnsFile = rootDir.resolve("metadata").resolve("columns");

	        List<String> columns = new ArrayList<>();
			var inputStream = new DataInputStream(Files.newInputStream(columnsFile));
		    // read the number of columns
		    var columnsSize = inputStream.readInt();
		    
		    // read each column name as a  string
		    for (int i = 0; i < columnsSize; i++) {
		      columns.add(inputStream.readUTF());  
		    }
		    inputStream.close();
		    return columns;
		} catch (IOException e) {
		    throw new IllegalStateException("Error reading columns file", e);
		}
		
		}

	@Override
	public String toString() {
		return toPrettyString();
	}
}