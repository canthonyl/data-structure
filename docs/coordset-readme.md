### Coordinate Set

[Coordinate Set](../src/main/java/canthonyl/datastructure/collection/CoordSet.java) is a set data structure where each element is a coordinate (0 based and non-negative) in 2 dimensional space.  
Once coordinates are added and tracked using Coordinate Set, with no additional effort, the following are available via a method call on a set instance:
- Retain edges and joined edge detection
- Compact and compress geometric feature compression

#### Retain edges

Coordinate Set - retain only edges as seen from x = 0 (left edge)

```java
//1) adds lines enclosing a rectangular region at (10,10) (30,10) (10,15) (30,15)
CoordSet set = new CoordSet(50, 30);
set.add(10,10,30,10);
set.add(10,10,10,15);
set.add(30,10,30,15);
set.add(10,15,30,15);

//2) adds horizontal line inside the rectangular region
set.add(13,13,27,13);

//3) retain edges as seen from left to right (only top, left, bottom edges remain)
int horizontal = 0;
int vertical = 1;
int decreasing = -1;
int increasing = 1;

int orientation = horizontal;
int direction = increasing;
set.retain(orientation, direction, 5, 5, 35, 20);
```

#### Compact and compress geometric feature compression

Coordinate Set - reduce repeating geometric features in x and y direction
```java
//1) Draw a grid with 4 cells of width and height 4x2, 2x1, and 1x1  
// XXXXXXXXX
// X    X  X
// X    X  X
// XXXXXXXXX
// X    X  X
// XXXXXXXXX

CoordSet set = new CoordSet(13, 13);
set.add(2,2,10,2);
set.add(2,3,2,6);
set.add(10,3,10,6);
set.add(2,7,10,7);
//divide enclosed region into 4 cells of varying width and height
set.add(7,3,7,6);
set.add(3,5,9,5);

//2) Compress and compact the coordinate set, resulting in a 5 by 5 grid with 4 cells of 1x1 width and height each
//enclosed in a 7x7 space
        
CoordSet compressed = set.compact();

//3) Outputs expected result to console:
System.out.println(compressed.getCellBinaryString(0,0));
//        
// XXXXX
// X X X
// XXXXX
// X X X
// XXXXX
//
```
