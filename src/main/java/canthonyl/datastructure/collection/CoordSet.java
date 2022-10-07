package canthonyl.datastructure.collection;


import canthonyl.jna.CellValue;
import com.sun.jna.Pointer;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static java.lang.Math.abs;
import static java.lang.Math.min;

public class CoordSet<T> implements ProjectTarget<T, Long> {

    public static final Long[] row = LongStream
            .range(0, 8)
            .map(i -> (-1L >>> -8) << i*8)
            .boxed().toArray(Long[]::new);

    public static final Long[] col = LongStream
            .iterate(1L|1L<<8|1L<<16|1L<<24|1L<<32|1L<<40|1L<<48|1L<<56, i -> i << 1)
            .limit(8)
            .boxed().toArray(Long[]::new);

    public static final Long[] fromX = LongStream.range(0,8)
            .map(i -> (((-1L>>>i-8)<<i))|(((-1L>>>i-8)<<i)<<8)|(((-1L>>>i-8)<<i)<<16)|(((-1L>>>i-8)<<i)<<24)|(((-1L>>>i-8)<<i)<<32)|(((-1L>>>i-8)<<i)<<40)|(((-1L>>>i-8)<<i)<<48)|(((-1L>>>i-8)<<i)<<56))
            .boxed().toArray(Long[]::new);

    public static final Long[] toX = LongStream
            .iterate(1L|1L<<8|1L<<16|1L<<24|1L<<32|1L<<40|1L<<48|1L<<56, i -> i|(i<<1))
            .limit(8)
            .boxed().toArray(Long[]::new);

    public static final Long[] fromY = LongStream
            .range(0, 8)
            .map(i -> -1L << i*8)
            .boxed().toArray(Long[]::new);

    public static final Long[] toY = LongStream
            .range(0, 8)
            .map(i -> -1L >>> -(i+1)*8)
            .boxed().toArray(Long[]::new);

    private static final Integer ORIENTATION_X = 0;
    private static final Integer ORIENTATION_Y = 1;

    private final Integer cellWidth;
    private final Integer cellHeight;
    private final CellValue[][] vals;

    private final Integer[] cellBoundByOrientation;
    private final Long[][] maskByOrientation;
    private final Integer w;
    private final Integer h;
    private final Integer numCellsX;
    private final Integer numCellsY;

    private Integer coordCount;
    private CompressionInfo compressionInfo;
    private Function<T, Long> converter;


    public CoordSet(Integer width, Integer height) {
        this(width, height, (x,y) -> 0L);
    }

    public CoordSet(Integer width, Integer height, LongBinaryOperator supplier) {
        cellWidth = 8;
        cellHeight = 8;
        w = width;
        h = height;
        numCellsX = min(1, width % cellWidth) + width / cellWidth;
        numCellsY = min(1, height % cellHeight) + height / cellHeight;
        coordCount = 0;
        cellBoundByOrientation = new Integer[]{numCellsX, numCellsY};
        maskByOrientation = new Long[][]{col, row};

        vals = CellValue.array(numCellsX, numCellsY, cellWidth * cellHeight / 8);
        for (Integer y = 0; y< numCellsY; y++){
            for (Integer x = 0; x< numCellsX; x++) {
                vals[y][x].setValue(supplier.applyAsLong(y,x));
            }
        }
        compressionInfo = new CompressionInfo(width, height);
    }

    public CoordSet(Integer width, Integer height, Integer cHeight, Pointer address) {
        cellWidth = 8;
        cellHeight = cHeight;
        numCellsX = width;
        numCellsY = height;
        w = numCellsX * cellWidth;
        h = numCellsY * cellHeight;
        coordCount = 0;
        cellBoundByOrientation = new Integer[]{numCellsX, numCellsY};
        maskByOrientation = new Long[][]{col, row};
        vals = address != null ? CellValue.array(numCellsX, numCellsY, cellWidth * cellHeight / 8, address) : CellValue.array(numCellsX, numCellsY, cellWidth * cellHeight / 8) ;
        compressionInfo = new CompressionInfo(width, height);
    }


    public CoordSet(String encodedKey) {
        String key = new String(Base64.getDecoder().decode(encodedKey.getBytes()));
        String[] wh = key.substring(0, key.indexOf("\n")).split(",");
        String[] allRows = key.substring(key.indexOf("\n")+1).split("\n");
        cellWidth = 8;
        cellHeight = 8;
        w = Integer.valueOf(wh[0]);
        h = Integer.valueOf(wh[1]);
        numCellsX = min(1, w % cellWidth) + w / cellWidth;
        numCellsY = min(1, h % cellHeight) + h / cellHeight;
        coordCount = 0;
        cellBoundByOrientation = new Integer[]{numCellsX, numCellsY};
        maskByOrientation = new Long[][]{col, row};

        vals = CellValue.array(numCellsX, numCellsY, cellWidth * cellHeight / 8);
        for (Integer cellY=0; cellY< numCellsY; cellY++){
            String[] cols = allRows[cellY].split(",");
            for (Integer cellX = 0; cellX < numCellsX; cellX++){
                Long value = Long.valueOf(cols[cellX]);
                vals[cellY][cellX].setValue(value);
                coordCount += Long.bitCount(value);
            }
        }

        compressionInfo = new CompressionInfo(w, h);
    }

    public Integer getWidth() { return w; }
    public Integer getHeight() { return h; }

    //determine if cell should be clipped with respect to given absolute bound
    private Long maskCell(Integer cellHIndex, Integer cellVIndex, Integer boundStartX, Integer boundStartY, Integer boundEndX, Integer boundEndY) {
        Integer cellStartX = cellHIndex * cellWidth;
        Integer cellStartY = cellVIndex * cellHeight;
        Integer cellEndX = min(cellStartX + cellWidth -1, w - 1);
        Integer cellEndY = min(cellStartY + cellHeight -1, h - 1);

        if ( boundStartX-cellStartX > 0 || cellEndX-boundEndX > 0 || boundStartY - cellStartY > 0 || cellEndY - boundEndY > 0 ) {
            Integer clipStartX = Math.max(cellStartX, boundStartX) - cellStartX;
            Integer clipStartY = Math.max(cellStartY, boundStartY) - cellStartY;
            Integer clipEndX = min(cellEndX, boundEndX) - cellStartX;
            Integer clipEndY = min(cellEndY, boundEndY) - cellStartY;
            return fromX[clipStartX] & fromY[clipStartY] & toX[clipEndX] & toY[clipEndY];
        } else {
            return -1L;
        }
    }

    public CoordSet retain(Integer orientation, Integer direction) {
        CoordSet result = new CoordSet(w, h);
        result.addAll(this);
        result.retain(orientation, direction, 0, 0, getWidth()-1, getHeight()-1);
        return result;
    }

    Optional<Long> prevCellVal(Integer cellX, Integer cellY, Integer orientation, Integer direction){
        Optional<Long> result = Optional.empty();
        if (orientation == ORIENTATION_X) {
            if (direction == -1 && cellX < numCellsX-1) {
                result = Optional.of(vals[cellY][cellX+1].getLong());
            } else if (direction == 1 && cellX > 0) {
                result = Optional.of(vals[cellY][cellX-1].getLong());
            }
        } else {
            if (direction == -1 && cellY < numCellsY-1 ){
                result = Optional.of(vals[cellY+1][cellX].getLong());
            } else if (direction == 1 && cellY > 0) {
                result = Optional.of(vals[cellY-1][cellX].getLong());
            }
        }
        return result;
    }

    Long retain(Optional<Long> prevCellStartVal, Long currentCellStartVal, Optional<Long> prevCellVal, Long currentCellVal, Integer orientation, Integer direction){
        Integer[][] deltaByOrientation = {{direction, 0},{0, direction}};
        Integer[] delta = deltaByOrientation[orientation];

        Long prevStartValShifted = shift(prevCellStartVal.orElse(0L), delta[0]*-7, delta[1]*-7);
        Long curStartValShifted = shift(currentCellStartVal, delta[0], delta[1]);
        Long shiftedStartVal = (currentCellStartVal^-1L) & (-1L^((-1L^prevStartValShifted) & (-1L^curStartValShifted))) & currentCellVal;

        Long prevCellValShifted = shift(prevCellVal.orElse(0L), delta[0]*-7, delta[1]*-7);
        Long curCellValShifted = shift(currentCellVal, delta[0], delta[1]);
        Long shiftedCellVal = (currentCellVal^-1L) & (-1L^((-1L^prevCellValShifted) & (-1L^curCellValShifted)));

        Long shiftedVal = -1L^((-1L^shiftRetainEdge(shift(shiftedStartVal, delta[0]*0, delta[1]*0), delta[0]*0, delta[1]*0)) &
                (-1L^shiftRetainEdge(shift(shiftedStartVal, delta[0]*1, delta[1]*1), delta[0]*-1, delta[1]*-1)) &
                (-1L^shiftRetainEdge(shift(shiftedStartVal, delta[0]*2, delta[1]*2), delta[0]*-2, delta[1]*-2)) &
                (-1L^shiftRetainEdge(shift(shiftedStartVal, delta[0]*3, delta[1]*3), delta[0]*-3, delta[1]*-3)) &
                (-1L^shiftRetainEdge(shift(shiftedStartVal, delta[0]*4, delta[1]*4), delta[0]*-4, delta[1]*-4)) &
                (-1L^shiftRetainEdge(shift(shiftedStartVal, delta[0]*5, delta[1]*5), delta[0]*-5, delta[1]*-5)) &
                (-1L^shiftRetainEdge(shift(shiftedStartVal, delta[0]*6, delta[1]*6), delta[0]*-6, delta[1]*-6)) &
                (-1L^shiftRetainEdge(shift(shiftedStartVal, delta[0]*7, delta[1]*7), delta[0]*-7, delta[1]*-7)));

        Long shiftedVals = shiftedCellVal & shiftedVal;

        Long scvirs = -1L^((-1L^(-1L^((-1L^shiftRetainEdge(shift(shiftedVals,delta[0]*1, delta[1]*1), delta[0]*-1, delta[1]*-1)) &
                (-1L^shiftRetainEdge(shift(shiftedVals, delta[0]*2, delta[1]*2), delta[0]*-2, delta[1]*-2)) &
                (-1L^shiftRetainEdge(shift(shiftedVals, delta[0]*3, delta[1]*3), delta[0]*-3, delta[1]*-3)) &
                (-1L^shiftRetainEdge(shift(shiftedVals, delta[0]*4, delta[1]*4), delta[0]*-4, delta[1]*-4)) &
                (-1L^shiftRetainEdge(shift(shiftedVals, delta[0]*5, delta[1]*5), delta[0]*-5, delta[1]*-5)) &
                (-1L^shiftRetainEdge(shift(shiftedVals, delta[0]*6, delta[1]*6), delta[0]*-6, delta[1]*-6)) &
                (-1L^shiftRetainEdge(shift(shiftedVals, delta[0]*7, delta[1]*7), delta[0]*-7, delta[1]*-7))))) &
                (-1L^shiftRetainEdge(shiftedVal, delta[0]*-8, delta[1]*-8)))
                ;

        return (shiftedVal & (scvirs^-1L)) & currentCellVal ;

    }

    public CoordSet retain(CoordSet target){
        CoordSet result = new CoordSet(w, h);
        Integer horizontal = 0;
        Integer vertical = 1;

        for (Integer cellY = 0; cellY < numCellsY; cellY++){
            for (Integer cellX = 0; cellX < numCellsX; cellX++){
                Optional<Long> nStartVal = prevCellVal(cellX, cellY, vertical, 1);
                Optional<Long> nCellVal = target.prevCellVal(cellX, cellY, vertical, 1);
                Optional<Long> eStartVal = prevCellVal(cellX, cellY, horizontal, -1);
                Optional<Long> eCellVal = target.prevCellVal(cellX, cellY, horizontal, -1);
                Optional<Long> sStartVal = prevCellVal(cellX, cellY, vertical, -1);
                Optional<Long> sCellVal = target.prevCellVal(cellX, cellY, vertical, -1);
                Optional<Long> wStartVal = prevCellVal(cellX, cellY, horizontal, 1);
                Optional<Long> wCellVal = target.prevCellVal(cellX, cellY, horizontal, 1);

                Long startVal = vals[cellY][cellX].getLong();
                Long cellVal = target.vals[cellY][cellX].getLong();

                Long nResult = retain(sStartVal, startVal, sCellVal, cellVal, vertical, -1);
                Long eResult = retain(wStartVal, startVal, wCellVal, cellVal, horizontal, 1);
                Long sResult = retain(nStartVal, startVal, nCellVal, cellVal, vertical, 1);
                Long wResult = retain(eStartVal, startVal, eCellVal, cellVal, horizontal, -1);

                Long retainedVal = nResult | eResult | sResult | wResult;
                result.vals[cellY][cellX].setValue(retainedVal);
                result.coordCount += Long.bitCount(retainedVal);
            }
        }
        return result;
    }


    public void retain(Integer orientation, Integer direction, Integer x1, Integer y1, Integer x2, Integer y2) {
        Integer directionInd = direction == 1 ? 0 : 1;

        Integer[][] select = {{0,1}, {1,0}};
        Integer[][] range = {{x1/8, x2/8}, {y1/8, y2/8}};
        Integer[][] levelBound = {{0,7},{7,0}};

        Integer search = select[orientation][0];
        Integer step = select[orientation][1];
        Integer start = select[directionInd][0];
        Integer end = select[directionInd][1];
        Integer[] i = new Integer[2]; //x,y
        Long[] searchDirectionMask = orientation.intValue() == 0 ? col : row;

        for (i[step] = range[step][0]; i[step] <= range[step][1]; i[step]++){
            Long prev = 0L;
            Long toggle = -1L;
            for (i[search] = range[search][start]; i[search].compareTo(range[search][end]) != direction; i[search] += direction) {
                Integer cellX = i[0];
                Integer cellY = i[1];

                Long resultMask = maskCell(cellX, cellY, x1, y1, x2, y2);
                CellValue cellValue = vals[cellY][cellX];
                Long val = cellValue.getLong();
                Long maskedVal = val & resultMask;

                Long result = 0L;
                Long currentSegment;

                for (Integer l=levelBound[directionInd][0]; l.compareTo(levelBound[directionInd][1]) != direction; l+= direction){
                    currentSegment = maskedVal & searchDirectionMask[l];
                    result |= (currentSegment & toggle & searchDirectionMask[l]);
                    toggle &= (-1L^prev) | currentSegment;
                    prev = currentSegment;
                    if (l != levelBound[directionInd][1]) {
                        Integer shiftBy = orientation.intValue() == 0 ? 1 : cellWidth;
                        if (direction == 1) {
                            prev = prev << shiftBy;
                            toggle = toggle << shiftBy;
                        } else {
                            prev = prev >>> shiftBy;
                            toggle = toggle >>> shiftBy;
                        }
                    }
                }
                Long resultValue = -1L^(((-1L^(val & (-1L^resultMask))) & (-1L^result)));
                cellValue.setValue( resultValue );
                Integer resetShift = orientation.intValue() == 0 ? cellWidth-1 : (cellHeight-1) * (cellWidth);
                if (direction == 1) {
                    prev = prev >>> resetShift;
                    toggle = toggle >>> resetShift;
                } else {
                    prev = prev << resetShift;
                    toggle = toggle << resetShift;
                }
            }
        }
    }

    public Boolean containsAllCoordsIn(CoordSet targetSet) {
        for(Integer y=0; y<numCellsY; y++){
            for (Integer x=0; x< numCellsX; x++){
                Long compare = targetSet.vals[y][x].getLong() & (-1L^(vals[y][x].getLong()));
                if (!Objects.equals(compare, 0L)) {
                    return false;
                }
            }
        }
        return true;
    }

    public Boolean containsAnyCoordsIn(CoordSet targetSet) {
        for(Integer y=0; y<numCellsY; y++){
            for (Integer x=0; x< numCellsX; x++){
                Long compare = targetSet.vals[y][x].getLong() & vals[y][x].getLong();
                if (!Objects.equals(compare, 0L)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Long compress(Long originalValue, Long compressionXBit, Long compressionYBit) {
        Long yCompressedValue = 0L;
        Integer yEndBit = 64-Long.numberOfLeadingZeros(compressionYBit);
        Integer yStart = Long.numberOfTrailingZeros(compressionYBit);
        Integer end;
        Integer rowsKept = 0;
        while (yStart < yEndBit) {
            end = Long.numberOfTrailingZeros(compressionYBit + Long.lowestOneBit(compressionYBit));
            yCompressedValue |= (originalValue & fromY[yStart] & toY[end-1]) >>> (yStart - rowsKept) * cellHeight;
            compressionYBit &= (-1L << end);
            rowsKept += (end - yStart);
            yStart = Long.numberOfTrailingZeros(compressionYBit);
        }

        Long xCompressedValue = 0L;
        Integer xEndBit = 64-Long.numberOfLeadingZeros(compressionXBit);
        Integer xStart = Long.numberOfTrailingZeros(compressionXBit);
        Integer xEnd;
        Integer colKept = 0;
        while (xStart < xEndBit) {
            xEnd = Long.numberOfTrailingZeros(compressionXBit + Long.lowestOneBit(compressionXBit));
            Integer numShift = xStart - colKept;
            xCompressedValue |= ((yCompressedValue & fromX[xStart] & toX[xEnd-1]) >>> (numShift)) /*& toX[7-numShift]*/;
            compressionXBit &= (-1L << xEnd);
            colKept += (xEnd - xStart);
            xStart = Long.numberOfTrailingZeros(compressionXBit);
        }

        return xCompressedValue;
    }

    @Override
    public void setConverter(Function<T, Long> converter) {
        this.converter = converter;
    }

    @Override
    public void updateRange(Integer x1, Integer y1, Integer x2, Integer y2, T value) {
        Long convertedValue = converter.apply(value);

        for (Integer cellY=y1; cellY <= y2; cellY++) {
            for (Integer cellX=x1; cellX <= x2; cellX++) {
                CellValue cellValue = vals[cellY][cellX];
                cellValue.setValue(convertedValue);
                cellValue.write();
            }
        }

    }


    class CompressionInfo {
        final CompressionBand x;
        final CompressionBand y;

        CompressionInfo(Integer width, Integer height){
            x = new CompressionBand(width);
            y = new CompressionBand(height);
        }
        void clear(){ x.clear(); y.clear(); }
        void reset(){ x.resetCellIndex(); y.resetCellIndex(); }
        Integer originalWidth() { return x.dimensionLength; }
        Integer originalHeight() { return y.dimensionLength; }
    }

    class CompressionBand {
        private Integer cellLength = 8;
        private Integer bucketLength = 64;
        private Integer dimensionLength;

        private Integer offset;
        private Integer numBuckets;
        private Integer totalBitCount;

        private Long[] buckets;

        CompressionBand(Integer length) {
            this.dimensionLength = length;
            this.numBuckets = min(length % bucketLength, 1) + length / bucketLength;
            this.buckets = IntStream.range(0, numBuckets).mapToObj(i -> 0L).toArray(Long[]::new);
            this.offset = 0;
            this.totalBitCount = 0;
        }

        void encodeCell(Long val) {
            Integer index = offset / bucketLength;
            Integer shift = offset % bucketLength;
            Long prevVal = buckets[index];
            buckets[index] |= (val & row[0]) << shift;
            totalBitCount += Long.bitCount(buckets[index] ^ prevVal);
        }

        void clear() { Arrays.fill(buckets, 0L); totalBitCount = 0; }
        void resetCellIndex() { offset = 0; }
        void nextCell() { offset += cellLength; }

        Integer cellCompressBitCount() { return Long.bitCount(currentValue()); }

        Integer totalBitCount() { return totalBitCount; }

        Long valueAtCell(Integer cellIndex){
            Integer index = (cellIndex * cellLength) / bucketLength;
            Integer shift = (cellIndex * cellLength) % bucketLength;
            return (buckets[index] >>> shift) & row[0];
        }

        Long currentValue() {
            Integer index = offset / bucketLength;
            Integer shift = offset % bucketLength;
            return (buckets[index] >>> shift) & row[0];
        }

    }

    private Long transposeIfSet(Long value, Integer orientation) {
        Long[] mask = maskByOrientation[orientation];
        Long shifted = 0L;
        for (Integer level=0; level<8; level++){
            if (!Objects.equals(value & mask[level], 0L)) {
                shifted |= 1L << level;
            }
        }
        return shifted;
    }

    private Long shift(Long value, Integer dx, Integer dy) {
        Long result = 0L;
        if (Math.abs(dx) < cellWidth && Math.abs(dy) < cellHeight) {
            Integer shift = dy * cellWidth + dx;
            Long mask = dx < 0 ? toX[cellWidth + dx - 1] : fromX[dx];
            if (shift < 0) {
                result |= (value >>> Math.abs(shift)) & mask;
            } else {
                result |= (value << Math.abs(shift)) & mask;
            }
        }
        return result;
    }

    Long shiftRetainEdge(Long value, Integer dx, Integer dy){
        Long x = abs(dx) > 0 ? (dx < 0 ? value & col[7] : value & col[0]) : 0L;
        Long y = abs(dy) > 0 ? (dy < 0 ? value & row[7] : value & row[0]) : 0L;

        Long xBackground = -1L^((-1L^shift(-1L^((-1L^(dx > 0 || dx < -7 ? (dx > 0 ? (dx>7? (x^x) : x) : (x^x) >>> 7) : 0L)) &
                (-1L^(dx > 1 || dx < -6 ? (dx > 1 ? (dx>7? (x^x) : x) << 1 : (dx<-7? x^x : x) >>> 6) : 0L)) &
                (-1L^(dx > 2 || dx < -5 ? (dx > 2 ? (dx>7? (x^x) : x) << 2 : (dx<-7? x^x : x) >>> 5) : 0L)) &
                (-1L^(dx > 3 || dx < -4 ? (dx > 3 ? (dx>7? (x^x) : x) << 3 : (dx<-7? x^x : x) >>> 4) : 0L)) &
                (-1L^(dx > 4 || dx < -3 ? (dx > 4 ? (dx>7? (x^x) : x) << 4 : (dx<-7? x^x : x) >>> 3) : 0L)) &
                (-1L^(dx > 5 || dx < -2 ? (dx > 5 ? (dx>7? (x^x) : x) << 5 : (dx<-7? x^x : x) >>> 2) : 0L)) &
                (-1L^(dx > 6 || dx < -1 ? (dx > 6 ? (dx>7? (x^x) : x) << 6 : (dx<-7? x^x : x) >>> 1) : 0L)) &
                (-1L^(dx > 7 || dx < 0 ? (dx > 7 ? (x^x) << 7 : (dx<-7?(x^x):x)) : 0L))),0, dy)) &
                (-1L^((dx > 7 || dx < -7 ? (x>0||x<0?(dx > 7? col[0]&(x^-1L) : col[7]&(x^-1L)):0L) : 0L))));

        Long yBackground = -1L^((-1L^shift(-1L^((-1L^(dy > 0 || dy < -7 ? (dy > 0 ? (dy>7? (y^y) : y) : (y^y) >>> 56): 0L)) &
                (-1L^(dy > 1 || dy < -6 ? (dy > 1? (dy>7? (y^y) : y) << 8 : (dy<-7? (y^y): y) >>> 48) : 0L)) &
                (-1L^(dy > 2 || dy < -5 ? (dy > 2? (dy>7? (y^y) : y) << 16: (dy<-7? (y^y): y) >>> 40) : 0L)) &
                (-1L^(dy > 3 || dy < -4 ? (dy > 3? (dy>7? (y^y) : y) << 24: (dy<-7? (y^y): y) >>> 32) : 0L)) &
                (-1L^(dy > 4 || dy < -3 ? (dy > 4? (dy>7? (y^y) : y) << 32: (dy<-7? (y^y): y) >>> 24) : 0L)) &
                (-1L^(dy > 5 || dy < -2 ? (dy > 5? (dy>7? (y^y) : y) << 40: (dy<-7? (y^y): y) >>> 16) : 0L)) &
                (-1L^(dy > 6 || dy < -1 ? (dy > 6? (dy>7? (y^y) : y) << 48: (dy<-7? (y^y): y) >>> 8) : 0L)) &
                (-1L^(dy > 7 || dy < 0 ? (dy > 7? (y^y) << 56: (dy<-7?(y^y):y)) : 0L))), dx, 0)) &
                (-1L^((dy > 7 || dy < -7 ? (y>0||y<0?(dy > 7? row[0]&(y^-1L) : row[7]&(y^-1L)):0L) : 0L))));

        Long xBackgroundClipped = (abs(dx) > 0 ? (dx < 0 ? fromX[7-abs(dx+1)] : toX[abs(dx)-1]) : 0L);
        Long yBackgroundClipped = (abs(dy) > 0 ? (dy < 0 ? fromY[7-abs(dy+1)] : toY[abs(dy)-1]) : 0L);

        Long result = shift(value, dx, dy);
        return -1L^((-1L^(-1L^((-1L^(xBackground & xBackgroundClipped)) & (-1L^(yBackground & yBackgroundClipped))))) & (-1L^result));
    }

    void overlay(Integer x, Integer y, Long value){
        Integer offsetX = x % cellWidth;
        Integer offsetY = y % cellHeight;
        Integer cellY = y / cellHeight;
        Integer cellX = x / cellWidth;

        Long maskX = offsetX == 0 ? 0L : fromX[0] & toX[offsetX - 1];
        Long maskY = offsetY == 0 ? 0L : fromY[0] & toY[offsetY - 1];
        Long shifted = shift(value, offsetX, offsetY);
        Long currentValue = vals[cellY][cellX].getLong();
        vals[cellY][cellX].setValue((currentValue & (maskX | maskY)) | shifted);

        Boolean nextX = cellX < numCellsX - 1 && offsetX > 0;
        Boolean nextY = cellY < numCellsY - 1 && offsetY > 0;

        if (nextX) {
            currentValue = vals[cellY][cellX+1].getLong();
            shifted = shift(value, offsetX- cellWidth, offsetY);
            vals[cellY][cellX+1].setValue((currentValue & (fromX[offsetX] & toX[7] | maskY)) | shifted);
        }
        if (nextY) {
            currentValue = vals[cellY+1][cellX].getLong();
            shifted = shift(value, offsetX, offsetY- cellHeight);
            vals[cellY+1][cellX].setValue((currentValue & (fromY[offsetY] & toY[7] | maskX)) | shifted);
        }
        if (nextX && nextY) {
            currentValue = vals[cellY+1][cellX+1].getLong();
            shifted = shift(value, offsetX- cellWidth, offsetY- cellHeight);
            vals[cellY+1][cellX+1].setValue((currentValue & ((fromY[offsetY] & toY[7]) | (fromX[offsetX] & toX[7]))) | shifted);
        }
    }

    Long extract(Integer x, Integer y) {
        Integer offsetX = x % cellWidth;
        Integer offsetY = y % cellHeight;
        Integer cellX = x / cellWidth;
        Integer cellY = y / cellHeight;

        Long value = shift(vals[cellY][cellX].getLong(), -offsetX, -offsetY);

        Boolean nextX = cellX < numCellsX - 1 && offsetX > 0;
        Boolean nextY = cellY < numCellsY - 1 && offsetY > 0;
        if (nextX) value |= shift(vals[cellY][cellX+1].getLong(), 8-offsetX, -offsetY);
        if (nextY) value |= shift(vals[cellY+1][cellX].getLong(), -offsetX, 8-offsetY);
        if (nextX && nextY) value |= shift(vals[cellY+1][cellX+1].getLong(), 8-offsetX, 8-offsetY);

        if (cellX == numCellsX-1 && offsetX > 0) {
            value = (value & toX[(w-1)% cellWidth]) | (-1L^vals[cellY][cellX].getLong()) & col[(w-1)% cellWidth] ;
        }
        if (cellY == numCellsY-1 && offsetY > 0) {
            value = (value & toY[(h-1)% cellHeight]) | (-1L^vals[cellY][cellX].getLong()) & row[(h-1)% cellHeight] ;
        }
        return value;
    }


    public CoordSet compact() {

        compressionInfo.clear();
        for (Integer cellY = 0; cellY < numCellsY; cellY++) {
            Long yCompress = 0L;
            compressionInfo.x.resetCellIndex();
            for (Integer cellX = 0; cellX < numCellsX; cellX++) {
                Integer cellStartX = cellX * cellWidth;
                Integer cellStartY = cellY * cellHeight;

                Long cellValue = vals[cellY][cellX].getLong();
                Long xCompress = cellValue ^ extract(cellStartX + 1, cellStartY) ;
                Long transposed = transposeIfSet(xCompress, 0);
                compressionInfo.x.encodeCell(transposed);
                compressionInfo.x.nextCell();

                yCompress |= cellValue ^ extract(cellStartX, cellStartY+1);
            }
            Long transposed = transposeIfSet(yCompress, 1);
            compressionInfo.y.encodeCell(transposed);
            compressionInfo.y.nextCell();
        }

        CoordSet result = new CoordSet(compressionInfo.x.totalBitCount(), compressionInfo.y.totalBitCount());

        Integer resultSize = 0;
        Integer currentX = 0;
        Integer currentY = 0;
        compressionInfo.reset();

        for (Integer cellX = 0; cellX < numCellsX; cellX++ ){
            for (Integer cellY = 0; cellY < numCellsY; cellY++){
                Long original = vals[cellY][cellX].getLong();
                Long compressed = compress(original, compressionInfo.x.currentValue(), compressionInfo.y.currentValue());
                result.overlay(currentX, currentY, compressed);
                currentY += compressionInfo.y.cellCompressBitCount();
                compressionInfo.y.nextCell();
                resultSize += Long.bitCount(compressed);
            }
            currentX += compressionInfo.x.cellCompressBitCount();
            currentY = 0;
            compressionInfo.x.nextCell();
            compressionInfo.y.resetCellIndex();
        }

        result.coordCount = resultSize;
        return result;
    }

    CoordSet compact(CompressionInfo info){
        CoordSet result = new CoordSet(info.x.totalBitCount(), info.y.totalBitCount());

        Integer resultSize = 0;
        Integer currentX = 0;
        Integer currentY = 0;
        info.reset();

        for (Integer cellX = 0; cellX < numCellsX; cellX++ ){
            for (Integer cellY = 0; cellY < numCellsY; cellY++){
                Long original = vals[cellY][cellX].getLong();
                Long compressed = compress(original, info.x.currentValue(), info.y.currentValue());
                result.overlay(currentX, currentY, compressed);
                currentY += info.y.cellCompressBitCount();
                info.y.nextCell();
                resultSize += Long.bitCount(compressed);
            }
            currentX += info.x.cellCompressBitCount();
            currentY = 0;
            info.x.nextCell();
            info.y.resetCellIndex();
        }

        result.coordCount = resultSize;
        return result;
    }


    public Boolean contains(Integer x, Integer y) {
        Integer offsetY = y % cellHeight;
        Integer offsetX = x % cellWidth;
        CellValue cellValue = vals[y/ cellHeight][x/ cellWidth];
        Long val = cellValue.getLong();
        return !Objects.equals(0L, val & (1L << (offsetY * cellWidth + offsetX)));
    }

    public Integer count(){
        return coordCount;
    }

    public Boolean add(Integer x, Integer y) {
        Integer changeCount = applyBitOperationAt(x, y, (a, b) -> a|b);
        coordCount += changeCount;
        return changeCount > 0;
    }

    public Boolean remove(Integer x, Integer y) {
        Integer changeCount = applyBitOperationAt(x, y, (a, b) -> (-1L^a)&b);
        coordCount -= changeCount;
        return changeCount > 0;
    }

    public Integer add(Integer x1, Integer y1, Integer x2, Integer y2) {
        Integer changeCount = applyBitOperationInRegion(x1, y1, x2, y2, (a,b) -> a|b);
        coordCount += changeCount;
        return changeCount;
    }

    public Integer remove(Integer x1, Integer y1, Integer x2, Integer y2) {
        Integer changeCount = applyBitOperationInRegion(x1, y1, x2, y2, (a,b) -> (-1L^a)&b);
        coordCount -= changeCount;
        return changeCount;
    }

    public CoordSet intersect(CoordSet set){
        CoordSet result = new CoordSet(set.w, set.h);
        for(Integer y=0; y<numCellsY; y++){
            for (Integer x=0; x< numCellsX; x++){
                Long resultValue = vals[y][x].getLong() & set.vals[y][x].getLong();
                result.vals[y][x].setValue(resultValue);
                result.coordCount += Long.bitCount(resultValue);
            }
        }
        return result;
    }

    public CoordSet union(CoordSet set){
        CoordSet result = new CoordSet(set.w, set.h);
        for(Integer y=0; y<numCellsY; y++){
            for (Integer x=0; x< numCellsX; x++){
                Long resultValue = -1L^((-1L^(vals[y][x].getLong())) & (-1L^(set.vals[y][x].getLong())));
                result.vals[y][x].setValue(resultValue);
                result.coordCount += Long.bitCount(resultValue);
            }
        }
        return result;
    }

    public Long decompress(Long compressedValue, Long xCompressionBit, Long yCompressionBit) {
        Long workingCBit = xCompressionBit | (-1L^row[0]);
        Long decompressedX = 0L;
        Integer currentBit = 0;
        Integer cBitIndex = 0;
        while (currentBit < cellWidth) {
            Long compressedStrip = shift(compressedValue & col[cBitIndex], currentBit - cBitIndex, 0);
            Long decompressed = 0L;
            Integer length = Long.numberOfTrailingZeros(workingCBit) - currentBit + 1;
            for (Integer i=0; i<length; i++){
                decompressed |= shift(compressedStrip, i, 0);
            }
            decompressedX |= decompressed;
            currentBit += length;
            cBitIndex += 1;
            workingCBit &= -1L << currentBit;
        }

        Long result = 0L;
        workingCBit = yCompressionBit | (-1L^row[0]);
        currentBit = 0;
        cBitIndex = 0;
        while (currentBit < cellHeight) {
            Long compressedStrip = shift(decompressedX & row[cBitIndex], 0, currentBit - cBitIndex);
            Long decompressed = 0L;
            Integer length = Long.numberOfTrailingZeros(workingCBit) - currentBit + 1;
            for (Integer i=0; i<length; i++){
                decompressed |= shift(compressedStrip, 0, i);
            }
            result |= decompressed;
            currentBit += length;
            cBitIndex += 1;
            workingCBit &= -1L << currentBit;
        }

        return result;
    }

    public CompressionInfo getCompressionInfo() { return compressionInfo; }

    public CoordSet scale(CompressionInfo info) {
        CoordSet result = new CoordSet(info.originalWidth(), info.originalHeight());

        Long xCompressBit, yCompressBit;
        Integer xCompressBitCount, yCompressBitCount;
        Integer compressedLengthX, compressedLengthY;
        Integer x = 0, y;

        info.reset();
        for (Integer cellX = 0; cellX < result.numCellsX; cellX++) {
            y = 0;
            xCompressBit = info.x.valueAtCell(cellX);
            xCompressBitCount = Long.bitCount(xCompressBit);
            compressedLengthX = xCompressBitCount;
            if (Objects.equals(xCompressBit & (1L << 7), 0L)) {
                compressedLengthX += 1;
            }

            for (Integer cellY = 0; cellY < result.numCellsY; cellY++){
                yCompressBit = info.y.valueAtCell(cellY);
                yCompressBitCount = Long.bitCount(yCompressBit);
                compressedLengthY = yCompressBitCount;
                if (Objects.equals(yCompressBit & (1L << 7), 0L)) {
                    compressedLengthY += 1;
                }
                Long compressed = extract(x, y, compressedLengthX, compressedLengthY);
                Long decompressed = decompress(compressed, xCompressBit, yCompressBit);
                result.vals[cellY][cellX].setValue(decompressed);
                result.coordCount += Long.bitCount(decompressed);
                y += yCompressBitCount;

            }
            x += xCompressBitCount;
        }
        return result;
    }

    Long transpose(Long val, Integer orientation){
        LongUnaryOperator clearLowestOneBit = l -> l^Long.lowestOneBit(l);
        LongUnaryOperator targetBitIndex = orientation == 0 ? i -> 8*(7-(i%8))+i/8
                : i -> (i % 8)*8 + (7-(i/8)) ;
        LongUnaryOperator shiftToTarget = i -> 1L << i;

        return LongStream.iterate(val, clearLowestOneBit)
                .limit(Long.bitCount(val))
                .map(Long::numberOfTrailingZeros)
                .map(targetBitIndex)
                .map(shiftToTarget)
                .boxed()
                .reduce((a,b) -> a|b)
                .orElse(0L);
    }

    public CoordSet transpose(Integer orientation){
        CoordSet result = new CoordSet(h, w);

        for (Integer cellX=0; cellX < result.numCellsX; cellX++){
            for (Integer cellY=0; cellY < result.numCellsY; cellY++){
                Long val = orientation == 0 ? vals[cellX][numCellsX-cellY-1].getLong()
                        : vals[numCellsY-cellX-1][cellY].getLong();

                result.vals[cellY][cellX].setValue(transpose(val, orientation));
            }
        }

        Boolean shiftNeeded = (orientation == 0 ? w : h) % 8 > 0;
        if (shiftNeeded) {
            Integer xShiftAmount = 0;
            Integer yShiftAmount = 0;

            if (orientation == 0) {
                yShiftAmount = cellWidth - w % cellWidth;
            } else {
                xShiftAmount = cellHeight - h % cellHeight;
            }

            for (Integer x=0; x < result.getWidth(); x += cellWidth){
                for (Integer y=0; y < result.getHeight(); y += cellHeight){
                    Integer cellWidth = min(result.getWidth(), x + this.cellWidth) - x;
                    Integer cellHeight = min(result.getHeight(), y + this.cellHeight) - y;
                    Long shiftedVal = result.extract(x + xShiftAmount, y+yShiftAmount, cellWidth, cellHeight);
                    CellValue cellValue = result.vals[y/ this.cellHeight][x/ this.cellWidth];
                    cellValue.setValue(shiftedVal);
                }
            }
        }

        result.coordCount = coordCount;
        return result;
    }

    Long extract(Integer x, Integer y, Integer lengthX, Integer lengthY) {
        Integer offsetX = x % cellWidth;
        Integer offsetY = y % cellHeight;
        Integer cellX = x / cellWidth;
        Integer cellY = y / cellHeight;

        Long value = shift(vals[cellY][cellX].getLong(), -offsetX, -offsetY) ;


        Boolean nextX = cellX < numCellsX - 1 && (offsetX > (cellWidth - lengthX));
        Boolean nextY = cellY < numCellsY - 1 && (offsetY > (cellHeight - lengthY));
        if (nextX) value |= shift(vals[cellY][cellX+1].getLong(), 8-offsetX, -offsetY);
        if (nextY) value |= shift(vals[cellY+1][cellX].getLong(), -offsetX, 8-offsetY);
        if (nextX && nextY) value |= shift(vals[cellY+1][cellX+1].getLong(), 8-offsetX, 8-offsetY);

        if (cellX == numCellsX-1 && (offsetX > (cellWidth - lengthX))) {
            value |= (-1L^value) & (fromX[(w-1)% cellWidth] & toX[(w-1)% cellWidth]);
        }
        if (cellY == numCellsY-1 && (offsetY > (cellHeight - lengthY))) {
            value |= (-1L^value) & (fromY[(h-1)% cellHeight] & toY[(h-1)% cellHeight]);
        }
        return value & toX[lengthX-1] & toY[lengthY-1];
    }

    public void addAllCoords(){
        applyBitOperationInRegion(0, 0, w-1, h-1, (a,b) -> (-1L & a) | b);
        coordCount = w * h;
    }

    public CoordSet filter(Integer x1, Integer y1, Integer x2, Integer y2) {
        CoordSet result = new CoordSet(w, h);
        if (coordCount > 0) {
            for (Integer cellCordY = y1 - y1 % cellHeight; cellCordY <= y2 - y2 % cellHeight; cellCordY += cellHeight) {
                for (Integer cellCoordX = x1 - x1 % cellWidth; cellCoordX <= x2 - x2 % cellWidth; cellCoordX += cellWidth) {
                    Integer startX = Math.max(x1, cellCoordX) - cellCoordX;
                    Integer startY = Math.max(y1, cellCordY) - cellCordY;
                    Integer endX = min(x2, cellCoordX + cellWidth - 1) - cellCoordX;
                    Integer endY = min(y2, cellCordY + cellHeight - 1) - cellCordY;

                    Long mask = fromX[startX] & fromY[startY] & toX[endX] & toY[endY];
                    CellValue cellValue = vals[cellCordY / cellHeight][cellCoordX / cellWidth];
                    CellValue resultCellValue = result.vals[cellCordY / cellHeight][cellCoordX / cellWidth];
                    Long value = mask & cellValue.getLong();
                    resultCellValue.setValue(value);
                    result.coordCount += Long.bitCount(value);
                }
            }
        }
        return result;
    }

    public CoordSet copy(){
        CoordSet result = new CoordSet(w, h);
        result.addAll(this);
        return result;
    }

    public void addAll(CoordSet other) {
        Integer count = 0;
        for(Integer y=0; y< numCellsY; y++){
            for (Integer x=0; x< numCellsX; x++){
                CellValue cellValue = vals[y][x];
                Long beforeVal = cellValue.getLong();
                Long afterVal = beforeVal | other.vals[y][x].getLong();
                cellValue.setValue(afterVal);
                count += Long.bitCount(beforeVal ^ afterVal);
            }
        }
        coordCount += count;
    }

    public Integer removeAll(CoordSet other) {
        Integer count = 0;
        for(Integer y=0; y< numCellsY; y++){
            for (Integer x=0; x< numCellsX; x++){
                CellValue cellValue = vals[y][x];
                CellValue otherCellValue = other.vals[y][x];
                Long beforeVal = cellValue.getLong();
                Long afterVal = beforeVal & (-1L^otherCellValue.getLong());
                cellValue.setValue(afterVal);
                count += Long.bitCount(beforeVal ^ afterVal);
            }
        }
        coordCount -= count;
        return count;
    }

    public <R, S> void project(ProjectTarget<R, S> target, R value){
        for (Integer startY = 0; startY < h; startY += cellHeight) {
            for (Integer startX = 0; startX < w; startX += cellWidth) {
                Integer endY = min(startY + cellHeight, h);
                Integer endX = min(startX + cellWidth, w);
                CellValue cellValue = vals[startY/ cellHeight][startX/ cellWidth];

                for (Integer y = startY; y < endY; y++){
                    Optional<Integer> rangeStartX = Optional.empty();
                    Integer count=0;
                    for (Integer x = startX; x < endX; x++) {
                        Integer i = (y - startY) * cellWidth + x - startX;
                        if (!Objects.equals(0L, (1L<<i) & cellValue.getLong())) {
                            if (!rangeStartX.isPresent()) {
                                rangeStartX = Optional.of(x);
                            }
                            count++;
                        } else {
                            if (rangeStartX.isPresent()) {
                                Integer rangeStart = rangeStartX.get();
                                Integer rangeEnd = rangeStart+count-1;
                                target.updateRange(rangeStart, y, rangeEnd, y, value);
                                rangeStartX = Optional.empty();
                                count = 0;
                            }
                        }
                    }
                    if (rangeStartX.isPresent()) {
                        Integer rangeStart = rangeStartX.get();
                        Integer rangeEnd = rangeStart+count-1;
                        target.updateRange(rangeStart, y, rangeEnd, y, value);
                    }
                }
            }
        }
    }

    public <R> void project(R[][] target, R value){
        for (Integer startY = 0; startY < h; startY += cellHeight) {
            for (Integer startX = 0; startX < w; startX += cellWidth) {
                Integer endX = min(startX + cellWidth, w);
                Integer endY = min(startY + cellHeight, h);
                CellValue cellValue = vals[startY/ cellHeight][startX/ cellWidth];
                for (Integer y = startY; y < endY; y++){
                    for (Integer x = startX; x < endX; x++) {
                        Integer i = (y - startY) * cellWidth + x - startX;
                        if (!Objects.equals(0L, (1L<<i) & cellValue.getLong())) {
                            target[y][x] = value;
                        }
                    }
                }
            }
        }
    }

    private Integer applyBitOperationAt(Integer x, Integer y, LongBinaryOperator operation) {
        Integer cellX = x/ cellWidth;
        Integer cellY = y/ cellHeight;
        CellValue cellValue = vals[cellY][cellX];
        Long mask = 1L << (y%cellHeight)* cellWidth + x% cellWidth;
        Long beforeVal = cellValue.getLong();
        Long afterVal = operation.applyAsLong(mask, beforeVal);
        cellValue.setValue(afterVal);
        return Long.bitCount(beforeVal ^ afterVal);
    }

    private Integer applyBitOperationInRegion(Integer x1, Integer y1, Integer x2, Integer y2, LongBinaryOperator operation) {
        Integer count = 0;

        for (Integer cellCordY = y1 - y1 % cellHeight; cellCordY <= y2 - y2 % cellHeight; cellCordY+= cellHeight) {
            for (Integer cellCoordX = x1 - x1 % cellWidth; cellCoordX <= x2 - x2 % cellWidth; cellCoordX+= cellWidth) {
                Integer startX = Math.max(x1, cellCoordX) - cellCoordX;
                Integer startY = Math.max(y1, cellCordY) - cellCordY;
                Integer endX = min(x2, cellCoordX + cellWidth - 1) - cellCoordX;
                Integer endY = min(y2, cellCordY + cellHeight - 1) - cellCordY;

                CellValue cellValue = vals[cellCordY/ cellHeight][cellCoordX/ cellWidth];
                Long mask = fromX[startX] & fromY[startY] & toX[endX] & toY[endY];
                Long beforeVal = cellValue.getLong();
                Long afterVal = operation.applyAsLong(mask, beforeVal);
                cellValue.setValue(afterVal);
                count += Long.bitCount(beforeVal ^ afterVal);
            }
        }
        return count;
    }

    public String getKey() {
        StringBuilder sb = new StringBuilder();
        sb.append(w+","+h);
        for (Integer cellY = 0; cellY < numCellsY; cellY++){
            sb.append("\n");
            for (Integer cellX = 0; cellX < numCellsX; cellX++){
                if (cellX > 0) {
                    sb.append(",");
                }
                sb.append(vals[cellY][cellX].getLong());
            }
        }
        return Base64.getEncoder().encodeToString(sb.toString().getBytes());
    }

    public String getCellBinaryString(Integer cellX, Integer cellY){
        return format(vals[cellY][cellX].getLong())
                .replace("0"," ")
                .replace("1", "X")
                ;
    }

    Long getAsLong(Integer cellX, Integer cellY) {
        return vals[cellY][cellX].getLong();
    }

    Integer getNumCellsX(){return numCellsX;}
    Integer getNumCellsY(){return numCellsY;}


    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || o.getClass() != this.getClass()) return false;
        CoordSet other = (CoordSet)o;
        if (!Objects.equals(w, other.w) || !Objects.equals(h, other.h)) return false;
        for (Integer y=0; y<numCellsY; y++) {
            for (Integer x = 0; x < numCellsX; x++) {
                if (!Objects.equals(vals[y][x].getLong(), other.vals[y][x].getLong())){
                    return false;
                }
            }
        }
        return true;
    }


    public String format(Long val){
        String binaryString = String.format("%64s", Long.toBinaryString(Long.reverse(val))).replace(' ', '0');
        StringBuilder sb = new StringBuilder();
        for (Integer i=0; i<8; i++){
            if (sb.length()>0){
                sb.append("\n");
            }
            sb.append(binaryString, i*8, i*8+8);
        }
        return sb.toString();
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for (Integer cellY = 0; cellY < numCellsY; cellY++){
            Integer rowH = h - cellY * cellHeight;
            String rows = Arrays.stream(vals[cellY])
                    .map(s-> format(s.getLong()))
                    .map(s -> Arrays.asList(s.split("\n")))
                    .reduce((a,b) -> IntStream.range(0, a.size())
                            .mapToObj(i -> a.get(i).concat(b.get(i)))
                            .collect(Collectors.toList()))
                    .get().stream()
                    .map(s -> s.substring(0, w))
                    .limit(rowH)
                    .collect(Collectors.joining("\n"));
            sb.append(rows).append("\n");
        }
        return sb.toString();
    }

}
