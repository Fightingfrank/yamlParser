package com.yamlParser;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryImpl;

public class YamlResourceFactory extends ResourceFactoryImpl{
	
	@Override
	public Resource createResource(URI uri){
		return new YamlResource(uri);
	}
}
