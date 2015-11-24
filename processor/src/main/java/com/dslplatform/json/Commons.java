package com.dslplatform.json;

import javax.lang.model.element.Element;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class Commons {

    public interface Predicate<T> {
        public boolean apply(T e);
    }

    public static <T> List<T> filterWhereEither(List<T> coll, Predicate<T> ... predicates) {
        List<T> result = new ArrayList<T>();
        if(!nullOrEmpty(predicates))
        for(T element: coll) {
            for(Predicate<T> predicate: predicates) {
                if(predicate.apply(element)) {
                    result.add(element);
                    break;
                }
            }
        }
        System.out.println(result);
        return result;
    }

    public static <T> boolean nullOrEmpty(T[] objs) {
        return objs==null || objs.length ==0;
    }
    public static boolean nullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    public static boolean equalsEither(Object self, Object ... objs) {
        if(nullOrEmpty(objs)) return true;
        for(Object o: objs) {
            if(self.equals(o))
                return true;
        }
        return false;
    }

    public static boolean equalsAll(Object self, Object ... objs) {
        if(nullOrEmpty(objs)) return true;
        for(Object o: objs) {
            if(!self.equals(o))
                return false;
        }
        return true;
    }

    public static boolean either(Boolean ... exprs) {
        for(Boolean expr: exprs) {
            if(expr) return true;
        }
        return false;
    }

    public static <T> boolean contains(T[] coll, T a) {
        if(nullOrEmpty(coll)) return false;
        for(T t: coll) {
            if(t.equals(a)) return true;
        }
        return false;
    }
}
