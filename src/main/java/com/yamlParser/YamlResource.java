package com.yamlParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

public class YamlResource extends ResourceImpl implements Handler{
	
	
	
	protected Stack<Object> Stack = new Stack<Object>();
	protected Stack<Object> mapStack = new Stack<Object>();
	protected HashMap<String, EClass> eClassCache = new HashMap<String, EClass>();
	protected HashMap<EClass, List<EClass>> allSubtypesCache = new HashMap<EClass, List<EClass>>();
	
	
	public static void main(String args[]) throws IOException{
//		ResourceSet metamodelResourceSet = new ResourceSetImpl();
//		metamodelResourceSet.getPackageRegistry().put(EcorePackage.eINSTANCE.getNsURI(), EcorePackage.eINSTANCE);
//		metamodelResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
//		Resource metamodelResource = metamodelResourceSet.createResource(URI.createFileURI(new File("model/messaging.ecore").getAbsolutePath()));
//		metamodelResource.load(null);
//		
//		EPackage metamodel = (EPackage) metamodelResource.getContents().get(0);
//		System.out.println(metamodel.toString());
//		EObject object = metamodel.eContents().get(0);
//		System.out.println(object.toString());

		
		//Yaml part
		ResourceSet modelResourceSet = new ResourceSetImpl();
		modelResourceSet.getPackageRegistry().put(EcorePackage.eINSTANCE.getNsURI(), EcorePackage.eINSTANCE);
		modelResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new YamlResourceFactory());
		Resource modelResource = modelResourceSet.createResource(URI.createFileURI(new File("model/messaging.yaml").getAbsolutePath()));
		modelResource.load(null);
		
//		EObject eobject = modelResource.getContents().get(0);
//		System.out.println(eobject);
	}
	
	public YamlResource(URI uri) {
		super(uri);
	}
	
	@Override
	protected void doLoad(InputStream inputStream, Map<?, ?> options)
			 {
		try {
			doLoadImpl(inputStream, options);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	public void doLoadImpl(InputStream inputStream, Map<?,?> options){
		new YAMLParser(inputStream,this).doLoad();
	}
	
	
	@Override
	public void startTagElement(Object element, String option) {
		EObject eObject = null;
		EClass eClass = null;
		if(option.equals(ConstantStringObject.OPTION_LIST_PARSE) || option.equals(ConstantStringObject.OPTIN_MAP_PARSE)){
			//at the top level
			
			
			
			
			
			
			if(Stack.isEmpty() || Stack.peek() == null){
				eClass = eClassForName(element.toString());
				if(eClass!=null){
					eObject = eClass.getEPackage().getEFactoryInstance().create(eClass);
					getContents().add(eObject);
					// no setAttribute
//					setAttribute(eObject, element.toString()); 
				}else{
					// add warnings
				}
				
			}else{  
				//this is attribute list
				Object peekObject = Stack.peek();
				if(peekObject == null){
					Stack.push(null);
					//add warnings
					return ;
				}else if(peekObject instanceof EReferenceSlot){
					EReferenceSlot containmentSlot = (EReferenceSlot)peekObject;
					eClass = (EClass) eNamedElementForName(element.toString(), getAllSubtypes(containmentSlot.getEReference().getEReferenceType()));
					if(eClass != null){
						eObject = eClass.getEPackage().getEFactoryInstance().create(eClass);
						containmentSlot.newValue(eObject);
						Stack.push(eObject);
//						setAttribute(eObject,element.toString());
						
					}else{
						Stack.push(null);
						//add warnings
					}
				}else if (peekObject instanceof EObject){
					EObject parent = (EObject)peekObject;
					EAttribute eAttribute = (EAttribute)eNamedElementForName(element.toString(), parent.eClass().getEAllAttributes());
					if(eAttribute != null){
//						parent.
					}
				}
				
			}
			Stack.push(eObject);
		}
		System.out.println("In startDocument : " + element.toString());
	}

	@Override
	public void endElement(Object element, String option) {
		
	}

	@Override
	public void startValueElement(Object key, Object value, String option) {
		
		System.out.println("In startElement ::: /" + key + ": " + value);
//		System.out.println();
	}
	
	protected EClass eClassForName(String name){
		EClass eClass = eClassCache.get(name);
		if(eClass == null){
			eClass = (EClass)eNamedElementForName(name, getAllConcreteEClasses());
			eClassCache.put(name, eClass);
		}
		return eClass;
	}
	
	protected ENamedElement eNamedElementForName(String name, Collection<? extends ENamedElement> candidates){
		
		ENamedElement eNamedElement = eNamedElementForName(name, candidates,false);
		if(eNamedElement == null){
			System.out.println("should develop fuzzy function");
		}
		return eNamedElement;
		
	}
	
	protected ENamedElement eNamedElementForName(String name, Collection<? extends ENamedElement> candidates, boolean fuzzy){
		if(!fuzzy){
			for(ENamedElement candidate: candidates){
				if(candidate.getName().equalsIgnoreCase(name))
					return candidate;
			}
		}else{
			//fuzzy function
			return null;
		}
		return null;
	}
	
	protected List<EClass> getAllSubtypes(EClass eClass){
		List<EClass> allSubtypes = allSubtypesCache.get(eClass);
		if (allSubtypes == null) {
			allSubtypes = new ArrayList<EClass>();
			for (EClass candidate : getAllConcreteEClasses()) {
				if (candidate.getEAllSuperTypes().contains(eClass)) {
					allSubtypes.add(candidate);
				}
			}
			if (!eClass.isAbstract()) allSubtypes.add(eClass);
			allSubtypesCache.put(eClass, allSubtypes);
		}
		return allSubtypes;
	}
	
	protected List<EClass> getAllConcreteEClasses(){
		List<EClass> eClasses = new ArrayList<EClass>();
		Iterator<Object> it = getResourceSet().getPackageRegistry().values().iterator();
		while(it.hasNext()){
			EPackage ePackage = (EPackage)it.next();
			for(EClassifier eClassifier: ePackage.getEClassifiers()){
				if(eClassifier instanceof EClass && !((EClass) eClassifier).isAbstract()){
					eClasses.add((EClass) eClassifier);
				}
			}
		}
		
		return eClasses;
	}
	
	protected int getStackSize(){
		return Stack.size();
	}
	
	// pop stack content except the first instance
	protected void clearStack(){
		for(int i = 0 ; i < Stack.size()-1; i++){
			Stack.pop();
		}
	}
	
	public void setAttribute(EObject eObject, String element){
		
	}
}
