package canthonyl.datastructure.collection;


import java.util.function.Function;

public interface ProjectTarget<T, S> {

    void setConverter(Function<T, S> converter);

    void updateRange(Integer x1, Integer y1, Integer x2, Integer y2, T value);

}

