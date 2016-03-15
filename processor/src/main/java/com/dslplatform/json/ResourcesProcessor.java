package com.dslplatform.json;

import org.apache.commons.io.IOUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedOptions({"dsljson.namespace"})
public class ResourcesProcessor extends AbstractProcessor {

	private static final String DSL_EXTENSION = ".dsl";

	private Types typeUtils;
	private Elements elementUtils;
	private Messager messager;
	private Filer filer;

	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		this.elementUtils = processingEnv.getElementUtils();
		this.typeUtils = processingEnv.getTypeUtils();
		this.messager = processingEnv.getMessager();
		this.filer = processingEnv.getFiler();
    }

    @Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		// TODO: See how to do this only once
		
		printme("Processing resources...");
        if(!roundEnv.processingOver()) {
			for(File dslFile : getDslFilesInClasspath()) {
				process(dslFile);
			}
		}
		else printme("Processing already over");
		return false;
	}

	private void process(File file) {
        try {
			String content = IOUtils.toString(new FileInputStream(file));
			messager.printMessage(Diagnostic.Kind.NOTE, content);
        } catch(IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
	}

	private List<InputStream> dslFilesInResources() {
		List<InputStream> result = new ArrayList<InputStream>();
		try {
			FileObject fo = filer.getResource(StandardLocation.CLASS_PATH, "", "dsl/aValue.dsl");
			printme(fo.getName());
			result.add(fo.openInputStream());
		} catch(IOException e) {
			printme("Failed fetching resources: " + e.getMessage());
			e.printStackTrace();
		}
		printme("Rezultat: " + result);
		return result;
	}

	private List<File> getDslFilesInClasspath() {
		List<File> result = new ArrayList<File>();

		try {
			URL dslRoot = ResourcesProcessor.class.getClassLoader().getResource("dsl/");
			printme("Resource retrieved: " + dslRoot);

			File resFile = new File(dslRoot.toURI());
			result.addAll(dslFilesInSubtree(resFile));
		} catch(URISyntaxException e) {
			// TODO: Show error;
		}

		return result;
	}

	private static List<File> dslFilesInSubtree(File root) {
		List<File> result = new ArrayList<File>();
		if(root == null) return result;

		if(root.isDirectory()) {
			for(File child : root.listFiles()) {
				if(child.isDirectory()) result.addAll(dslFilesInSubtree(child));
				if(isDslFile(child)) result.add(child);
			}
		} else {
			if(isDslFile(root)) result.add(root);
		}


		return result;
	}

	private static boolean isDslFile(File file) {
		if(file != null) {
			if(file.getName().toLowerCase().endsWith(DSL_EXTENSION))
				return true;
		}
		return false;
	}

	private void printme(String string) {
		//System.out.println(string);
		messager.printMessage(Diagnostic.Kind.NOTE, string);
	}
}
