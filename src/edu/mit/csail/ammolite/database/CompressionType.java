package edu.mit.csail.ammolite.database;

import java.io.Serializable;

public enum CompressionType implements Serializable {
	CYCLIC,
	RING,
	BASIC,
	BINARY_LABELED,
	FULLY_LABELED,
	WEIGHTED,
	NONE,
	
	CONNECTION_2,
	CONNECTION_3,
	CONNECTION_4,
	CONNECTION_5,
	CONNECTION_6,
	
	OVERLAP_5,
	OVERLAP_6,
	OVERLAP_4,
	OVERLAP_7,
	OVERLAP_8,
	OVERLAP_9,
	
	BINARY_OVERLAP_5,
	BINARY_OVERLAP_6,
	BINARY_OVERLAP_4,
	
	
}
