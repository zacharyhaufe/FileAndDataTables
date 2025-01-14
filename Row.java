package model;

import java.util.List;

public record Row(String key, List<Object> fields) {
	
	@Override
	public int hashCode() {
		return key.hashCode() ^ fields.hashCode();
	}
}