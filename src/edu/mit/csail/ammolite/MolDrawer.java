package edu.mit.csail.ammolite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.renderer.visitor.IDrawVisitor;
import org.openscience.cdk.renderer.ChemModelRenderer;
import org.openscience.cdk.renderer.IRenderer;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;

import edu.mit.csail.ammolite.compression.CyclicStruct;
import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.utils.SDFUtils;

public class MolDrawer {
	
	public static void draw(String sdfFile){
		List<IAtomContainer> mols = SDFUtils.parseSDF(sdfFile);
		for(int i=0; i<mols.size(); i++){
			IAtomContainer mol = mols.get(i);
			String id = (String) mol.getProperty("PUBCHEM_COMPOUND_CID");
			String name = sdfFile.replace(".sdf", "_" + id);
			draw(mol, name);
		}
	}
	
	public static void drawAsStruct(String sdfFile){
		List<IAtomContainer> mols = SDFUtils.parseSDF(sdfFile);
		for(int i=0; i<mols.size(); i++){
			IAtomContainer mol = mols.get(i);
			MolStruct struct = new CyclicStruct(mol);
			String id = (String) mol.getProperty("PUBCHEM_COMPOUND_CID");
			String name = sdfFile.replace(".sdf", "_struct_" + id);
			draw(struct, name);
		}
	}
	
	public static void draw(IAtomContainer mol, String imageName){
		int WIDTH = 500;
	    int HEIGHT = 500;
	    
	    // the draw area and the image should be the same size
	    Rectangle drawArea = new Rectangle(WIDTH, HEIGHT);
	    Image image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
	    
	    
	    // generators make the image elements
	    List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();
	    generators.add(new BasicSceneGenerator());
	    generators.add(new BasicBondGenerator());
	    generators.add(new BasicAtomGenerator());
	    
	    // the renderer needs to have a toolkit-specific font manager 
	    AtomContainerRenderer renderer = new AtomContainerRenderer(generators, new AWTFontManager());
	    
	    // the call to 'setup' only needs to be done on the first paint
	    renderer.setup(mol, drawArea);
	    
	    // paint the background
	    Graphics2D g2 = (Graphics2D)image.getGraphics();
	    g2.setColor(Color.WHITE);
	    g2.fillRect(0, 0, WIDTH, HEIGHT);
	    
	    
	  IDrawVisitor drawVisitor = new AWTDrawVisitor(g2);
	    
	    // the paint method also needs a toolkit-specific renderer
	    renderer.paint(mol, drawVisitor);
	    try {
			ImageIO.write((RenderedImage)image, "PNG", new File(imageName+".png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
