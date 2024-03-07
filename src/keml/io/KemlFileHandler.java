package keml.io;

import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import keml.Conversation;
import keml.KemlPackage;

public class KemlFileHandler {
	
	private ResourceSetImpl resS;
	
	public KemlFileHandler() {
		resS = initializeKemlXMIResourceSet();
	}

	private ResourceSetImpl initializeKemlXMIResourceSet() {
		Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
		ResourceSetImpl resSet = new ResourceSetImpl();
		resSet.getPackageRegistry().put(KemlPackage.eNS_URI, KemlPackage.eINSTANCE);
		resSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("keml", new XMIResourceFactoryImpl());
		return resSet;
	}
	
	public void saveKeml(Conversation conv, String path) {
		Resource resource = resS.createResource(URI.createFileURI(path));
		resource.getContents().add(conv);

		try {
			resource.save(null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
