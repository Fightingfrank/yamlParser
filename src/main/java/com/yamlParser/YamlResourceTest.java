package com.yamlParser;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.Test;

public class YamlResourceTest {

	@Test
	public void testGetAllSubtypes() {
		
	}
	
	@Test
	public void testGetAllConcreteEClasses(){
		ResourceSet modelResourceSet = new ResourceSetImpl();
		YamlResource resource = new YamlResource(URI.createFileURI(new File("model/messaging.yaml").getAbsolutePath()));
		List<EClass> eClasses= resource.getAllConcreteEClasses();
		for(EClass sd : eClasses){
			System.out.println(sd.getName());
		}
	}

}
