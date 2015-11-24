package com.dslplatform.json;

import com.dslplatform.compiler.client.AnnotationCompiler;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

@SupportedAnnotationTypes({"com.dslplatform.json.CompiledJson"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedOptions({"dsljson.namespace", "dsljson.timeApi"})
public class CompiledJsonProcessor extends AbstractProcessor {

	private static final Map<String, String> SupportedTypes;

	static {
		SupportedTypes = new HashMap<String, String>();
		SupportedTypes.put("int", "int");
		SupportedTypes.put("long", "long");
		SupportedTypes.put("float", "float");
		SupportedTypes.put("double", "double");
		SupportedTypes.put("boolean", "bool");
		SupportedTypes.put("java.lang.String", "string?");
		SupportedTypes.put("java.lang.Integer", "int?");
		SupportedTypes.put("java.lang.Long", "long?");
		SupportedTypes.put("java.lang.Float", "float?");
		SupportedTypes.put("java.lang.Double", "double?");
		SupportedTypes.put("java.lang.Boolean", "bool?");
		SupportedTypes.put("java.math.BigDecimal", "decimal?");
		SupportedTypes.put("java.time.LocalDate", "date?");
		SupportedTypes.put("java.time.OffsetDateTime", "timestamp?");
		SupportedTypes.put("org.joda.time.LocalDate", "date?");
		SupportedTypes.put("org.joda.time.DateTime", "timestamp?");
		SupportedTypes.put("byte[]", "binary");
		SupportedTypes.put("java.util.UUID", "uuid?");
		SupportedTypes.put("java.util.Map<java.lang.String,java.lang.String>", "map?");
		SupportedTypes.put("java.net.InetAddress", "ip?");
		SupportedTypes.put("java.awt.Color", "color?");
		SupportedTypes.put("java.awt.geom.Rectangle2D", "rectangle?");
		SupportedTypes.put("java.awt.image.BufferedImage", "image?");
	}

	private TypeElement jsonTypeElement;
	private DeclaredType jsonDeclaredType;
	private String namespace = "dsl_" + UUID.randomUUID().toString().replace("-", "");
	private boolean useJodaTime = false;

	private Elements elements;
	private Types types;
	private Messager messager;
	private TypeMirrorUtils utils;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);

		this.elements = processingEnv.getElementUtils();
		this.types = processingEnv.getTypeUtils();
		this.messager = processingEnv.getMessager();
		this.utils = new TypeMirrorUtils(types, elements, messager);

		jsonTypeElement = elements.getTypeElement(CompiledJson.class.getCanonicalName());
		jsonDeclaredType = types.getDeclaredType(jsonTypeElement);

		Map<String, String> options = processingEnv.getOptions();
		String ns = options.get("dsljson.namespace");
		if (ns != null && ns.length() > 0) {
			namespace = ns;
		}
		String timeApi = options.get("dsljson.timeApi");
		if ("joda-time".equals(timeApi)) {
			useJodaTime = true;
		}
	}

	static class StructInfo {
		public final TypeElement element;
		public final String name;
		public final boolean isEnum;

		public StructInfo(TypeElement element, String name, boolean isEnum) {
			this.element = element;
			this.name = name;
			this.isEnum = isEnum;
		}
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (roundEnv.processingOver()) {
			return false;
		}
		Set<? extends Element> jsonAnnotated = roundEnv.getElementsAnnotatedWith(jsonTypeElement);
		if (!jsonAnnotated.isEmpty()) {
			Map<String, StructInfo> structs = new HashMap<String, StructInfo>();
			StringBuilder dsl = new StringBuilder();
			dsl.append("module json {\n");
			for (Element el : jsonAnnotated) {
				if (!(el instanceof TypeElement)) {
					continue;
				}
				TypeElement element = (TypeElement) el;
				if (!hasEmptyCtor(element)) {
					AnnotationMirror entityAnnotation = getAnnotation(element, jsonDeclaredType);
					processingEnv.getMessager().printMessage(
							Diagnostic.Kind.ERROR,
							"CompiledJson requires public no argument constructor",
							element,
							entityAnnotation);
					//TODO: other checks
				} else {
					String name = "struct" + structs.size();
					structs.put(element.asType().toString(), new StructInfo(element, name, element.getKind() == ElementKind.ENUM));
				}
			}
			for (StructInfo info : structs.values()) {
				if (info.isEnum) {
					dsl.append("  enum ");
				} else {
					dsl.append("  struct ");
				}
				dsl.append(info.name);
				dsl.append(" {\n");

				if (info.isEnum) {
					List<String> constants = utils.getEnumConstants(info.element);
					for (String c : constants) {
						dsl.append("    ");
						dsl.append(c);
						dsl.append(";\n");
					}
				} else {
					Map<String, Element> properties = utils.getBeanProperties(info.element);
					for (Map.Entry<String, Element> p : properties.entrySet()) {
						String propertyType = getPropertyType(utils.returnType(p.getValue()), structs);
						if (propertyType != null) {
							dsl.append("    ");
							dsl.append(propertyType);
							dsl.append(" ");
							dsl.append(p.getKey());
							dsl.append(";\n");
						} else {
							processingEnv.getMessager().printMessage(
									Diagnostic.Kind.ERROR,
									"Specified property type not supported",
									p.getValue(),
									getAnnotation(info.element, jsonDeclaredType));
						}
					}
				}

				dsl.append("    external name Java '");
				dsl.append(info.element.getQualifiedName());
				dsl.append("';\n }\n");
			}
			dsl.append("}");

			if (dsl.length() < 20) {
				return false;
			}

			String fullDsl = dsl.toString();
			processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, fullDsl);

			String fileContent;
			try {
				fileContent = AnnotationCompiler.buildExternalJson(fullDsl, namespace, useJodaTime);
			} catch (Exception e) {
				processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "DSL compilation error\n" + e.getMessage());
				return false;
			}
			try {
				JavaFileObject jfo = processingEnv.getFiler().createSourceFile("ExternalSerialization");
				BufferedWriter bw = new BufferedWriter(jfo.openWriter());
				bw.write(fileContent);
				bw.close();
				FileObject rfo = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/com.dslplatform.json.Configuration");
				bw = new BufferedWriter(rfo.openWriter());
				bw.write(namespace + ".json.ExternalSerialization");
				bw.close();
			} catch (IOException e) {
				processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed saving compiled json serialization files");
			}
		}
		return false;
	}

	private boolean hasEmptyCtor(Element element) {
		for (ExecutableElement constructor : ElementFilter.constructorsIn(element.getEnclosedElements())) {
			List<? extends VariableElement> parameters = constructor.getParameters();
			if (parameters.isEmpty() && (element.getKind() == ElementKind.ENUM || constructor.getModifiers().contains(Modifier.PUBLIC))) {
				return true;
			}
		}
		return false;
	}

	private AnnotationMirror getAnnotation(Element element, DeclaredType annotationType) {
		for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
			if (types.isSameType(mirror.getAnnotationType(), annotationType)) {
				return mirror;
			}
		}
		return null;
	}

	private String getPropertyType(TypeMirror type, Map<String, StructInfo> structs) {
		String simpleType = SupportedTypes.get(type.toString());
		if (simpleType != null) {
			return simpleType;
		}
		if (type instanceof ArrayType) {
			ArrayType at = (ArrayType) type;
			simpleType = SupportedTypes.get(at.getComponentType().toString());
			if (simpleType != null) {
				return simpleType + "[]?";
			}
			StructInfo item = structs.get(at.getComponentType().toString());
			if (item != null) {
				return "json." + item.name + "?[]?";
			}
		}

		if (utils.isParameterizedList(type)) {
			String typeName = utils.containedTypeName(type);
			simpleType = SupportedTypes.get(typeName);
			if (simpleType != null) {
				return "List<" + simpleType + ">?";
			}
			StructInfo item = structs.get(typeName);
			if (item != null) {
				return "List<json." + item.name + "?>?";
			}
		}
		StructInfo info = structs.get(type.toString());
		if (info != null) {
			return "json." + info.name + "?";
		}
		return null;
	}
}
