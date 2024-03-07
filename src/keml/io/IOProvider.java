/**
 * 
 */
package keml.io;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import keml.Conversation;
import keml.KemlPackage;
import keml.io.graphml.GraphML2KEML;

/**
 * 
 */
public class IOProvider {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws Exception {

		String currentPath = new java.io.File(".").getCanonicalPath();
		System.out.println("Current dir:" + currentPath);
		GraphML2KEML myReader = new GraphML2KEML();
		Conversation conv = myReader.readFromPath("../../graphs/objective3-2-2v4.graphml");
		//  write to file		
		Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
		ResourceSetImpl resSet = new ResourceSetImpl();
		resSet.getPackageRegistry().put(KemlPackage.eNS_URI, KemlPackage.eINSTANCE);
		resSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("keml", new XMIResourceFactoryImpl());
		
		Resource resource = resSet.createResource(URI.createFileURI("../../graphs/objective3-2-2v4.keml"));
		resource.getContents().add(conv);

		resource.save(null);
		
	}

}
