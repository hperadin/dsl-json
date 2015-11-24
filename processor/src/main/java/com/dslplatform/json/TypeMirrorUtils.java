package com.dslplatform.json;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;

import static com.dslplatform.json.Commons.filterWhereEither;
import static com.dslplatform.json.Commons.nullOrEmpty;

class TypeMirrorUtils {

    public Commons.Predicate<Element> IS_GETTER = new Commons.Predicate<Element>() {
        @Override
        public boolean apply(Element e) {
           return isGetter(e) && isAccesibleMethod(e);
        }
    };

    public Commons.Predicate<Element> IS_SETTER = new Commons.Predicate<Element>() {
        @Override
        public boolean apply(Element e) {
            return isSetter(e) && isAccesibleMethod(e);
        }
    };

    public Commons.Predicate<Element> IS_PUBLIC_FIELD = new Commons.Predicate<Element>() {
        @Override
        public boolean apply(Element e) {
            return isPublicField(e);
        }
    };

    private final Types types;
    private final Elements elements;
    private final Messager messager;

    public static final String GET_PREFIX = "get";
    public static final String SET_PREFIX = "set";
    public static final String IS_PREFIX = "is";

    // Quazi-constants, since this cannot be a static util class
    private final TypeMirror STRING_TYPE;
    private final TypeMirror OBJECT_TYPE;

    public final Comparator<Element> TYPE_NAME_COMPARATOR = new Comparator<Element>() {
        @Override
        public int compare(Element o1, Element o2) {
            return declaredTypeName(o1).compareTo(declaredTypeName(o2));
        }
    };

    public TypeMirrorUtils(Types types, Elements elements, Messager messager) {
        this.types = types;
        this.elements = elements;
        this.messager = messager;

        this.STRING_TYPE = elements.getTypeElement(String.class.getCanonicalName()).asType();
        this.OBJECT_TYPE = elements.getTypeElement(Object.class.getCanonicalName()).asType();
    }

    public boolean isNumeric(Element element) {
        return Commons.equalsEither(element.asType().getKind(),
                TypeKind.SHORT,
                TypeKind.INT,
                TypeKind.LONG,
                TypeKind.FLOAT,
                TypeKind.DOUBLE);
    }

    public boolean isPublicField(Element e) {
        return e.getKind().isField()
                && e.getModifiers().contains(Modifier.PUBLIC);
    }

    public boolean isPrimitive(Element element) {
        return element.asType().getKind().isPrimitive()
                || types.isSameType(element.asType(), STRING_TYPE);
    }

    public boolean isString(Element element) {
        System.out.println(element.asType().getKind() + ", " + STRING_TYPE);
        return element.asType().getKind().equals(TypeKind.CHAR)
                || types.isSameType(element.asType(), STRING_TYPE);
    }

    public boolean isBoolean(Element element) {
        return element.asType().getKind().equals(TypeKind.BOOLEAN);
    }

    public boolean isByte(Element element) {
        return element.asType().getKind().equals(TypeKind.BYTE);
    }

    public boolean isEnum(Element element)
    {
        return element.getKind().equals(ElementKind.ENUM);
    }

    public boolean isIterable(Element element) {
        TypeElement list = elements.getTypeElement(Iterable.class.getCanonicalName());
        return element.asType().getKind().equals(TypeKind.ARRAY)
                || types.isSubtype(element.asType(), types.getDeclaredType(list));
    }

    public boolean isClass(Element element) {
        return element.getKind().isClass()
            || element.getKind().isInterface();
    }

    public String declaredName(Element element) {
        return element.getSimpleName().toString();
    }

    public String declaredTypeName(Element element) {
        return element.asType().getKind().equals(TypeKind.EXECUTABLE) ?
                ((ExecutableElement)element).getReturnType().toString()
                : element.toString();
    }

    public String simpleName(Element element) {
        if(element.asType().getKind().equals(TypeKind.EXECUTABLE)) {
            return ((ExecutableElement)element).getSimpleName().toString();
        } else {
            return element.getSimpleName().toString();
        }
    }

    public List<Element> supertypeElements(Element element) {
        List<Element> supertypeElements = new ArrayList<Element>();
        for(TypeMirror tm: types.directSupertypes(element.asType())) {
            // Upon encountering Object, stop (this will ignore interfaces)
            if(types.isSameType(tm, OBJECT_TYPE))
                break;
            supertypeElements.add(types.asElement(tm));
        }
        return supertypeElements;
    }

    public List<Element> publicFields(Element element) {
        List<Element> publicFields = new ArrayList<Element>();
        for(Element enclosed: element.getEnclosedElements()) {
            if(isPublicField(enclosed)) {
                publicFields.add(enclosed);
            }
        }
        return publicFields;
    }

    public List<Element> nestedClasses(Element element) {
        List<Element> nestedClasses = new ArrayList<Element>();
        for(Element enclosed: element.getEnclosedElements()) {
            if(enclosed.getKind().isClass() || enclosed.getKind().isInterface()) {
                nestedClasses.add(enclosed);
            }
        }
        return nestedClasses;
    }

    public boolean isGetter(Element element) {
        return isGetterName(declaredName(element))
                && element.getKind().equals(ElementKind.METHOD)
                && element.getModifiers().contains(Modifier.PUBLIC)
                && !((ExecutableElement) element).getReturnType().getKind().equals(TypeKind.VOID)
                && ((ExecutableElement) element).getParameters().isEmpty();
    }

    public boolean isSetter(Element element) {
        return isSetterName(declaredName(element))
                && element.getKind().equals(ElementKind.METHOD)
                && element.getModifiers().contains(Modifier.PUBLIC)
                && ((ExecutableElement) element).getReturnType().getKind().equals(TypeKind.VOID)
                && ((ExecutableElement) element).getParameters().size() == 1;
    }

    public List<ExecutableElement> getters(Element element) {
        List<ExecutableElement> getters = new ArrayList<ExecutableElement>();
        for(Element enclosed: element.getEnclosedElements()) {
            if(isGetter(enclosed)) getters.add((ExecutableElement)enclosed);
        }
        return getters;
    }

    public List<ExecutableElement> setters(Element element) {
        List<ExecutableElement> setters = new ArrayList<ExecutableElement>();
        for(Element enclosed: element.getEnclosedElements()) {
            if(isSetter(enclosed)) setters.add((ExecutableElement)enclosed);
        }
        return setters;
    }

    public String fieldNameFromAccessor(Element accessor) {
        if(isGetter(accessor) || isSetter(accessor))
            return fieldNameFromMethodName(declaredName(accessor));
        else return declaredName(accessor);
    }

    public static String fieldNameFromMethodName(String methodName) {
        if(methodName.startsWith(GET_PREFIX)) {
            return lowercaseFirst(methodName.substring(GET_PREFIX.length()));
        } else if(methodName.startsWith(IS_PREFIX)) {
            return lowercaseFirst(methodName.substring(IS_PREFIX.length()));
        } else if(methodName.startsWith(SET_PREFIX)) {
            return lowercaseFirst(methodName.substring(SET_PREFIX.length()));
        } else {
            return methodName;
        }
    }

    public static boolean isGetterName(String name) {
        return (name.startsWith(GET_PREFIX) || name.startsWith(IS_PREFIX))
                && !name.equals(GET_PREFIX) && !name.equals(IS_PREFIX);
    }

    public static boolean isSetterName(String name) {
        return name.startsWith(SET_PREFIX)
                && !name.equals(SET_PREFIX);
    }

    public static String lowercaseFirst(String string) {
        if(nullOrEmpty(string) || string.length() == 1) return string;
        return string.toLowerCase().charAt(0) + string.substring(1);
    }

    public List<String> getEnumConstants(TypeElement element) {
        List<String> result = new ArrayList<String>();
        for (VariableElement field : ElementFilter.fieldsIn(element.getEnclosedElements())) {
            String name = field.getSimpleName().toString();
            if (field.asType().equals(element.asType())) {
                result.add(name);
            }
        }
        return result;
    }

    public boolean isAccesibleMethod(Element element) {
        if(!element.getKind().equals(ElementKind.METHOD)) return false;
        ExecutableElement method = (ExecutableElement) element;
        return method.getModifiers().contains(Modifier.PUBLIC)
                && !method.getModifiers().contains(Modifier.STATIC)
                && !method.getModifiers().contains(Modifier.ABSTRACT);
    }

    public Map<String, ExecutableElement> getBeanProperties(TypeElement element) {
        Map<String, VariableElement> setters = new HashMap<String, VariableElement>();
        Map<String, ExecutableElement> getters = new HashMap<String, ExecutableElement>();
        Map<String, Element> publicFields = new HashMap<String, Element>();

        List<Element> enclosedElements = (List<Element>)element.getEnclosedElements();
        for (Element enclosed : filterWhereEither(enclosedElements, IS_GETTER, IS_SETTER, IS_PUBLIC_FIELD)) {
            System.out.println(declaredName(enclosed));
            String property = fieldNameFromAccessor(enclosed);

            if(isGetter(enclosed)) {
                getters.put(property, (ExecutableElement) enclosed);
            }
            else if(isSetter(enclosed)) {
                setters.put(property, ((ExecutableElement) enclosed).getParameters().get(0));
            }
            else {
                publicFields.put(property, enclosed);
            }
        }

        Map<String, ExecutableElement> result = new HashMap<String, ExecutableElement>();
        for (Map.Entry<String, ExecutableElement> kv : getters.entrySet()) {
            VariableElement setterArgument = setters.get(kv.getKey());
            if (setterArgument != null && setterArgument.asType().equals(kv.getValue().getReturnType())) {
                result.put(kv.getKey(), kv.getValue());
            } else if (setterArgument != null && (setterArgument.asType() + "<").startsWith(kv.getValue().getReturnType().toString())) {
                result.put(kv.getKey(), kv.getValue());
            }
        }
        return result;
    }
}
