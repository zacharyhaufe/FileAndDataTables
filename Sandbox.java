package apps;

import java.util.Arrays;
import java.util.List;

import model.Table;
import tables.BinaryTable;
import tables.HashTable;
import tables.JSONTable;
import tables.SymbolTable;

public class Sandbox {
	public static void main(String[] args) {
		
		// Example table as a symbol table
		  Table example1 = new SymbolTable("here", List.of("name", "type", "value", "comment"));
		  example1.clear();
		  example1.put("B", Arrays.asList("string", "World!", null));
		  example1.put("A", Arrays.asList("string", "Hello,", null));
		  example1.put("x", List.of("number", 1, "first coordinate"));
		  example1.put("y", List.of("number", 20, "second coordinate"));
		  example1.put("z", List.of("number", 300, "third coordinate"));
		  example1.put("k", List.of("boolean", true, "lowercase name"));
		  example1.put("K", List.of("boolean", false, "uppercase name"));
		  System.out.println("Example 1:");
		  System.out.println(example1);
		  
		 //  Golf table as a binary table
		 Table golfTable = new BinaryTable("Golf Clubs", List.of("Variable", "Type of Club", "Brand", "Distance (yards)", "Rating"));
		 golfTable.clear();
		 golfTable.put("Dgj5", Arrays.asList("Driver", "Taylor Made", "230", "9"));
		 golfTable.put("hfn4", Arrays.asList("3 Hybrid", "Taylor Made", "180", "8"));
		 golfTable.put("sfn5", Arrays.asList("7 iron", "Callaway", "140", "7"));
		 golfTable.put("p   ", Arrays.asList("Putter", "Taylor Made", "20", "8"));
		 golfTable.put("flop", Arrays.asList("5 iron", "Callaway", "150", "7"));
		 golfTable.put("diomcP", Arrays.asList("Pitching Wedge", "Callaway", "120", "7"));
		 golfTable.put("Afnf", Arrays.asList("Sand Wedge", "Taylor Made", "100", "10"));
		 golfTable.put("fdifj", Arrays.asList("9 iron", "Callaway", "100", "6"));
		 golfTable.put("efofm0", Arrays.asList("8 iron", "Callaway", "110", "7"));
		 System.out.println("Golf table: ");
		 System.out.print(golfTable);

		//  Video game table as a hash table
		 Table videoGameTable = new HashTable("Video Games", List.of("Variable", "Game Name", "System", "Type", "How good was I", "Rating"));
		 videoGameTable.clear();
		 videoGameTable.put("Gofldf f", Arrays.asList("Grand Theft Auto 5", "PS4 and PC", "Free World", "7", "9"));
		 videoGameTable.put("Vf njf", Arrays.asList("Valorant", "PC", "FPS", "8", "8"));
		 videoGameTable.put("Ffmff ", Arrays.asList("Fortnite", "PS4 and PC", "Battle Royale", "8", "10"));
		 videoGameTable.put("ufjf", Arrays.asList("Ultimate Alliance", "PS3", "Story", "5", "7"));
		 videoGameTable.put("rf", Arrays.asList("Retro Bowl", "iPhone", "Arcade", "8", "4"));
		 videoGameTable.put("B", Arrays.asList("Call of Duty: Black Ops II", "PS3", "FPS", "8", "10"));
		 videoGameTable.put("nfinef", Arrays.asList("NCAA Football 2012", "Wii", "Football", "7", "10"));
		 videoGameTable.put("Nfekmefe", Arrays.asList("NCAA Football 2024", "PS5", "Football", "7", "7"));
		 videoGameTable.put("g", Arrays.asList("Call of Duty: Ghosts", "PS3", "FPS", "6", "7"));
		 videoGameTable.put("K", Arrays.asList("NBA 2K20", "PS4", "Basketball", "9", "10"));
		 System.out.println("Video Game table: ");
		 System.out.println(videoGameTable);

		//	Song table as a JSON table
		 Table songTable = new JSONTable("Songs", List.of("Variable", "Song Title", "Artist", "Release Year", "Genre", "Personal Rating"));
		 songTable.clear();
		 songTable.put("J", Arrays.asList("Jungle", "Drake", "2015", "Rap", "8"));
		 songTable.put("Lffnv", Arrays.asList("Address Your Letters", "The Backseat Lovers", "2018", "Alternative", "8"));
		 songTable.put("h", Arrays.asList("Headlines", "Drake", "2011", "Rap", "9"));
		 songTable.put("Cco33", Arrays.asList("Cigarette Daydreams", "Cage the Elephant", "2013", "Alternative", "7"));
		 songTable.put("I", Arrays.asList("Ivy", "Frank Ocean", "2016", "R&B", "8"));
		 songTable.put("sff;", Arrays.asList("Silver Lining", "Mt. Joy", "2017", "Alternative", "7"));
		 songTable.put("Ufmkc", Arrays.asList("Use Somebody", "Kings of Leon", "2008", "Rock", "8"));
		 songTable.put("l", Arrays.asList("Language", "Brent Faiyaz", "2017", "R&B", "8"));
		 songTable.put("y", Arrays.asList("Love Yourz", "J Cole", "2014", "Rap", "9"));
		 songTable.put("P", Arrays.asList("Pool House", "The Backseat Lovers", "2018", "Alternative", "10"));
		 songTable.put("S", Arrays.asList("Somethin' Stupid", "Frank Sinatra", "1967", "Traditional Pop", "8"));
		 System.out.println("Song Table: ");
		 System.out.println(songTable);


		}
	}

	
