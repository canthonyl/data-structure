package canthonyl.fixture;

import canthonyl.datastructure.collection.ProjectTarget;

import java.util.Arrays;
import java.util.function.Function;

public class ArrayProjection<T> implements ProjectTarget<T, T> {

    private final T[][] target;
    private Function<T, T> converter;

    public ArrayProjection(T[][] target) {
        this.target = target;
        this.converter = Function.identity();
    }

    @Override
    public void setConverter(Function<T, T> converter) {
        this.converter = converter;
    }

    @Override
    public void updateRange(Integer x1, Integer y1, Integer x2, Integer y2, T value) {
        for (int y=y1; y<=y2; y++){
            Arrays.fill(target[y], x1, x2+1, converter.apply(value));
        }
    }


}
