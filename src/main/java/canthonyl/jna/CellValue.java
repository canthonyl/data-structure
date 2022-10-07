package canthonyl.jna;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

public class CellValue extends Structure implements Structure.ByValue{
    private final Integer size;

    public CellValue(Integer size) {
        this.size = size;
        this.byteArray = new byte[size];
    }

    public CellValue(Integer size, Pointer p) {
        super(p);
        this.size = size;
        this.byteArray = new byte[size];
    }


    public void setValue(Integer val) {
        byteArray[0] = (byte)(((-1L>>>-8))&val);
        byteArray[1] = (byte)((((-1L>>>-8)<<8)&val)>>>8);
        byteArray[2] = (byte)((((-1L>>>-8)<<16)&val)>>>16);
        byteArray[3] = (byte)((((-1L>>>-8)<<24)&val)>>>24);
    }

    public void setValue(Long val) {
        for (Integer i=0; i<size; i++)
            byteArray[i] = (byte)((((-1L>>>-8)<<8*i)&val)>>>8*i);
    }

    public Integer getInteger(){
        Integer result = 0;
        for (Integer i=0; i<4; i++)
            result |= (-1^((-1^(0))&(-1^((-1>>>-8)&byteArray[i])))) << 8*i;
        return result;
    }

    public Long getLong(){
        Long result = 0L;
        for (Integer i=0; i<8; i++)
            result |= (-1L^((-1L^(0L))&(-1L^((-1L>>>-8)&byteArray[i])))) << 8*i;
        return result;
    }

    public Integer getSize(){ return size; }

    public byte[] byteArray;

    public static CellValue[][] array(Integer numCellX, Integer numCellY, Integer size, Pointer startAddress) {
        CellValue[][] result = (CellValue[][])Array.newInstance(CellValue.class, numCellY, numCellX);
        for (int y=0; y<numCellY; y++){
            for (int x=0; x<numCellX; x++){
                result[y][x] = new CellValue(size, startAddress.share(((long)y*numCellX+x)*size));
            }
        }
        return result;
    }

    public static CellValue[][] array(Integer numCellX, Integer numCellY, Integer size) {
        CellValue[][] result = (CellValue[][])Array.newInstance(CellValue.class, numCellY, numCellX);
        for (int y=0; y<numCellY; y++){
            for (int x=0; x<numCellX; x++){
                result[y][x] = new CellValue(size);
            }
        }
        return result;
    }

    @Override
    public List<String> getFieldOrder(){
        return Arrays.asList("byteArray");
    }
}
