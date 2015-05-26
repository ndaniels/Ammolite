package edu.mit.csail.ammolite.search;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;

import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;

public class SDFWritingResultHandler implements IResultHandler {
    
    List<SearchMatch> matches = null;
    String resultFolder;
    
    public SDFWritingResultHandler(String resultFolder){
        this.resultFolder = resultFolder;
        this.matches = new LinkedList<SearchMatch>();
    }

    @Override
    public boolean recordingStructures() {
        return true;
    }

    @Override
    public void handleCoarse(SearchMatch result) {
        // Do nothing

    }

    @Override
    public void handleFine(SearchMatch result) {
        matches.add(result);

    }

    @Override
    public void finishOneQuery() {
        PrintWriter writer;
        IAtomContainer query = matches.get(0).getQuery();
        if(resultFolder.charAt(resultFolder.length() - 1) != File.separatorChar){
            resultFolder = resultFolder + File.separator;
        }
        String queryIDStr = MolUtils.getUnknownOrID(query).toString();
        if( queryIDStr.equals("UNKNOWN_ID")){
            queryIDStr += "_" + query.hashCode();
        }
        String queryFolder = resultFolder + queryIDStr + File.separator;

        try {
            File queryFolderFile = new File(queryFolder);
            queryFolderFile.mkdirs();
            
            String simpleName = queryFolder + "result-table.csv";
            writer = new PrintWriter(simpleName, "UTF-8");
        
        for(SearchMatch match: matches){
            // Write to simple result file
            writer.print( MolUtils.getPubID( match.query));
            writer.print(", ");
            writer.print( MolUtils.getPubID( match.getTarget()));
            writer.print(", ");
            writer.print(MolUtils.getAtomCountNoHydrogen(match.query));
            writer.print(", ");
            writer.print(MolUtils.getAtomCountNoHydrogen( match.getTarget()));
            writer.print(", ");
            writer.print(match.getOverlap());
            writer.print(", ");
            writer.print(MCSUtils.tanimotoCoeff(match.getOverlap(), match.query, match.getTarget()));
            writer.println();
            
            // Write all <match>.sdf
            String matchFilename = queryFolder + MolUtils.getUnknownOrID( match.getTarget()).toString() + ".sdf";
            FileWriter fw = new FileWriter(matchFilename, true);
            SDFWriter sdfWriter = new SDFWriter(fw);
            sdfWriter.write(match.getMCS());
            sdfWriter.write(match.getTarget());
            sdfWriter.close();
            fw.close();
            
        }
        // Write query.sdf
        String queryFilename = queryFolder + "query.sdf";
        FileWriter fw = new FileWriter(queryFilename, true);
        SDFWriter sdfWriter = new SDFWriter(fw);
        sdfWriter.write(query);
        sdfWriter.close();
        fw.close();
        
        
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (CDKException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
