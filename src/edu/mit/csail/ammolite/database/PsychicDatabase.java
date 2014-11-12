package edu.mit.csail.ammolite.database;

import java.util.List;
import java.util.Map;

import edu.mit.csail.ammolite.utils.PubchemID;
import edu.mit.csail.ammolite.utils.StructID;

public class PsychicDatabase extends GenericStructDatabase {
    
    Map<PubchemID, Integer> sizeMap;

    public PsychicDatabase(String name, String version, String compression,
                    boolean organized, Map<StructID, List<PubchemID>> idMap, 
                    Map<PubchemID, Integer> sizeMap, List<String> structFiles, 
                                                        List<String> sourceFiles) {
        super(name, version, compression, organized, idMap, structFiles, sourceFiles);
        this.sizeMap = sizeMap;
        
    }
    
    public int atomCount(PubchemID id){
        return sizeMap.get(id);
    }

}
