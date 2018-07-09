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
import java.util.Stack;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

public class YamlResource extends ResourceImpl implements Handler{
	
	
	protected Stack<Object> stack = new Stack<Object>();
	protected Stack<Object> mapStack = new Stack<Object>();
	//String in HashMap store Class name to figure EClass
	protected HashMap<String, EClass> eClassCache = new HashMap<String, EClass>();
	
	protected HashMap<EClass, List<EClass>> allSubtypesCache = new HashMap<EClass, List<EClass>>();
	protected List<UnresolvedReference> unresolvedReferences = new ArrayList<UnresolvedReference>();
	
	public static void main(String args[]) throws IOException{
		
		ResourceSet metamodelResourceSet = new ResourceSetImpl();
		metamodelResourceSet.getPackageRegistry().put(EcorePackage.eINSTANCE.getNsURI(), EcorePackage.eINSTANCE);
		metamodelResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		Resource metamodelResource = metamodelResourceSet.createResource(URI.createFileURI(new File("model/messaging.ecore").getAbsolutePath()));
		metamodelResource.load(null);
		EPackage metamodel = (EPackage)metamodelResource.getContents().get(0);
		
//		Yaml part
		ResourceSet modelResourceSet = new ResourceSetImpl();
		modelResourceSet.getPackageRegistry().put(metamodel.getNsURI(), metamodel);
		modelResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new YamlResourceFactory());
		Resource modelResource = modelResourceSet.createResource(URI.createFileURI(new File("model/messaging.yaml").getAbsolutePath()));
		modelResource.load(null);
		
		System.out.println(modelResource.getContents().get(0));
		EObject rootObject = modelResource.getContents().get(0);
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
			eClass = eClassForName(element.toString());
		
			if(eClass != null){
				System.out.println(eClass.getName());
				eObject = eClass.getEPackage().getEFactoryInstance().create(eClass);
				if(stack.isEmpty()){  // root element
					getContents().add(eObject);
				}else{ 
					//size() == 0, create EReferenceSlot
					Object parent = stack.peek();
					if(eObject.eClass().getEAllContainments().size() == 0 && !(parent instanceof EReferenceSlot)){
						System.out.println(eObject.eClass().getName());
						EReference containment = (EReference) eNamedElementForName(element.toString(),((EObject)parent).eClass().getEAllContainments());
						System.out.println(containment.getName());
						EReferenceSlot containmentSlot = new EReferenceSlot(containment,(EObject) parent);
						stack.push(containmentSlot);
						return ;
					}else if(parent instanceof EReferenceSlot){
						EReferenceSlot containmentSlot = (EReferenceSlot)parent;
						containmentSlot.newValue(eObject);
					}
					else{
						// EReference object, but has attribute
						setContainmentObject(eObject,element.toString());
					}
				}
				stack.push(eObject);
			}else{
				// warnings
			}
		}
	}
		
	// pop stack content except the first instance
	@Override
	public void clearStack(){
		for(int i = 0 ; i < stack.size()-1; i++){
			stack.pop();
		}
	}
	@Override
	public void endElement(Object element, String option) {
		
	}

	@SuppressWarnings("unused")
	@Override
	public void startValueElement(String key, String value, String option) {
		EObject parent = (EObject) stack.peek();
		List<EStructuralFeature> eStructuralFeatures = getCandidateStructuralFeaturesForAttribute(parent.eClass()); 
		EStructuralFeature sf = findEStructuralFeatureMatch(eStructuralFeatures, key);
		if(parent == null || sf == null)
			//add warnings
			return ;
		if(sf instanceof EAttribute){
			setEAttributeValue(parent, (EAttribute) sf, key, value);
		}else if(sf instanceof EReference){
				//add warnings
			EReference eReference = (EReference) sf;
			System.out.println(sf.toString());
			System.out.println(sf.getName());
			unresolvedReferences.add(new UnresolvedReference(parent, eReference, key, value));
		}
	
	}
	
	protected EStructuralFeature findEStructuralFeatureMatch(List<EStructuralFeature> eStructuralFeatures, String name){
		for(EStructuralFeature sf : eStructuralFeatures){
			if(sf.getName().equals(name))
				return sf;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	protected void setEAttributeValue(EObject eObject, EAttribute eAttribute, String attributeName, String value){
		Object eValue = getEValue(eAttribute, attributeName, value);
		System.out.println(eAttribute.getName() + " ,  type: " + eAttribute.getEType().toString());
		if(eValue == null)
			return;
		if(eAttribute.isMany()){
			((List<Object>) eObject.eGet(eAttribute)).add(eValue);
		}else{
			System.out.println(eValue.toString());
			eObject.eSet(eAttribute, eValue);
		}
	}
	protected Object getEValue(EAttribute eAttribute, String attributeName, String value){
		try{
			return eAttribute.getEAttributeType().getEPackage().getEFactoryInstance().createFromString(eAttribute.getEAttributeType(),value);
		}catch(Exception exception){
			exception.printStackTrace();
			//add warnings
			return null;
		}
	}
	
	protected EClass eClassForName(String name){
		EClass eClass = eClassCache.get(name);
		if(eClass == null){
			eClass = (EClass)eNamedElementForName(name, getAllConcreteEClasses());
			if(eClass == null && !stack.isEmpty()){
				EObject eObject= (EObject)stack.peek();
				eClass = eClassForName(name,eObject.eClass());
			}
			eClassCache.put(name, eClass);
		}
		return eClass;
	}
	
	protected EClass eClassForName(String name, EClass parentClass){
		for(EStructuralFeature sf: parentClass.getEAllStructuralFeatures()){
				if(sf.getName().equals(name)){
					return (EClass) sf.getEType();
				}
			
		}
		return null;
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
	
//	protected List<EClass> getAllSubtypes(EClass eClass){
//		List<EClass> allSubtypes = allSubtypesCache.get(eClass);
//		if (allSubtypes == null) {
//			allSubtypes = new ArrayList<EClass>();
//			for (EClass candidate : getAllConcreteEClasses()) {
//				if (candidate.getEAllSuperTypes().contains(eClass)) {
//					allSubtypes.add(candidate);
//				}
//			}
//			if (!eClass.isAbstract()) allSubtypes.add(eClass);
//			allSubtypesCache.put(eClass, allSubtypes);
//		}
//		return allSubtypes;
//	}
	
	
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

	
	
	
	protected List<EStructuralFeature> getCandidateStructuralFeaturesForAttribute(EClass eClass){
		List<EStructuralFeature> eStructuralFeatures = new ArrayList<EStructuralFeature>();
		for(EStructuralFeature sf : eClass.getEAllStructuralFeatures()){
				eStructuralFeatures.add(sf);
		}
		return eStructuralFeatures;
	}
	
	
	
	protected void setContainmentObject(EObject eObject, String name){
		//setAttribute中的EObject参数就是现在的parent,现在的eObject是根据那个name和value确定的
		EObject parent = (EObject)stack.peek();
//		if()
		System.out.println(parent.toString());
		System.out.println(eObject.eClass());
		
		if(parent instanceof EReferenceSlot){
			
		}else{
			EReference containment = (EReference)findEStructuralFeatureMatch(getCandidateStructuralFeaturesForAttribute(parent.eClass()), name);
			if(containment != null){
				if(containment.isMany()){
					System.out.println("Contaiment class:" + containment.getEType().toString());
					((List<EObject>) parent.eGet(containment)).add(eObject);
				}else{
					parent.eSet(containment, eObject);
				}
			}
		}		
		
	}	
}
