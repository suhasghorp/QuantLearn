package com.quantlearn.utils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 *
 */
public class StreamUtil {
    public static <T, U, R> Stream<R> zip(Stream<T> s1, Stream<U> s2, BiFunction<T,U,R> f) {
        Iterator<U> itr = s2.iterator();
        return s1.filter(x-> itr.hasNext()).map(x -> f.apply(x, itr.next()));
    }
    public static <T,U> Stream<Pair<T,U>> zip(Stream<T> s1, Stream<U> s2) {
        return zip(s1, s2, (a, b) -> new Pair<T,U>(a, b));
    }
    public static <T> Stream<Pair<T,Long>> zipWithIndex(Stream<T> s1) {
        return zip(s1, LongStream.iterate(0, n -> n +1).boxed());
    }
    public static class Pair<T, U> {
        final public T _1;
        final public U _2;
        public Pair(T t, U u) {
            _1 = t;
            _2 = u;
        }
        @Override public boolean equals(Object o1){
            if (o1 instanceof Pair) {
                Pair<T,U> p = (Pair<T,U>)o1;
                return _1.equals(p._1) && _2.equals(p._2);
            }
            return false;
        }
        @Override public int hashCode() {
            int hash = 1;
            hash = hash * 31 + _1.hashCode();
            hash = hash * 31 + _2.hashCode();
            return hash;
        }
        @Override public String toString(){
            return "(" + _1 + "," + _2 +")";
        }
    }
    
    //test
    public static void main(String[] args) {
        List<String> list = Arrays.asList("1", "2", "3", "4");
        Stream<String> s1 = list.stream();
        Stream<String> s2 = list.stream().skip(1);
        
        Stream<Pair<String, String>> zip = zip(s1, s2);
        
        // lazy evalution
        zip.map(p -> Integer.parseInt(p._1) *10 + Integer.parseInt(p._2))
            .forEach(System.out::println);
        
        zipWithIndex(list.stream()).forEach(System.out::println);
        
    }
}