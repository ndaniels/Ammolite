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
	CONNECTION_3
}
