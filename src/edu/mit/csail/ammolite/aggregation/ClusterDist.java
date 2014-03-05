package edu.mit.csail.ammolite.aggregation;

public class ClusterDist {
	public Cluster a;
	public Cluster b;
	public double d;
	public long time;
	
	public ClusterDist(Cluster _a, Cluster _b, double _d, long _time){
		a = _a;
		b = _b;
		d = _d;
		time = _time;
	}
}
