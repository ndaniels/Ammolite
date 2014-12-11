package edu.mit.csail.ammolite.database;

import java.io.Serializable;

public enum CompressionType implements Serializable {
	CYCLIC,
	RING,
	BASIC,
	LABELED,
	WEIGHTED,
	NONE,
}
