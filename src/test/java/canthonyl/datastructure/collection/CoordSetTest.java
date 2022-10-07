package canthonyl.datastructure.collection;

import canthonyl.fixture.Point;
import canthonyl.fixture.ArrayProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static canthonyl.datastructure.collection.CoordSet.*;
import static org.junit.jupiter.api.Assertions.*;

public class CoordSetTest {

    private CoordSet coordSet;
    private Point[][] point;

    //test coord set 1:
    private Point topFrameStart, topFrameEnd, leftFrameStart, leftFrameEnd, centerFrameStart, centerFrameEnd,
            rightFrameStart, rightFrameEnd, bottomFrameStart, bottomFrameEnd;

    //test coord set 2:
    private Point topHalfStart, topHalfEnd, leftQuarterStart, leftQuarterEnd, rightQuarterStart, rightQuarterEnd;


    @BeforeEach
    public void setup(){
        //test coords set 1 for 100x100
        topFrameStart = Point.of(0, 0);
        topFrameEnd = topFrameStart.add(100-1, 10-1);

        leftFrameStart = Point.of(0, 10);
        leftFrameEnd = leftFrameStart.add(10-1, 80-1);

        centerFrameStart = Point.of(10, 10);
        centerFrameEnd = centerFrameStart.add(80-1, 80-1);

        rightFrameStart = Point.of(90, 10);
        rightFrameEnd = rightFrameStart.add(10-1, 80-1);

        bottomFrameStart = Point.of(0, 90);
        bottomFrameEnd = bottomFrameStart.add(100-1, 10-1);

        //test coords set 2
        topHalfStart = Point.of(0, 0);
        topHalfEnd = topHalfStart.add(100-1, 50-1);

        leftQuarterStart = Point.of(0, 50);
        leftQuarterEnd = leftQuarterStart.add(50-1, 50-1);

        rightQuarterStart = Point.of(50, 50);
        rightQuarterEnd = rightQuarterStart.add(50-1, 50-1);

        point = new Point[100][100];
        for (int x=0; x<100; x++){
            for (int y=0; y<100; y++){
                point[x][y] = Point.of(x,y);
            }
        }
    }

    @Test
    public void setContainsCoordsAfterAdd(){
        coordSet = new CoordSet(100, 100);

        coordSet.add(centerFrameStart.x, centerFrameStart.y, centerFrameEnd.x, centerFrameEnd.y);
        forAllCoordsIn(centerFrameStart, centerFrameEnd, (c, p) -> assertTrue(c.contains(p.x, p.y)));

        forAllCoordsIn(topFrameStart, topFrameEnd, (c, p) -> assertFalse(c.contains(p.x, p.y)));
        forAllCoordsIn(leftFrameStart, leftFrameEnd, (c, p) -> assertFalse(c.contains(p.x, p.y)));
        forAllCoordsIn(rightFrameStart, rightFrameEnd, (c, p) -> assertFalse(c.contains(p.x, p.y)));
        forAllCoordsIn(bottomFrameStart, bottomFrameEnd, (c, p) -> assertFalse(c.contains(p.x, p.y)));
    }

    @Test
    public void setDoesNotContainCoordsAfterRemove(){
        coordSet = new CoordSet(100, 100);
        coordSet.addAllCoords();

        coordSet.remove(centerFrameStart.x, centerFrameStart.y, centerFrameEnd.x, centerFrameEnd.y);
        forAllCoordsIn(centerFrameStart, centerFrameEnd, (c, p) -> assertFalse(c.contains(p.x, p.y)));

        forAllCoordsIn(topFrameStart, topFrameEnd, (c, p) -> assertTrue(c.contains(p.x, p.y)));
        forAllCoordsIn(leftFrameStart, leftFrameEnd, (c, p) -> assertTrue(c.contains(p.x, p.y)));
        forAllCoordsIn(rightFrameStart, rightFrameEnd, (c, p) -> assertTrue(c.contains(p.x, p.y)));
        forAllCoordsIn(bottomFrameStart, bottomFrameEnd, (c, p) -> assertTrue(c.contains(p.x, p.y)));
    }

    @Test
    public void setDoesNotContainCoordsAfterRemoveAll(){
        coordSet = new CoordSet(100, 100);
        coordSet.addAllCoords();
        assertEquals(100*100, coordSet.count());

        CoordSet other = new CoordSet(100, 100);
        other.addAllCoords();
        coordSet.removeAll(other);

        forAllCoordsIn(0, 0, 99, 99, (c, p) -> assertFalse(c.contains(p.x, p.y)));
        assertEquals(0, coordSet.count());
    }

    @Test
    public void filterReturnsCoordsWithinBoundary(){
        CoordSet source = new CoordSet(128, 128);
        source.add(32, 32, 96, 96);
        CoordSet result = source.filter(33, 33, 95, 95);
        assertFalse(result.contains(32, 32));
        assertTrue(result.contains(33, 33));
        assertTrue(result.contains(95, 95));
        assertFalse(result.contains(96, 96));
        assertEquals((95-33+1)*(95-33+1), result.count());
    }

    @Test
    public void countUpdateAfterAdd(){
        coordSet = new CoordSet(100, 100);
        assertEquals(0, coordSet.count());
        assertNotNull(topHalfStart);
        assertEquals(5000, coordSet.add(topHalfStart.x, topHalfStart.y, topHalfEnd.x, topHalfEnd.y));
        assertEquals(5000, coordSet.count());

        assertEquals(2500, coordSet.add(leftQuarterStart.x, leftQuarterStart.y, leftQuarterEnd.x, leftQuarterEnd.y));
        assertEquals(7500, coordSet.count());

        assertEquals(true, coordSet.add(rightQuarterStart.x, rightQuarterStart.y));
        assertEquals(7501, coordSet.count());

        assertEquals(2499, coordSet.add(rightQuarterStart.x, rightQuarterStart.y, rightQuarterEnd.x, rightQuarterEnd.y));
        assertEquals(10000, coordSet.count());
    }

    @Test
    public void countUpdateAfterRemove(){
        coordSet = new CoordSet(100, 100);
        coordSet.addAllCoords();

        assertEquals(10000, coordSet.count());

        assertEquals(5000, coordSet.remove(topHalfStart.x, topHalfStart.y, topHalfEnd.x, topHalfEnd.y));
        assertEquals(5000, coordSet.count());

        assertEquals(2500, coordSet.remove(leftQuarterStart.x, leftQuarterStart.y, leftQuarterEnd.x, leftQuarterEnd.y));
        assertEquals(2500, coordSet.count());

        assertEquals(true, coordSet.remove(rightQuarterStart.x, rightQuarterStart.y));
        assertEquals(2499, coordSet.count());

        assertEquals(2499, coordSet.remove(rightQuarterStart.x, rightQuarterStart.y, rightQuarterEnd.x, rightQuarterEnd.y));
        assertEquals(0, coordSet.count());
    }

    @Test
    public void addReturnsZeroOrFalseWhenAddingExistingCoord(){
        coordSet = new CoordSet(10, 10);
        coordSet.addAllCoords();
        assertEquals(0, coordSet.add(0,0,1,1));
        assertEquals(false, coordSet.add(9,9));
    }

    @Test
    public void removeReturnsZeroOrFalseWhenAddingExistingCoord(){
        coordSet = new CoordSet(10, 10);
        assertEquals(0, coordSet.remove(0,0,1,1));
        assertEquals(false, coordSet.remove(9,9));
    }

    @Test
    public void setIntersection(){
        CoordSet a = new CoordSet(100, 100);
        a.add(topFrameStart.x, topFrameStart.y, centerFrameEnd.x, centerFrameEnd.y);

        CoordSet b = new CoordSet(100, 100);
        b.add(centerFrameStart.x, centerFrameStart.y, bottomFrameEnd.x, bottomFrameEnd.y);

        coordSet = a.intersect(b);
        forAllCoordsIn(centerFrameStart, centerFrameEnd, (set, point) -> assertTrue(set.contains(point.x, point.y)));

        forAllCoordsIn(topFrameStart, topFrameEnd, (set, point) -> assertFalse(set.contains(point.x, point.y)));
        forAllCoordsIn(leftFrameStart, leftFrameEnd, (set, point) -> assertFalse(set.contains(point.x, point.y)));
        forAllCoordsIn(rightFrameStart, rightFrameEnd, (set, point) -> assertFalse(set.contains(point.x, point.y)));
        forAllCoordsIn(bottomFrameStart, bottomFrameEnd, (set, point) -> assertFalse(set.contains(point.x, point.y)));
    }


    @Test
    public void setUnion(){
        CoordSet a = new CoordSet(100, 100);
        a.add(leftFrameStart.x, leftFrameStart.y, centerFrameEnd.x, centerFrameEnd.y);

        CoordSet b = new CoordSet(100, 100);
        b.add(centerFrameStart.x, centerFrameStart.y, rightFrameEnd.x, rightFrameEnd.y);

        coordSet = a.union(b);
        forAllCoordsIn(leftFrameStart, rightFrameEnd, (set, point) -> assertTrue(set.contains(point.x, point.y)));

        forAllCoordsIn(topFrameStart, topFrameEnd, (set, point) -> assertFalse(set.contains(point.x, point.y)));
        forAllCoordsIn(bottomFrameStart, bottomFrameEnd, (set, point) -> assertFalse(set.contains(point.x, point.y)));
    }


    @Test
    public void addAllAddsAllNewElements(){
        coordSet = new CoordSet(100, 100);
        coordSet.add(leftFrameStart.x, leftFrameStart.y, centerFrameEnd.x, centerFrameEnd.y);

        CoordSet b = new CoordSet(100, 100);
        b.add(centerFrameStart.x, centerFrameStart.y, rightFrameEnd.x, rightFrameEnd.y);

        coordSet.addAll(b);
        forAllCoordsIn(leftFrameStart, rightFrameEnd, (set, point) -> assertTrue(set.contains(point.x, point.y)));
        forAllCoordsIn(topFrameStart, topFrameEnd, (set, point) -> assertFalse(set.contains(point.x, point.y)));
        forAllCoordsIn(bottomFrameStart, bottomFrameEnd, (set, point) -> assertFalse(set.contains(point.x, point.y)));
        assertEquals(8000, coordSet.count());
    }

    @Test
    public void removeAll_withCommonCoords(){
        String input =
                "XXXXXXXXXX"+
                "X  XXXX  X"+
                "X XX  XX X"+
                "XXX    XXX"+
                "XX      XX"+
                "XX      XX"+
                "XXX    XXX"+
                "X XX  XX X"+
                "X  XXXX  X"+
                "XXXXXXXXXX";

        String toRemove =
                "          "+
                "   XXXX   "+
                "  XX  XX  "+
                " XX    XX "+
                " X      X "+
                " X      X "+
                " XX    XX "+
                "  XX  XX  "+
                "   XXXX   "+
                "          ";

        String expected =
                "XXXXXXXXXX"+
                "X        X"+
                "X        X"+
                "X        X"+
                "X        X"+
                "X        X"+
                "X        X"+
                "X        X"+
                "X        X"+
                "XXXXXXXXXX";

        CoordSet expectedSet = createCoordSet(expected, 10, 10);
        CoordSet removeSet = createCoordSet(toRemove, 10, 10);
        CoordSet actualSet = createCoordSet(input, 10, 10);
        Long expectedSize = toRemove.chars().filter(c -> c == 'X').count();
        Long actualSize = actualSet.removeAll(removeSet).longValue();
        assertCoordSetEquals(expectedSet, actualSet);
        assertEquals(expectedSize, actualSize);
    }

    @Test
    public void removeAll_disjointSets(){
        String input =
                "XXXXXXXXXX"+
                "X        X"+
                "X        X"+
                "X        X"+
                "X        X"+
                "X        X"+
                "X        X"+
                "X        X"+
                "X        X"+
                "XXXXXXXXXX";

        String toRemove =
                "          "+
                "   XXXX   "+
                "  XX  XX  "+
                " XX    XX "+
                " X      X "+
                " X      X "+
                " XX    XX "+
                "  XX  XX  "+
                "   XXXX   "+
                "          ";


        CoordSet expectedSet = createCoordSet(input, 10, 10);
        CoordSet removeSet = createCoordSet(toRemove, 10, 10);
        CoordSet actualSet = createCoordSet(input, 10, 10);
        Long actualSize = actualSet.removeAll(removeSet).longValue();
        assertCoordSetEquals(expectedSet, actualSet);
        assertEquals(0L, actualSize);
    }


    @Test
    public void retainLeftToRight_EdgeToEdge_withinBucketSize(){
        coordSet = new CoordSet(5, 5);
        coordSet.add(0,0,0,4);
        coordSet.add(2,0,2,4);
        coordSet.add(4,0,4,4);

        forAllCoordsIn(Point.of(0,0), Point.of(0,4), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,0), Point.of(1,4), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(2,0), Point.of(2,4), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(3,0), Point.of(3,4), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(4,0), Point.of(4,4), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));

        coordSet.retain(0, 1, 0, 0, 4, 4);

        forAllCoordsIn(Point.of(0,0), Point.of(0,4), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,0), Point.of(1,4), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(2,0), Point.of(2,4), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(3,0), Point.of(3,4), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(4,0), Point.of(4,4), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));

    }

    @Test
    public void retainLeftToRight_withinBucketSize(){
        coordSet = new CoordSet(5, 5);
        coordSet.add(0,0,0,4);
        coordSet.add(2,0,2,4);
        coordSet.add(4,0,4,4);

        forAllCoordsIn(Point.of(0,0), Point.of(0,4), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,0), Point.of(1,4), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(2,0), Point.of(2,4), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(3,0), Point.of(3,4), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(4,0), Point.of(4,4), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));

        coordSet.retain(0,1, 1, 0, 4, 4);

        forAllCoordsIn(Point.of(0,0), Point.of(0,4), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,0), Point.of(1,4), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(2,0), Point.of(2,4), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(3,0), Point.of(3,4), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(4,0), Point.of(4,4), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));

    }


    @Test
    public void retainLeftToRight_EdgeToEdge(){
        coordSet = new CoordSet(10, 10);
        coordSet.add(1,0,1,9);
        coordSet.add(2,0,2,9);
        coordSet.add(4,0,4,9);
        coordSet.add(5,0,5,9);
        coordSet.add(7,0,7,9);
        coordSet.add(8,0,8,9);

        forAllCoordsIn(Point.of(0,0), Point.of(0,9), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,0), Point.of(1,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(2,0), Point.of(2,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(3,0), Point.of(3,9), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(4,0), Point.of(4,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(5,0), Point.of(5,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(6,0), Point.of(6,9), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(7,0), Point.of(7,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(8,0), Point.of(8,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(9,0), Point.of(9,9), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));

        coordSet.retain(0, 1, 0, 0, 9, 9);

        forAllCoordsIn(Point.of(0,0), Point.of(0,9), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,0), Point.of(1,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(2,0), Point.of(2,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(3,0), Point.of(3,9), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(4,0), Point.of(4,9), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(5,0), Point.of(5,9), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(6,0), Point.of(6,9), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(7,0), Point.of(7,9), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(8,0), Point.of(8,9), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(9,0), Point.of(9,9), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
    }


    @Test
    public void retainTopToBottom_EdgeToEdge(){
        coordSet = new CoordSet(10, 10);
        coordSet.add(0,1,9,1);
        coordSet.add(0,2,9,2);
        coordSet.add(0,4,9,4);
        coordSet.add(0,5,9,5);
        coordSet.add(0,7,9,7);
        coordSet.add(0,8,9,8);

        forAllCoordsIn(point[0][0], point[9][0], (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(point[0][1], point[9][1], (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(point[0][2], point[9][2], (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(point[0][3], point[9][3], (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(point[0][4], point[9][4], (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(point[0][5], point[9][5], (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(point[0][6], point[9][6], (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(point[0][7], point[9][7], (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(point[0][8], point[9][8], (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(point[0][9], point[9][9], (set, point) -> assertEquals(false, set.contains(point.x, point.y)));

        coordSet.retain(1, 1, 0, 0, 9, 9);

        forAllCoordsIn(point[0][0], point[9][0], (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(point[0][1], point[9][1], (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(point[0][2], point[9][2], (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(point[0][3], point[9][3], (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(point[0][4], point[9][4], (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(point[0][5], point[9][5], (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(point[0][6], point[9][6], (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(point[0][7], point[9][7], (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(point[0][8], point[9][8], (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(point[0][9], point[9][9], (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
    }

    @Test
    public void retainOnlyRemovesCoordsInBoundedRegion_EmptyRegion(){
        coordSet = new CoordSet(100, 100);

        coordSet.add(0,0,99,99);
        coordSet.remove(centerFrameStart.x, centerFrameStart.y, centerFrameEnd.x, centerFrameEnd.y);

        coordSet.retain(0, 1, centerFrameStart.x, centerFrameStart.y, centerFrameEnd.x, centerFrameEnd.y);
        coordSet.retain(1, 1, centerFrameStart.x, centerFrameStart.y, centerFrameEnd.x, centerFrameEnd.y);
        coordSet.retain(0, -1, centerFrameStart.x, centerFrameStart.y, centerFrameEnd.x, centerFrameEnd.y);
        coordSet.retain(1, -1, centerFrameStart.x, centerFrameStart.y, centerFrameEnd.x, centerFrameEnd.y);

        forAllCoordsIn(topFrameStart, topFrameEnd, (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(leftFrameStart, leftFrameEnd, (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(centerFrameStart, centerFrameEnd, (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(rightFrameStart, rightFrameEnd, (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(bottomFrameStart, bottomFrameEnd, (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
    }

    @Test
    public void retainLeftToRight_Bounded(){
        coordSet = new CoordSet(10, 10);
        //vertical lines
        coordSet.add(1,0,1,9);
        coordSet.add(2,0,2,9);
        coordSet.add(4,0,4,9);
        coordSet.add(5,0,5,9);
        coordSet.add(7,0,7,9);
        coordSet.add(8,0,8,9);
        //borders
        coordSet.add(0,0,0,9);
        coordSet.add(0,0,9,0);
        coordSet.add(0,9,9,9);
        coordSet.add(9,0,9,9);

        forAllCoordsIn(Point.of(1,1), Point.of(1,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(2,1), Point.of(2,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(3,1), Point.of(3,8), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(4,1), Point.of(4,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(5,1), Point.of(5,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(6,1), Point.of(6,8), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(7,1), Point.of(7,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(8,1), Point.of(8,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));

        coordSet.retain(0, 1, 1, 1, 8, 8);

        forAllCoordsIn(Point.of(1,1), Point.of(1,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(2,1), Point.of(2,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(3,1), Point.of(3,8), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(4,1), Point.of(4,8), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(5,1), Point.of(5,8), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(6,1), Point.of(6,8), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(7,1), Point.of(7,8), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(8,1), Point.of(8,8), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));

        forAllCoordsIn(Point.of(0,0),Point.of(0,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(0,0),Point.of(9,0), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(0,9),Point.of(9,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(9,0),Point.of(9,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
    }


    @Test
    public void retainRightToLeft_Bounded(){
        coordSet = new CoordSet(10, 10);
        //vertical lines
        coordSet.add(1,0,1,9);
        coordSet.add(2,0,2,9);
        coordSet.add(4,0,4,9);
        coordSet.add(5,0,5,9);
        coordSet.add(7,0,7,9);
        coordSet.add(8,0,8,9);
        //borders
        coordSet.add(0,0,0,9);
        coordSet.add(0,0,9,0);
        coordSet.add(0,9,9,9);
        coordSet.add(9,0,9,9);

        forAllCoordsIn(Point.of(1,1), Point.of(1,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(2,1), Point.of(2,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(3,1), Point.of(3,8), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(4,1), Point.of(4,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(5,1), Point.of(5,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(6,1), Point.of(6,8), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(7,1), Point.of(7,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(8,1), Point.of(8,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));

        coordSet.retain(0, -1, 1, 1, 8, 8);

        forAllCoordsIn(Point.of(1,1), Point.of(1,8), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(2,1), Point.of(2,8), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(3,1), Point.of(3,8), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(4,1), Point.of(4,8), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(5,1), Point.of(5,8), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(6,1), Point.of(6,8), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(7,1), Point.of(7,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(8,1), Point.of(8,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));

        forAllCoordsIn(Point.of(0,0),Point.of(0,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(0,0),Point.of(9,0), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(0,9),Point.of(9,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(9,0),Point.of(9,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
    }

    @Test
    public void retainTopToBottom_Bounded(){
        coordSet = new CoordSet(10, 10);
        //horizontal lines
        coordSet.add(1,1,8,1);
        coordSet.add(1,2,8,2);
        coordSet.add(1,4,8,4);
        coordSet.add(1,5,8,5);
        coordSet.add(1,7,8,7);
        coordSet.add(1,8,8,8);
        //borders
        coordSet.add(0,0,0,9);
        coordSet.add(0,0,9,0);
        coordSet.add(0,9,9,9);
        coordSet.add(9,0,9,9);

        forAllCoordsIn(Point.of(1,1), Point.of(8,1), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,2), Point.of(8,2), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,3), Point.of(8,3), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,4), Point.of(8,4), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,5), Point.of(8,5), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,6), Point.of(8,6), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,7), Point.of(8,7), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,8), Point.of(8,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));

        coordSet.retain(1, 1, 1, 1, 8, 8);

        forAllCoordsIn(Point.of(1,1), Point.of(8,1), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,2), Point.of(8,2), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,3), Point.of(8,3), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,4), Point.of(8,4), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,5), Point.of(8,5), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,6), Point.of(8,6), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,7), Point.of(8,7), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,8), Point.of(8,8), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));

        forAllCoordsIn(Point.of(0,0),Point.of(0,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(0,0),Point.of(9,0), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(0,9),Point.of(9,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(9,0),Point.of(9,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
    }

    @Test
    public void retainBottomToTop_Bounded(){
        coordSet = new CoordSet(10, 10);
        //horizontal lines
        coordSet.add(1,1,8,1);
        coordSet.add(1,2,8,2);
        coordSet.add(1,4,8,4);
        coordSet.add(1,5,8,5);
        coordSet.add(1,7,8,7);
        coordSet.add(1,8,8,8);
        //borders
        coordSet.add(0,0,0,9);
        coordSet.add(0,0,9,0);
        coordSet.add(0,9,9,9);
        coordSet.add(9,0,9,9);

        forAllCoordsIn(Point.of(1,1), Point.of(8,1), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,2), Point.of(8,2), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,3), Point.of(8,3), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,4), Point.of(8,4), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,5), Point.of(8,5), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,6), Point.of(8,6), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,7), Point.of(8,7), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,8), Point.of(8,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));

        coordSet.retain(1, -1, 1, 1, 8, 8);

        forAllCoordsIn(Point.of(1,1), Point.of(8,1), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,2), Point.of(8,2), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,3), Point.of(8,3), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,4), Point.of(8,4), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,5), Point.of(8,5), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,6), Point.of(8,6), (set, point) -> assertEquals(false, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,7), Point.of(8,7), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(1,8), Point.of(8,8), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));

        forAllCoordsIn(Point.of(0,0),Point.of(0,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(0,0),Point.of(9,0), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(0,9),Point.of(9,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
        forAllCoordsIn(Point.of(9,0),Point.of(9,9), (set, point) -> assertEquals(true, set.contains(point.x, point.y)));
    }

    @Test
    public void retainLeftToRight_boundedCircle(){

        String input =
                "XXXXXXXXXX"+
                "X  XXXX  X"+
                "X XX  XX X"+
                "XXX    XXX"+
                "XX      XX"+
                "XX      XX"+
                "XXX    XXX"+
                "X XX  XX X"+
                "X  XXXX  X"+
                "XXXXXXXXXX";

        String expected =
                "XXXXXXXXXX"+
                "X  XXXX  X"+
                "X XX     X"+
                "XXX      X"+
                "XX       X"+
                "XX       X"+
                "XXX      X"+
                "X XX     X"+
                "X  XXXX  X"+
                "XXXXXXXXXX";

        CoordSet expectedSet = createCoordSet(expected, 10, 10);
        CoordSet actualSet = createCoordSet(input, 10, 10);
        actualSet.retain(0, 1, 1, 1, 8,8);
        assertCoordSetEquals(expectedSet, actualSet);
    }


    @Test
    public void retainIntoLeftToRight_boundedCircle(){

        String input =
                "          "+
                "   XXXX   "+
                "  XX  XX  "+
                " XX    XX "+
                "XX      XX"+
                "XX      XX"+
                " XX    XX "+
                "  XX  XX  "+
                "   XXXX   "+
                "          ";

        String expected =
                "          "+
                "   XXXX   "+
                "  XX      "+
                " XX       "+
                "XX        "+
                "XX        "+
                " XX       "+
                "  XX      "+
                "   XXXX   "+
                "          ";

        CoordSet inputSet = createCoordSet(input, 10, 10);
        CoordSet unchangedInputSet = createCoordSet(input, 10, 10);
        CoordSet expectedSet = createCoordSet(expected, 10, 10);
        CoordSet actualSet = inputSet.retain(0, 1);
        assertCoordSetEquals(expectedSet, actualSet);
        assertCoordSetEquals(unchangedInputSet, inputSet);
    }

    @Test
    public void retainRightToLeft_boundedCircle(){

        String input =
                "XXXXXXXXXX"+
                "X  XXXX  X"+
                "X XX  XX X"+
                "XXX    XXX"+
                "XX      XX"+
                "XX      XX"+
                "XXX    XXX"+
                "X XX  XX X"+
                "X  XXXX  X"+
                "XXXXXXXXXX";

        String expected =
                "XXXXXXXXXX"+
                "X  XXXX  X"+
                "X     XX X"+
                "X      XXX"+
                "X       XX"+
                "X       XX"+
                "X      XXX"+
                "X     XX X"+
                "X  XXXX  X"+
                "XXXXXXXXXX";

        CoordSet expectedSet = createCoordSet(expected, 10, 10);
        CoordSet actualSet = createCoordSet(input, 10, 10);
        actualSet.retain(0, -1, 1, 1, 8,8);
        assertCoordSetEquals(expectedSet, actualSet);
    }


    @Test
    public void retainTopToBottom_boundedCircle(){

        String input =
                "XXXXXXXXXX"+
                "X  XXXX  X"+
                "X XX  XX X"+
                "XXX    XXX"+
                "XX      XX"+
                "XX      XX"+
                "XXX    XXX"+
                "X XX  XX X"+
                "X  XXXX  X"+
                "XXXXXXXXXX";

        String expected =
                "XXXXXXXXXX"+
                "X  XXXX  X"+
                "X XX  XX X"+
                "XXX    XXX"+
                "XX      XX"+
                "XX      XX"+
                "XX      XX"+
                "X        X"+
                "X        X"+
                "XXXXXXXXXX";

        CoordSet expectedSet = createCoordSet(expected, 10, 10);
        CoordSet actualSet = createCoordSet(input, 10, 10);
        actualSet.retain(1, 1, 1, 1, 8,8);
        assertCoordSetEquals(expectedSet, actualSet);
    }

    @Test
    public void retainBottomToTop_boundedCircle(){

        String input =
                "XXXXXXXXXX"+
                "X  XXXX  X"+
                "X XX  XX X"+
                "XXX    XXX"+
                "XX      XX"+
                "XX      XX"+
                "XXX    XXX"+
                "X XX  XX X"+
                "X  XXXX  X"+
                "XXXXXXXXXX";

        String expected =
                "XXXXXXXXXX"+
                "X        X"+
                "X        X"+
                "XX      XX"+
                "XX      XX"+
                "XX      XX"+
                "XXX    XXX"+
                "X XX  XX X"+
                "X  XXXX  X"+
                "XXXXXXXXXX";

        CoordSet expectedSet = createCoordSet(expected, 10, 10);
        CoordSet actualSet = createCoordSet(input, 10, 10);
        actualSet.retain(1, -1, 1, 1, 8,8);
        assertCoordSetEquals(expectedSet, actualSet);
    }

    @Test
    public void retainRemovesRightEdge(){
        String input =
                "X  X  X"+
                "X  X  X"+
                "XXXXXXX";

        String expectedStr =
                "X      "+
                "X      "+
                "XXXXXXX";

        CoordSet expected = createCoordSet(expectedStr, 7, 3);
        CoordSet actual = createCoordSet(input, 7, 3);
        actual.retain(0, 1, 0, 0, 7-1, 3-1);
        assertEquals(true, Objects.equals(expected, actual));
    }


    @Test
    public void coordSetEqualsIfBothContainSameCoords(){
        CoordSet setA = new CoordSet(10, 10);
        CoordSet setB = new CoordSet(10, 10);

        setA.add(1,1,3,3);
        setA.add(5,5,7,7);
        setB.add(5,5,7,7);
        setB.add(1,1,3,3);

        assertEquals(true, setA.equals(setB));
        assertEquals(true, setB.equals(setA));

    }

    @Test
    public void coordSetNotEqualsIfOneContainsDiffCoords(){
        CoordSet setA = new CoordSet(10, 10);
        CoordSet setB = new CoordSet(10, 10);

        setA.add(1,1,3,3);
        setA.add(5,5,7,7);
        setB.add(5,5,7,7);
        setB.add(1,1,3,3);
        setA.add(9, 9);

        assertEquals(false, setA.equals(setB));
        assertEquals(false, setB.equals(setA));

    }

    @Test
    public void fullyContainsChecksIfCurrentSetContainsAllCoordsFromTargetSet(){
        String outer =
                "          "+
                " XX XX XX "+
                "  XXXXXX  "+
                "   X  X   "+
                "XXXXXXXXXX";
        String inner =
                "          "+
                " X  XX  X "+
                "  XXX XX  "+
                "   X  X   "+
                "X X X X XX";

        CoordSet outerSet = createCoordSet(outer, 10, 5);
        CoordSet innerSet = createCoordSet(inner, 10, 5);

        assertEquals(true, outerSet.containsAllCoordsIn(innerSet));
        assertEquals(false, innerSet.containsAllCoordsIn(outerSet));
    }

    @Test
    public void containsAllCoord(){
        CoordSet a = new CoordSet(4, 4);
        a.add(0,0);
        a.add(1,0);
        a.add(0,1);

        CoordSet b = new CoordSet(4, 4);
        b.add(1,0);
        b.add(0,1);

        Boolean result = a.containsAllCoordsIn(b);
        assertEquals(true, result);
    }

    @Test
    public void totalDisjointSetReturnsFalseForFullyContains(){
        String a =
                " XX XX XX "+
                "          "+
                "  XXXXXX  "+
                "          "+
                "XXXXXXXXXX";
        String b =
                "          "+
                " XX XX XX "+
                "XX      XX"+
                "   X  X   "+
                "          ";

        CoordSet setA = createCoordSet(a, 10, 5);
        CoordSet setB = createCoordSet(b, 10, 5);

        assertEquals(false, setA.containsAllCoordsIn(setB));
        assertEquals(false, setB.containsAllCoordsIn(setA));
    }


    @Test
    public void containsAny_setsWithAtLeastOneIntersectingCoords(){
        String a =
                " XX XX XX "+
                "          "+
                "  XXXXXX  "+
                "          "+
                "XXXXXXXXXX";
        String b =
                "          "+
                " XX XX XX "+
                "XX      XX"+
                "   X  X   "+
                "         X";

        CoordSet setA = createCoordSet(a, 10, 5);
        CoordSet setB = createCoordSet(b, 10, 5);

        assertEquals(true, setA.containsAnyCoordsIn(setB));
        assertEquals(true, setB.containsAnyCoordsIn(setA));
    }

    @Test
    public void containsAny_disjointSetsWithNoIntersectingCoords(){
        String a =
                " XX XX XX "+
                "          "+
                "  XXXXXX  "+
                "          "+
                "XXXXXXXXXX";
        String b =
                "          "+
                " XX XX XX "+
                "XX      XX"+
                "   X  X   "+
                "          ";

        CoordSet setA = createCoordSet(a, 10, 5);
        CoordSet setB = createCoordSet(b, 10, 5);

        assertEquals(false, setA.containsAnyCoordsIn(setB));
        assertEquals(false, setB.containsAnyCoordsIn(setA));
    }


    @Test
    public void sameSetCoordsFullyContains(){
        String input =
                "XXXXXXXXXX"+
                "X  XXXX  X"+
                "X XX  XX X"+
                "XXX    XXX"+
                "XX      XX"+
                "XX      XX"+
                "XXX    XXX"+
                "X XX  XX X"+
                "X  XXXX  X"+
                "XXXXXXXXXX";


        CoordSet setA = createCoordSet(input, 10, 10);
        CoordSet setB = createCoordSet(input, 10, 10);

        assertEquals(true, setA.containsAllCoordsIn(setB));
        assertEquals(true, setB.containsAllCoordsIn(setA));
    }

    @Test
    public void key(){
        CoordSet set = new CoordSet(10, 10);
        set.add(1,1,8,8);
        String key = set.getKey();
        System.out.println(key);
        CoordSet set2 = new CoordSet(key);
        assertEquals(true, Objects.equals(set, set2));
    }

    @Test
    public void lineCompactedToPoint(){
        String horizontalLineStr =
                "     "+
                "     "+
                " XXX "+
                "     "+
                "     ";

        String verticalLineStr =
                "     "+
                "  X  "+
                "  X  "+
                "  X  "+
                "     ";

        String expectedStr =
                "   "+
                " X "+
                "   ";

        CoordSet horizontalLine = createCoordSet(horizontalLineStr, 5, 5);
        CoordSet verticalLine = createCoordSet(verticalLineStr, 5, 5);

        CoordSet actualHLineCompacted = horizontalLine.compact();
        CoordSet actualVLineCompacted = verticalLine.compact();

        CoordSet expected = createCoordSet(expectedStr, 3, 3);
        assertEquals(true, Objects.equals(expected, actualHLineCompacted));
        assertEquals(true, Objects.equals(expected, actualVLineCompacted));

    }

    @Test
    public void rectangleReducedTo3x3(){
        String rectangleStr =
                "        "+
                " XXXXXX "+
                " X    X "+
                " X    X "+
                " XXXXXX "+
                "        ";

        String expectedStr =
                "     "+
                " XXX "+
                " X X "+
                " XXX "+
                "     ";

        CoordSet expected = createCoordSet(expectedStr, 5, 5);
        CoordSet rectangleCompacted = createCoordSet(rectangleStr, 8, 6);
        CoordSet compacted = rectangleCompacted.compact();

        assertEquals(true, compacted.containsAllCoordsIn(expected));

    }

    @Test
    public void expand(){
        CoordSet test = new CoordSet(10,10);
        test.add(1,0,1,9);
        test.add(3,0,3,9);
        test.add(5,0,5,9);
        test.add(7,0,7,9);
        test.add(9,0,9,9);
        test.add(8,0, 9,9);
        test.add(0,8, 9,9);
        Long result = test.extract(3,3,7, 7);
        System.out.println("Result = \n"+format(result));
    }

    @Test
    public void continuousRectangularRegionScaledBackFromPoint(){
        CoordSet input = new CoordSet(10, 10);
        input.add(0,0,9,9);

        CoordSet compacted = input.compact();
        CoordSet scaled = compacted.scale(input.getCompressionInfo());
        assertEquals(true, Objects.equals(input, scaled));


        CoordSet input2 = new CoordSet(1, 1);
        input2.add(0, 0);
        CoordSet scaled2 = input2.scale(input.getCompressionInfo());
        assertEquals(true, Objects.equals(input, scaled2));
    }

    @Test
    public void scaledBackRectangle(){
        String compactedStr =
                "     "+
                " XXX "+
                " X X "+
                " XXX "+
                "     ";

        String rectangleAStr =
                "              "+
                " XXXXXXXXXXXX "+
                " X          X "+
                " X          X "+
                " XXXXXXXXXXXX "+
                "              ";

        String rectangleBStr =
                "          "+
                " XXXXXXXX "+
                " X      X "+
                " X      X "+
                " X      X "+
                " X      X "+
                " X      X "+
                " X      X "+
                " XXXXXXXX "+
                "          ";


        CoordSet compacted = createCoordSet(compactedStr, 5, 5);
        CoordSet rectangleA = createCoordSet(rectangleAStr, 14, 6);
        CoordSet rectangleB = createCoordSet(rectangleBStr, 10, 10);

        rectangleA.compact();
        rectangleB.compact();

        CoordSet actualRectangleA = compacted.scale(rectangleA.getCompressionInfo());
        CoordSet actualRectangleB = compacted.scale(rectangleB.getCompressionInfo());

        assertEquals(true, Objects.equals(rectangleA, actualRectangleA));
        assertEquals(true, Objects.equals(rectangleB, actualRectangleB));
    }

    @Test
    public void discreteRegionsScaledBackToSize(){

        String allSquaresStr =
                "                "+
                " XXX XXXX XXXXX "+
                " XXX XXXX XXXXX "+
                " XXX XXXX XXXXX "+
                "     XXXX XXXXX "+
                "          XXXXX "+
                "                ";

        String squareAStr =
                "                "+
                " XXX            "+
                " XXX            "+
                " XXX            "+
                "                "+
                "                "+
                "                ";

        String squareBStr =
                "                "+
                "     XXXX       "+
                "     XXXX       "+
                "     XXXX       "+
                "     XXXX       "+
                "                "+
                "                ";

        String squareCStr =
                "                "+
                "          XXXXX "+
                "          XXXXX "+
                "          XXXXX "+
                "          XXXXX "+
                "          XXXXX "+
                "                ";

        String squareACompactedStr =
                "       "+
                " X     "+
                "       "+
                "       "+
                "       ";

        String squareBCompactedStr =
                "       "+
                "   X   "+
                "   X   "+
                "       "+
                "       ";

        String squareCCompactedStr =
                "       "+
                "     X "+
                "     X "+
                "     X "+
                "       ";

        CoordSet original = createCoordSet(allSquaresStr, 16, 7);
        original.compact();

        CoordSet expectedSquareA = createCoordSet(squareAStr, 16, 7);
        CoordSet expectedSquareB = createCoordSet(squareBStr, 16, 7);
        CoordSet expectedSquareC = createCoordSet(squareCStr, 16, 7);

        CoordSet squareACompacted = createCoordSet(squareACompactedStr, 7, 5);
        CoordSet squareBCompacted = createCoordSet(squareBCompactedStr, 7, 5);
        CoordSet squareCCompacted = createCoordSet(squareCCompactedStr, 7, 5);

        CoordSet actualSquareAScaled = squareACompacted.scale(original.getCompressionInfo());
        CoordSet actualSquareBScaled = squareBCompacted.scale(original.getCompressionInfo());
        CoordSet actualSquareCScaled = squareCCompacted.scale(original.getCompressionInfo());

        assertEquals(true, Objects.equals(expectedSquareA, actualSquareAScaled));
        assertEquals(true, Objects.equals(expectedSquareB, actualSquareBScaled));
        assertEquals(true, Objects.equals(expectedSquareC, actualSquareCScaled));
    }


    @Test
    public void filledRectangleCompactedToPoint(){
        String filledRectangleStr =
                "      "+
                " XXXX "+
                " XXXX "+
                " XXXX "+
                "      ";

        String expectedStr =
                "   "+
                " X "+
                "   ";

        CoordSet expected = createCoordSet(expectedStr, 3, 3);

        CoordSet filledRectangle = createCoordSet(filledRectangleStr, 6, 5);
        CoordSet actual = filledRectangle.compact();

        assertEquals(true, Objects.equals(expected, actual));
    }

    @Test
    public void compactReducesGridWithThicknessToSingleLine(){
        String poundStr =
                "  XX    XX  "+
                "  XX    XX  "+
                "XXXXXXXXXXXX"+
                "XXXXXXXXXXXX"+
                "  XX    XX  "+
                "  XX    XX  "+
                "XXXXXXXXXXXX"+
                "XXXXXXXXXXXX"+
                "  XX    XX  "+
                "  XX    XX  ";

        String expectedStr =
                " X X "+
                "XXXXX"+
                " X X "+
                "XXXXX"+
                " X X ";

        CoordSet input = createCoordSet(poundStr, 12, 10);
        CoordSet expectedSet = createCoordSet(expectedStr, 5, 5);

        coordSet = input.compact();
        assertEquals(true, coordSet.containsAllCoordsIn(expectedSet));
        forAllCoordsExcept(Point.of(0,0), Point.of(4, 4), expectedSet, (set, p) -> assertEquals(false, set.contains(p.x, p.y)));
    }

    @Test
    public void distinctFeaturesRetained(){
        String inputStr =
                "   XX    "+
                " XXXXXXX "+
                " X     X "+
                " X     X "+
                " XXXXXXX "+
                "   XX    ";

        String expectedStr =
                "   X   "+
                " XXXXX "+
                " X   X "+
                " XXXXX "+
                "   X   ";

        CoordSet input = createCoordSet(inputStr, 9, 6);
        CoordSet expectedSet = createCoordSet(expectedStr, 7, 5);

        coordSet = input.compact();
        assertEquals(true, coordSet.containsAllCoordsIn(expectedSet));
        forAllCoordsExcept(Point.of(0,0), Point.of(6, 4), expectedSet, (set, p) -> assertEquals(false, set.contains(p.x, p.y)));
    }

    @Test
    public void compactReducesRepeatedLengthToSingleColumnAndRow(){
        String inputStr =
                "XX XX"+
                "XXXXX"+
                "XXXXX"+
                "XX XX";

        String expectedStr =
                "X X"+
                "XXX"+
                "X X";

        CoordSet inputSet = createCoordSet(inputStr, 5, 4);
        CoordSet expected = createCoordSet(expectedStr, 3, 3);
        CoordSet actual = inputSet.compact();
        assertEquals(true, Objects.equals(expected, actual));
    }


    @Test
    public void compactNestedRectangle(){
        String inputStr =
                "XXXXXXXX" +
                "XXXXXXXX" +
                "XX   XXX" +
                "XX X XXX" +
                "XX   XXX" +
                "XXXXXXXX" +
                "XXXXXXXX" +
                "XXXXXXXX";

        String expectedStr =
                "XXXXX" +
                "X   X" +
                "X X X" +
                "X   X" +
                "XXXXX";

        CoordSet expected = createCoordSet(expectedStr, 5, 5);
        CoordSet input = createCoordSet(inputStr, 8, 8);
        CoordSet actual = input.compact();

        assertEquals(true, Objects.equals(expected, actual));

    }

    @Test
    public void overlayUpdatesAdjacentBlocks(){
        CoordSet overlayAtOrigin = new CoordSet(16, 16);
        CoordSet overlayAtX1Y1 = new CoordSet(16, 16);
        CoordSet overlayAtX2Y2 = new CoordSet(16, 16);
        CoordSet overlayAtX3Y3 = new CoordSet(16, 16);
        CoordSet overlayAtX4Y4 = new CoordSet(16, 16);
        CoordSet overlayAtX5Y5 = new CoordSet(16, 16);
        CoordSet overlayAtX6Y6 = new CoordSet(16, 16);
        CoordSet overlayAtX7Y7 = new CoordSet(16, 16);
        CoordSet overlayAtX8Y8 = new CoordSet(16, 16);
        CoordSet overlayAtX9Y9 = new CoordSet(16, 16);
        CoordSet overlayAtX10Y10 = new CoordSet(16, 16);
        CoordSet overlayAtX11Y11 = new CoordSet(16, 16);
        CoordSet overlayAtX12Y12 = new CoordSet(16, 16);
        CoordSet overlayAtX13Y13 = new CoordSet(16, 16);
        CoordSet overlayAtX14Y14 = new CoordSet(16, 16);
        CoordSet overlayAtX15Y15 = new CoordSet(16, 16);

        overlayAtOrigin.overlay(0, 0, -1L);
        overlayAtX1Y1.overlay(1, 1, -1L);
        overlayAtX2Y2.overlay(2, 2, -1L);
        overlayAtX3Y3.overlay(3, 3, -1L);
        overlayAtX4Y4.overlay(4, 4, -1L);
        overlayAtX5Y5.overlay(5, 5, -1L);
        overlayAtX6Y6.overlay(6, 6, -1L);
        overlayAtX7Y7.overlay(7, 7, -1L);
        overlayAtX8Y8.overlay(8, 8, -1L);
        overlayAtX9Y9.overlay(9, 9, -1L);
        overlayAtX10Y10.overlay(10, 10, -1L);
        overlayAtX11Y11.overlay(11, 11, -1L);
        overlayAtX12Y12.overlay(12, 12, -1L);
        overlayAtX13Y13.overlay(13, 13, -1L);
        overlayAtX14Y14.overlay(14, 14, -1L);
        overlayAtX15Y15.overlay(15, 15, -1L);

        forAllCoordsTrueIfInRange(Point.of(0,0), Point.of(7,7), overlayAtOrigin, (s, p) -> s.contains(p.x, p.y));
        forAllCoordsTrueIfInRange(Point.of(1,1), Point.of(8,8), overlayAtX1Y1, (s, p) -> s.contains(p.x, p.y));
        forAllCoordsTrueIfInRange(Point.of(2,2), Point.of(9,9), overlayAtX2Y2, (s, p) -> s.contains(p.x, p.y));
        forAllCoordsTrueIfInRange(Point.of(3,3), Point.of(10,10), overlayAtX3Y3, (s, p) -> s.contains(p.x, p.y));
        forAllCoordsTrueIfInRange(Point.of(4,4), Point.of(11,11), overlayAtX4Y4, (s, p) -> s.contains(p.x, p.y));
        forAllCoordsTrueIfInRange(Point.of(5,5), Point.of(12,12), overlayAtX5Y5, (s, p) -> s.contains(p.x, p.y));
        forAllCoordsTrueIfInRange(Point.of(6,6), Point.of(13,13), overlayAtX6Y6, (s, p) -> s.contains(p.x, p.y));
        forAllCoordsTrueIfInRange(Point.of(7,7), Point.of(14,14), overlayAtX7Y7, (s, p) -> s.contains(p.x, p.y));
        forAllCoordsTrueIfInRange(Point.of(8,8), Point.of(15,15), overlayAtX8Y8, (s, p) -> s.contains(p.x, p.y));
        forAllCoordsTrueIfInRange(Point.of(9,9), Point.of(15,15), overlayAtX9Y9, (s, p) -> s.contains(p.x, p.y));
        forAllCoordsTrueIfInRange(Point.of(10,10), Point.of(15,15), overlayAtX10Y10, (s, p) -> s.contains(p.x, p.y));
        forAllCoordsTrueIfInRange(Point.of(11,11), Point.of(15,15), overlayAtX11Y11, (s, p) -> s.contains(p.x, p.y));
        forAllCoordsTrueIfInRange(Point.of(12,12), Point.of(15,15), overlayAtX12Y12, (s, p) -> s.contains(p.x, p.y));
        forAllCoordsTrueIfInRange(Point.of(13,13), Point.of(15,15), overlayAtX13Y13, (s, p) -> s.contains(p.x, p.y));
        forAllCoordsTrueIfInRange(Point.of(14,14), Point.of(15,15), overlayAtX14Y14, (s, p) -> s.contains(p.x, p.y));
        forAllCoordsTrueIfInRange(Point.of(15,15), Point.of(15,15), overlayAtX15Y15, (s, p) -> s.contains(p.x, p.y));


    }

    @Test
    public void compactWithSpace(){
        String inputStr =
                "        "+
                " XXXXXX "+
                " XXXXXX "+
                " XX     "+
                " XX  XX "+
                " XX  XX "+
                "        ";
        CoordSet input = createCoordSet(inputStr, 8, 7);
        CoordSet compacted = input.compact();

        System.out.println("Width = "+compacted.getWidth());
        System.out.println("Height = "+compacted.getHeight());
        System.out.println(compacted.getCellBinaryString(0,0));

    }

    @Test
    public void extractReturnsBlockStartingAtCoord(){
        CoordSet set = new CoordSet(16, 16);
        set.add(4, 4, 11, 11);

        Long patternAtOrigin = CoordSet.fromX[4] & CoordSet.fromY[4];
        Long patternAtOffsetX1Y1 = CoordSet.fromX[3] & CoordSet.fromY[3];
        Long patternAtOffsetX2Y2 = CoordSet.fromX[2] & CoordSet.fromY[2];
        Long patternAtOffsetX3Y3 = CoordSet.fromX[1] & CoordSet.fromY[1];
        Long patternAtOffsetX4Y4 = CoordSet.fromX[0] & CoordSet.fromY[0];
        Long patternAtOffsetX5Y5 = CoordSet.toX[6] & CoordSet.toY[6];
        Long patternAtOffsetX6Y6 = CoordSet.toX[5] & CoordSet.toY[5];
        Long patternAtOffsetX7Y7 = CoordSet.toX[4] & CoordSet.toY[4];
        Long patternAtOffsetX8Y8 = CoordSet.toX[3] & CoordSet.toY[3];

        assertEquals(set.extract(0,0), patternAtOrigin);
        assertEquals(set.extract(1,1), patternAtOffsetX1Y1);
        assertEquals(set.extract(2,2), patternAtOffsetX2Y2);
        assertEquals(set.extract(3,3), patternAtOffsetX3Y3);
        assertEquals(set.extract(4,4), patternAtOffsetX4Y4);
        assertEquals(set.extract(5,5), patternAtOffsetX5Y5);
        assertEquals(set.extract(6,6), patternAtOffsetX6Y6);
        assertEquals(set.extract(7,7), patternAtOffsetX7Y7);
        assertEquals(set.extract(8,8), patternAtOffsetX8Y8);
    }

    @Test
    public void transposeHorizontallyCoordSet(){
        CoordSet set = new CoordSet(16, 38);
        set.add(4, 4, 11, 35);
        System.out.println(set);

        CoordSet expectedSet = new CoordSet(38, 16);
        expectedSet.add(4, 4, 35, 11);
        System.out.println(expectedSet);

        CoordSet actualSet = set.transpose(0);
        assertEquals(true, Objects.equals(expectedSet, actualSet));
    }

    @Test
    public void transposeHorizontallyCoordSetWidthLonger(){
        CoordSet set = new CoordSet(38, 16);
        set.add(4, 4, 35, 11);
        System.out.println(set);

        CoordSet expectedSet = new CoordSet(16, 38);
        expectedSet.add(4, 4, 11, 35);
        System.out.println(expectedSet);

        CoordSet actualSet = set.transpose(1);
        assertEquals(true, Objects.equals(expectedSet, actualSet));
    }

    @Test
    public void transposeVerticallyCoordSet(){
        CoordSet set = new CoordSet(16, 38);
        set.add(4, 4, 11, 35);
        System.out.println(set);

        CoordSet expectedSet = new CoordSet(38, 16);
        expectedSet.add(2, 4, 33, 11);
        System.out.println(expectedSet);

        CoordSet actualSet = set.transpose(1);
        assertEquals(true, Objects.equals(expectedSet, actualSet));
    }

    @Test
    public void transposeSingleGridHorizontalTest(){
        CoordSet set = new CoordSet(8,8);
        for (Integer i=0; i<8; i++) {
            assertEquals(1L << 8 * 7+i, set.transpose(1L << 8*i+0, 0));
            assertEquals(1L << 8 * 6+i, set.transpose(1L << 8*i+1, 0));
            assertEquals(1L << 8 * 5+i, set.transpose(1L << 8*i+2, 0));
            assertEquals(1L << 8 * 4+i, set.transpose(1L << 8*i+3,0));
            assertEquals(1L << 8 * 3+i, set.transpose(1L << 8*i+4, 0));
            assertEquals(1L << 8 * 2+i, set.transpose(1L << 8*i+5, 0));
            assertEquals(1L << 8 * 1+i, set.transpose(1L << 8*i+6, 0));
            assertEquals(1L << 8 * 0+i, set.transpose(1L << 8*i+7, 0));
        }
    }

    @Test
    public void transposeSingleGridVerticalTest(){
        CoordSet set = new CoordSet(8,8);
        for (Integer i=0; i<8; i++) {
            assertEquals(1L << 0+7-i, set.transpose(1L << 8*i+0, 1));
            assertEquals(1L << 8+7-i, set.transpose(1L << 8*i+1, 1));
            assertEquals(1L << 16+7-i, set.transpose(1L << 8*i+2, 1));
            assertEquals(1L << 24+7-i, set.transpose(1L << 8*i+3,1));
            assertEquals(1L << 32+7-i, set.transpose(1L << 8*i+4, 1));
            assertEquals(1L << 40+7-i, set.transpose(1L << 8*i+5, 1));
            assertEquals(1L << 48+7-i, set.transpose(1L << 8*i+6, 1));
            assertEquals(1L << 56+7-i, set.transpose(1L << 8*i+7, 1));
        }
    }

    @Test
    public void testRetain(){
        Long currentStartCell = (col[7]^(1L<<7))^(1L<<63);
        Long currentCell = col[6] | col[4];
        Integer orientation = 0;
        Integer direction = -1;
        CoordSet set = new CoordSet(8, 8);

        Long result = set.retain(Optional.empty(), currentStartCell, Optional.empty(), currentCell, orientation, direction);
        Long retainedResult = (col[6]) & ((row[0] | row[7])^-1L);
        assertEquals(retainedResult, result);

    }

    @Test
    public void testRetain2(){
        Optional<Long> prevStartCell = Optional.of(col[0] & ((row[0] | row[7])^-1L));
        Long currentStartCell = 0L;
        Optional<Long> prevCell = Optional.empty();
        Long currentCell = col[7] | col[6] | col[4];
        Integer orientation = 0;
        Integer direction = -1;
        CoordSet set = new CoordSet(8, 8);

        Long result = set.retain(prevStartCell, currentStartCell, prevCell, currentCell, orientation, direction);
        Long retainedResult = (col[7] | col[6])  & ((row[0] | row[7])^-1L);
        assertEquals(retainedResult, result);

    }

    @Test
    public void testRetain3(){
        Long currentStartCell = col[3] & ((row[0] | row[1] | row[6] | row[7])^-1L);
        Long currentCell = col[3];
        Integer orientation = 1;
        Integer direction = -1;
        CoordSet set = new CoordSet(8, 8);

        Long result = set.retain(Optional.empty(), currentStartCell, Optional.empty(), currentCell, orientation, direction);
        Long retainedResult = col[3] & (row[0] | row[1]);
        assertEquals(retainedResult, result);

    }


    @Test
    public void testShiftRetainEdgePattern(){
        CoordSet set = new CoordSet(8,8);

        Long pattern = col[1] | col[3] | col[5] | col[7] | row[1] | row[3] | row[5] | row[7];
        Long result = set.shiftRetainEdge(pattern, 3, 1);

        assertEquals(col[4] | col[6] | row[2] | row[4] | row[6], result);
    }

    @Test
    public void testShiftRetainEdgePattern2(){
        CoordSet set = new CoordSet(8,8);

        Long pattern = col[1] | col[3] | col[5] | col[7] | row[1] | row[3] | row[5] | row[7];
        Long result = set.shiftRetainEdge(pattern, 1, 3);

        assertEquals(col[2] | col[4] | col[6] | row[4] | row[6], result);
    }

    @Test
    public void testShiftRetainEdgePattern3(){
        CoordSet set = new CoordSet(8,8);
        Long pattern = col[0] | col[2] | col[4] | col[6] | row[0] | row[2] | row[4] | row[6];
        Long result = set.shiftRetainEdge(pattern, -3, -1);

        assertEquals(col[1] | col[3] | row[1] | row[3] | row[5], result);
    }

    @Test
    public void testShiftRetainEdgePattern4(){
        CoordSet set = new CoordSet(8, 8);
        Long pattern = col[0] | col[2] | col[4] | col[6] | row[0] | row[2] | row[4] | row[6];
        Long result = set.shiftRetainEdge(pattern, -1, -3);

        assertEquals(col[1] | col[3] | col[5] | row[1] | row[3], result);
    }

    @Test
    public void testShiftRetainEdgeBit(){
        CoordSet set = new CoordSet(8, 8);
        Long pattern = 1L << 63;
        Long result = set.shiftRetainEdge(pattern, 0, -8);

        assertEquals(row[7]^(1L<<63), result);
    }


    @Test
    public void testShiftRetainEdge(){
        CoordSet set = new CoordSet(8,8);

        Long result = set.shiftRetainEdge(col[7], 0, -8);
        assertEquals(row[7]^(1L<<63), result);
        //assertEquals(0L, result);

        result = set.shiftRetainEdge(row[0], 0, -8);
        assertEquals(0L, result);
        //assertEquals(0L, result);

        result = set.shiftRetainEdge(row[6], 0, -8);
        assertEquals(0L, result);
        //assertEquals(0L, result);

        result = set.shiftRetainEdge(row[7], 0, -8);
        assertEquals(0L, result);
        //assertEquals(0L, result);
    }

    @Test
    public void testShiftRetainEdgeTopRow(){
        CoordSet set = new CoordSet(8,8);

        Long result = set.shiftRetainEdge(col[7], 0, 8);
        assertEquals(row[0]^(1L<<7), result);

        result = set.shiftRetainEdge(row[0], 0, 8);
        assertEquals(0L, result);

        result = set.shiftRetainEdge(row[1], 0, 8);
        assertEquals(0L, result);

        result = set.shiftRetainEdge(row[6], 0, 8);
        assertEquals(0L, result);

        result = set.shiftRetainEdge(row[7], 0, 8);
        assertEquals(0L, result);
    }


    @Test
    public void testShiftRetainEdgeCol(){
        CoordSet set = new CoordSet(8,8);

        Long result = set.shiftRetainEdge(col[7], -8, 0);
        assertEquals(0L, result);

        result = set.shiftRetainEdge(row[0], -8, 0);
        assertEquals(col[7]^(1L<<7), result);

        result = set.shiftRetainEdge(row[6], -8, 0);
        assertEquals(col[7]^(1L<<55), result);

        result = set.shiftRetainEdge(row[7], -8, 0);
        assertEquals(col[7]^(1L<<63), result);
    }

    @Test
    public void testShiftRetainEdgeLeftCol(){
        CoordSet set = new CoordSet(8,8);

        Long result = set.shiftRetainEdge(col[7], 8, 0);
        assertEquals(0L, result);

        result = set.shiftRetainEdge(row[0], 8, 0);
        assertEquals(col[0]^1L, result);

        result = set.shiftRetainEdge(row[6], 8, 0);
        assertEquals(col[0]^(1L<<48), result);

        result = set.shiftRetainEdge(row[7], 8, 0);
        assertEquals(col[0]^(1L<<56), result);
    }


    @Test
    public void testRetainCoordSet(){
        String inputStr =
                "        "+
                "        "+
                " X      "+
                "        "+
                "        "+
                "        "+
                "        "+
                "        ";

        String workingStr =
                "        "+
                        "        "+
                        "X X     "+
                        "X X     "+
                        "X X     "+
                        "X X     "+
                        "        "+
                        "        ";

        String retainStr =
                "        "+
                        "        "+
                        "X X     "+
                        "        "+
                        "        "+
                        "        "+
                        "        "+
                        "        ";

        CoordSet input = createCoordSet(inputStr, 8, 8);
        CoordSet working = createCoordSet(workingStr, 8, 8);
        CoordSet result = input.retain(working);

        CoordSet retainResult = createCoordSet(retainStr, 8, 8);

        assertEquals(retainResult.getKey(), result.getKey());

    }

    @Test
    public void testRetainCoordSet2(){
        String inputStr =
                "                "+
                "                "+
                "                "+
                "   XXXXXXXXXXX  "+
                "                "+
                "                "+
                "                "+
                "                ";

        String workingStr =
                "    XXXXXXXXX   "+
                "                "+
                "  XXXXXXXXXXXXX "+
                "  X           X "+
                "  XXXXXXXXXXXXX "+
                "                "+
                "    XXXXXXXXX   "+
                "                ";

        String retainStr =
                "                "+
                "                "+
                "   XXXXXXXXXXX  "+
                "  X           X "+
                "   XXXXXXXXXXX  "+
                "                "+
                "                "+
                "                ";

        CoordSet input = createCoordSet(inputStr, 16, 8);
        CoordSet working = createCoordSet(workingStr, 16, 8);
        CoordSet result = input.retain(working);

        CoordSet retainResult = createCoordSet(retainStr, 16, 8);

        assertEquals(retainResult.getKey(), result.getKey());

    }

    @Test
    public void test(){
        String startKey="MjAsMjAKLTkxODcyMDI1MDIzNDc0NTY1MTIsMCwwCjM2MTcwMDg2NDE5MDM4MzM2LDAsMAowLDAsMA==";
        String inputKey = "MjAsMjAKMCwwLDAKLTU3NjQ2MDc1MjMwMzQyMzQ4OCw3MjA1NzU5NDAzNzkyNzkzNiwwCjAsMCww";

        CoordSet input = new CoordSet(startKey);
        CoordSet working = new CoordSet(inputKey);

        CoordSet result = input.retain(working);
        CoordSet retainResult = new CoordSet(20, 20);
        retainResult.add(7, 15);

        String retainKey = retainResult.getKey();
        String resultKey = result.getKey();
        assertEquals(retainKey, resultKey);
    }

    @Test
    public void testRetainAcrossCellBoundary(){
        CoordSet input = new CoordSet(16, 8);
        CoordSet working = new CoordSet(16, 8);
        input.add(8,0);
        working.add(6,0, 7, 0);
        working.add(6, 2, 7, 2);

        CoordSet retain = new CoordSet(16, 8);
        retain.add(6, 0, 7, 0);

        CoordSet result = input.retain(working);

        assertEquals(retain.getKey(), result.getKey());

    }

    @Test
    public void projectConstantValue(){
        String[][] target = new String[10][];
        for (int i=0; i<10; i++){
            target[i] = IntStream.range(0, 10).mapToObj(v -> " ").toArray(String[]::new);
        }
        ArrayProjection projectTarget = new ArrayProjection(target);

        String text =
                "XXXXXXXXXX"+
                "X        X"+
                "X        X"+
                "X        X"+
                "X        X"+
                "X        X"+
                "X        X"+
                "X        X"+
                "X        X"+
                "XXXXXXXXXX";

        CoordSet set = createCoordSet(text, 10, 10);
        set.project(projectTarget, "X");

        StringBuilder sb = new StringBuilder();
        for (int i=0; i<target.length; i++){
            sb.append(Arrays.stream(target[i]).collect(Collectors.joining()));
        }
        assertEquals(text, sb.toString());
    }

    @Test
    public void copyReturnsCopyOfCoords(){

        String input =
                "XXXXXXXXXX"+
                "X  XXXX  X"+
                "X XX  XX X"+
                "XXX    XXX"+
                "XX      XX"+
                "XX      XX"+
                "XXX    XXX"+
                "X XX  XX X"+
                "X  XXXX  X"+
                "XXXXXXXXXX";

        CoordSet setA = createCoordSet(input, 10, 10);
        CoordSet copy = setA.copy();
        assertEquals(true, setA.containsAllCoordsIn(copy));
        assertEquals(true, copy.containsAllCoordsIn(setA));

    }

    private void forAllCoordsIn(Point upperLeft, Point lowerRight, BiConsumer<CoordSet, Point> action) {
        for (int x = upperLeft.x; x<= lowerRight.x; x++){
            for (int y= upperLeft.y; y<= lowerRight.y; y++){
                action.accept(coordSet, Point.of(x,y));
            }
        }
    }

    private void forAllCoordsIn(Point upperLeft, Point lowerRight, CoordSet set, BiConsumer<CoordSet, Point> action) {
        for (int x = upperLeft.x; x<= lowerRight.x; x++){
            for (int y= upperLeft.y; y<= lowerRight.y; y++){
                action.accept(set, Point.of(x,y));
            }
        }
    }

    private void forAllCoordsTrueIfInRange(Point upperLeft, Point lowerRight, CoordSet set, BiPredicate<CoordSet, Point> trueIfInRange) {
        for (int x = 0; x < set.getWidth(); x++){
            for (int y = 0; y < set.getHeight(); y++){
                Point p = Point.of(x, y);
                Boolean eval = trueIfInRange.test(set, p);
                Boolean inRange = p.betweenX(upperLeft, lowerRight) && p.betweenY(upperLeft, lowerRight);
                if (inRange) {
                    assertTrue(eval);
                } else {
                    assertFalse(eval);
                }
            }
        }
    }

    private void forAllCoordsNotIn(Point upperLeft, Point lowerRight, CoordSet set, BiConsumer<CoordSet, Point> action) {
        for (int x = 0; x < set.getWidth(); x++){
            for (int y = 0; y<= set.getHeight(); y++){
                Point p = Point.of(x, y);
                if (!(p.betweenX(upperLeft, lowerRight) && p.betweenY(upperLeft, lowerRight))) {
                    action.accept(set, p);
                }
            }
        }
    }

    private void forAllCoordsExcept(Point upperLeft, Point lowerRight, CoordSet exclusion, BiConsumer<CoordSet, Point> action) {
        for (int x = upperLeft.x; x<= lowerRight.x; x++){
            for (int y= upperLeft.y; y<= lowerRight.y; y++){
                if (!exclusion.contains(x, y)) {
                    action.accept(coordSet, Point.of(x, y));
                }
            }
        }
    }

    private void forAllCoordsIn(Integer x1, Integer y1, Integer x2, Integer y2, BiConsumer<CoordSet, Point> action) {
        for (int x = x1; x<=x2; x++){
            for (int y=y1; y<=y2; y++){
                action.accept(coordSet, Point.of(x,y));
            }
        }
    }

    private void assertCoordSetEquals(CoordSet expectedSet, CoordSet actualSet) {
        if (expectedSet.getWidth() != actualSet.getWidth() || expectedSet.getHeight() != actualSet.getHeight()) {
            fail("Dimension does not match (expected " + expectedSet.getWidth() + "x" + expectedSet.getHeight()
                    + " vs actual " + actualSet.getWidth() + "x" + actualSet.getHeight() + ")");
        }
        for (int x=0; x<expectedSet.getWidth(); x++){
            for (int y=0; y<expectedSet.getHeight(); y++){
                boolean expectedContains = expectedSet.contains(x,y);
                boolean actualContains = actualSet.contains(x,y);
                assertEquals(expectedContains, actualContains,
                        "Coord "+x+","+y+" "+(!expectedContains?"not in":"in")
                                +" expected while it is "+(!actualContains?"not in":"in")+" actual");
            }
        }

    }

    private CoordSet createCoordSet(String input, Integer width, Integer height) {
        if (input.length() != width * height) {
            throw new IllegalArgumentException("Input string does not have "+width*height+" characters");
        }
        CoordSet set = new CoordSet(width, height);
        for (int y = 0; y < height; y++){
            for (int x = 0; x < width; x++){
                if (input.charAt(y*width+x) == 'X') {
                    set.add(x, y);
                }
            }
        }
        return set;
    }

    public String format(Long val){
        String binaryString = String.format("%64s", Long.toBinaryString(Long.reverse(val))).replace(' ', '0');
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<8; i++){
            if (sb.length()>0){
                sb.append("\n");
            }
            sb.append(binaryString, i*8, i*8+8);
        }
        return sb.toString();
    }

}
