package canthonyl.datastructure.concurrency;

import canthonyl.datastructure.concurrency.Tokens;
import canthonyl.fixture.FixedClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TokensTest {

    private FixedClock clock;

    @BeforeEach
    public void setup(){
        clock = new FixedClock(0L);
    }

    @Test
    public void ableToGetTokenAfterTimeIsUp(){
        Tokens tokens = new Tokens(clock,1L, 5L, TimeUnit.MILLISECONDS);

        assertEquals(0, tokens.reserve());

        clock.advanceMillis(4);
        assertEquals(5, tokens.nextAvailableTimestamp());
        assertEquals(5, tokens.reserve());
        assertEquals(5, tokens.reserve());

        clock.advanceMillis(1);
        assertEquals(0, tokens.reserve());
    }

    @Test
    public void ableToGetMultipleTokensWithinWindow(){
        Tokens tokens = new Tokens(clock, 5L, 5L, TimeUnit.MILLISECONDS);
        for (int i = 0; i < 100; i++) {
            assertEquals( i < 5 ? 0 : i, tokens.nextAvailableTimestamp());
            assertEquals(0, tokens.reserve());
            clock.advanceMillis(1);
        }
    }

    @Test
    public void ableToGetMultipleTokensWithinWindow2(){
        Tokens tokens = new Tokens(clock, 15L, 5L, TimeUnit.MILLISECONDS);
        for (int i = 0; i < 100; i++) {
            assertEquals( i < 5 ? 0 : i, tokens.nextAvailableTimestamp());
            for (int j=0; j < 3; j++) {
                assertEquals(0, tokens.reserve());
            }
            clock.advanceMillis(1);
        }
    }

    @Test
    public void ableToGetTokenUpToMaxTokensInSlidingWindow_regularInterval(){
        Tokens tokens = new Tokens(clock,3L, 5L, TimeUnit.MILLISECONDS);

        // t=0 getToken (result = 0L, tokens: available = 3, remaining = 2)
        // t=1 getToken (result = 0L, tokens available = 2, remaining = 1)
        // t=2 getToken (result = 0L, tokens available = 1, remaining = 0)
        // t=3 getToken (result = 5L, tokens available = 0, remaining = 0)
        // t=4 getToken (result = 5L, tokens available = 0, remaining = 0)
        // t=5 getToken (result = 0L, tokens available = 1, remaining = 0)
        // t=6 getToken (result = 0L, tokens available = 1, remaining = 0)
        // t=7 getToken (result = 0L, tokens available = 1, remaining = 0)

        Long[] expectedResult = { 0L, 0L, 0L, 5L, 6L, 0L, 0L, 0L };

        for (int i=0; i<expectedResult.length; i++){
            System.out.println(i);
            assertEquals(expectedResult[i], tokens.reserve());
            clock.advanceMillis(1);
        }
    }

    @Test
    public void ableToGetTokenUpToMaxTokensInSlidingWindow_sparseToRegularInterval(){
        Tokens tokens = new Tokens(clock,5L, 5L, TimeUnit.MILLISECONDS);

        // t=0 --
        // t=1 getToken x 2 (result = 0L, tokens: available = 5, remaining = 3)
        // t=2 --
        // t=3 getToken x 3 (result = 0L, tokens available = 3, remaining = 0)
        // t=4 --
        // t=5 --
        // t=6 getToken x 2 (result = 0L, tokens available = 2, remaining = 0)
        // t=7 --
        // t=8 getToken x 2 (result = 0L, tokens available = 3, remaining = 1)
        // t=9 --
        // t=10 getToken x 1 (result = 0L, tokens available = 1, remaining = 0)
        // t=11 getToken x 1 (result = 0L, tokens available = 2, remaining = 1)
        // t=12 getToken x 1 (result = 0L, tokens available = 1, remaining = 0)
        // t=13 getToken x 1 (result = 0L, tokens available = 2, remaining = 1)
        // t=14 getToken x 1 (result = 0L, tokens available = 1, remaining = 0)
        // t=15 getToken x 1 (result = 0L, tokens available = 1, remaining = 0)

        //t=1
        clock.advanceMillis(1);
        assertEquals(0L, callReserveToken(2, tokens));

        //t=3
        clock.advanceMillis(2);
        assertEquals(0L, callReserveToken(3, tokens));

        //t=6
        clock.advanceMillis(3);
        assertEquals(0L, callReserveToken(2, tokens));

        //t=8
        clock.advanceMillis(2);
        assertEquals(0L, callReserveToken(2, tokens));

        //t=10
        clock.advanceMillis(2);
        assertEquals(0L, callReserveToken(1, tokens));

        //t=11
        clock.advanceMillis(1);
        assertEquals(0L, callReserveToken(1, tokens));

        //t=12
        clock.advanceMillis(1);
        assertEquals(0L, callReserveToken(1, tokens));

        //t=13
        clock.advanceMillis(1);
        assertEquals(0L, callReserveToken(1, tokens));

        //t=14
        clock.advanceMillis(1);
        assertEquals(0L, callReserveToken(1, tokens));

        //t=15
        clock.advanceMillis(1);
        assertEquals(0L, callReserveToken(1, tokens));
    }

    @Test
    public void nextAvailableTimestamp_ReturnsEarlierTimestampWhenTokenIsAvailable(){
        Tokens tokens = new Tokens(clock,6L, TimeUnit.MILLISECONDS, new Long[]{1L,1L,1L});

        //t=0
        assertEquals(0L, tokens.nextAvailableTimestamp(0L));
        assertEquals(0L, tokens.nextAvailableTimestamp(1L));
        assertEquals(0L, tokens.nextAvailableTimestamp(2L));

        assertEquals(0L, tokens.reserve(0L));

        //t=1
        clock.advanceMillis(1);
        assertEquals(0L, tokens.reserve(1L));

        //t=2
        clock.advanceMillis(1);
        assertEquals(0L, tokens.reserve(2L));

        //t=10
        clock.advanceMillis(8);
        assertEquals(6L, tokens.nextAvailableTimestamp(0L));
        assertEquals(7L, tokens.nextAvailableTimestamp(1L));
        assertEquals(8L, tokens.nextAvailableTimestamp(2L));
    }

    @Test
    public void nextAvailableTimestamp_ReturnsEarliestAvailableTimestamp(){
        Tokens tokens = new Tokens(clock,6L, TimeUnit.MILLISECONDS, new Long[]{1L,1L,1L});

        //t=0
        assertEquals(0L, tokens.nextAvailableTimestamp(0L));
        assertEquals(0L, tokens.nextAvailableTimestamp(1L));
        assertEquals(0L, tokens.nextAvailableTimestamp(2L));

        assertEquals(0L, tokens.reserve(0L));

        //t=1
        clock.advanceMillis(1);
        assertEquals(0L, tokens.reserve(1L));

        //t=2
        clock.advanceMillis(1);
        assertEquals(0L, tokens.reserve(2L));

        //t=3
        clock.advanceMillis(1);
        assertEquals(6L, tokens.nextAvailableTimestamp(0L));
        assertEquals(7L, tokens.nextAvailableTimestamp(1L));
        assertEquals(8L, tokens.nextAvailableTimestamp(2L));
    }

    @Test
    public void getAvailableTokenAfterReserved(){

        Tokens tokens = new Tokens(clock,3L, 5L, TimeUnit.MILLISECONDS);
        assertEquals(3L, tokens.availableTokensAfter(0L));

        //t=0
        assertEquals(0L, tokens.reserve());
        assertEquals(2L, tokens.availableTokensAfter(0L));
        assertEquals(3L, tokens.availableTokensAfter(5L));

        //t=1
        clock.advanceMillis(1);
        assertEquals(0L, tokens.reserve());
        assertEquals(1L, tokens.availableTokensAfter(1L));
        assertEquals(2L, tokens.availableTokensAfter(5L));
        assertEquals(3L, tokens.availableTokensAfter(6L));

        //t=2
        clock.advanceMillis(1);
        assertEquals(0L, tokens.reserve());
        assertEquals(0L, tokens.availableTokensAfter(2L));
        assertEquals(1L, tokens.availableTokensAfter(5L));
        assertEquals(2L, tokens.availableTokensAfter(6L));
        assertEquals(3L, tokens.availableTokensAfter(7L));

        //t=3
        clock.advanceMillis(1);
        assertEquals(5L, tokens.reserve());
        assertEquals(0L, tokens.availableTokensAfter(3L));
        assertEquals(1L, tokens.availableTokensAfter(5L));
        assertEquals(2L, tokens.availableTokensAfter(6L));
        assertEquals(3L, tokens.availableTokensAfter(7L));

        //t=4
        clock.advanceMillis(1);
        assertEquals(6L, tokens.reserve());
        assertEquals(0L, tokens.availableTokensAfter(4L));
        assertEquals(1L, tokens.availableTokensAfter(5L));
        assertEquals(2L, tokens.availableTokensAfter(6L));
        assertEquals(3L, tokens.availableTokensAfter(7L));

        //t=5
        clock.advanceMillis(1);
        assertEquals(0L, tokens.reserve());
        assertEquals(0L, tokens.availableTokensAfter(5L));
        assertEquals(1L, tokens.availableTokensAfter(6L));
        assertEquals(2L, tokens.availableTokensAfter(7L));
        assertEquals(2L, tokens.availableTokensAfter(8L));
        assertEquals(2L, tokens.availableTokensAfter(9L));
        assertEquals(3L, tokens.availableTokensAfter(10L));
    }

    @Test
    public void getAvailableTokenAcrossLevels(){

        Tokens tokens = new Tokens(clock, 6L, TimeUnit.MILLISECONDS, new Long[]{1L, 1L, 1L});

        assertEquals(3L, tokens.availableTokensAfter(0L));

        //t=0
        assertEquals(0L, tokens.reserve(0L));

        //t=1
        clock.advanceMillis(1);
        assertEquals(0L, tokens.reserve(1L));

        //t=2
        clock.advanceMillis(1);
        assertEquals(0L, tokens.reserve(2L));

        assertEquals(1L, tokens.availableTokensAfter(0L, 6L));
        assertEquals(2L, tokens.availableTokensAfter(0L, 7L));
        assertEquals(3L, tokens.availableTokensAfter(0L, 8L));

        assertEquals(0L, tokens.availableTokensAfter(1L, 6L));
        assertEquals(1L, tokens.availableTokensAfter(1L, 7L));
        assertEquals(2L, tokens.availableTokensAfter(1L, 8L));

        assertEquals(0L, tokens.availableTokensAfter(2L, 6L));
        assertEquals(0L, tokens.availableTokensAfter(2L, 7L));
        assertEquals(1L, tokens.availableTokensAfter(2L, 8L));


    }

    @Test
    public void reserveTokens_ReturnsReserveEstimate(){
        Tokens tokens = new Tokens(clock, 3L, 6L, TimeUnit.MILLISECONDS);
        //t=0
        assertEquals(0L, tokens.reserve());

        //t=2
        clock.advanceMillis(2);
        assertEquals(0L, tokens.reserve());

        //t=4
        clock.advanceMillis(2);
        assertEquals(0L, tokens.reserve());

        //t=5
        clock.advanceMillis(1);
        assertEquals(6L, tokens.reserve());
        assertEquals(8L, tokens.reserve());

        //t=6
        clock.advanceMillis(1);
        assertEquals(0L, tokens.reserve());
        assertEquals(8L, tokens.reserve());
    }

    @Test
    public void canNoLongerReserveTokenWhenTokensAtLevelDepleted() {
        Tokens tokens = new Tokens(clock, 10L, TimeUnit.MILLISECONDS, new Long[]{3L, 2L, 1L});

        //t=0
        assertEquals(0L, tokens.reserve(0L));

        //t=1
        clock.advanceMillis(1);
        assertEquals(0L, tokens.reserve(1L));

        //t=2
        clock.advanceMillis(1);
        assertEquals(0L, tokens.reserve(2L));

        //t=3
        clock.advanceMillis(1);
        assertEquals(0L, tokens.reserve(0L));
        assertEquals(0L, tokens.reserve(1L));
        assertEquals(12L, tokens.reserve(2L)); //unable to get token as level already depleted

        //t=4
        clock.advanceMillis(1);
        assertEquals(0L, tokens.reserve(0L));
        assertEquals(11L, tokens.reserve(1L)); //unable to get token as level already depleted

        //t=5
        clock.advanceMillis(1);
        assertEquals(10L, tokens.reserve(0L)); //unable to get token as level already depleted
    }


    @Test
    public void canNoLongerReserveTokenWhenTokensAtLevelOrLowerDepleted() {
        Tokens tokens = new Tokens(clock, 10L, TimeUnit.MILLISECONDS, new Long[]{2L, 2L, 2L});

        //t=0
        assertEquals(0L, tokens.reserve(0L));
        assertEquals(0L, tokens.reserve(1L));
        assertEquals(0L, tokens.reserve(2L));

        //t=1
        assertEquals(0L, tokens.reserve(0L));
        assertEquals(0L, tokens.reserve(1L));
        assertEquals(0L, tokens.reserve(2L));

        //t=2
        assertEquals(10L, tokens.reserve(1L)); //used up current level and below
        assertEquals(10L, tokens.reserve(0L)); //used up current level and below
    }

    @Test
    public void reserveReturnsEarlierReserveEstimateFromLowerLevels_WhenHigherLevelsDepleted(){
        Tokens tokens = new Tokens(clock, 6L, TimeUnit.MILLISECONDS, new Long[]{1L, 1L});

        //t=0
        assertEquals(0L, tokens.reserve(1L));

        //t=1
        clock.advanceMillis(1);
        assertEquals(0L, tokens.reserve(0L));

        //t=2
        clock.advanceMillis(1);
        assertEquals(6L, tokens.reserve(0L));
    }

    @Test
    public void ableToGetTokenFromLowerLevels_WhenHigherLevelsDepleted() {
         Tokens token = new Tokens(clock, 10L, TimeUnit.MILLISECONDS, new Long[]{2L, 2L, 2L});
        //t=0
         assertEquals(0L, token.reserve(0L));
        
         //t=1
         clock.advanceMillis(1);
         assertEquals(0L, token.reserve(1L));
        
         //t=2
         clock.advanceMillis(1);
         assertEquals(0L, token.reserve(2L));
        
         //t=3
         clock.advanceMillis(1);
         assertEquals(0L, token.reserve(0L));
        
         //t=4
         clock.advanceMillis(1);
         assertEquals(0L, token.reserve(0L));
        
         //t=5
         clock.advanceMillis(1);
         assertEquals(0L, token.reserve(0L));
        
         //t=6
         clock.advanceMillis(1);
         assertEquals(10L, token.reserve(0L));
         assertEquals(11L, token.reserve(1L));
         assertEquals(12L, token.reserve(2L));
        assertEquals(13L, token.reserve(0L));
        assertEquals(14L, token.reserve(1L));
        assertEquals(15L, token.reserve(2L));
        
         
    }



    @Test
    public void ableToReserveTokenAtHighestLevel_WhenLowerLevelReplenished(){
        Tokens token = new Tokens(clock, 6L, TimeUnit.MILLISECONDS, new Long[]{1L, 1L, 1L});

        //t=0
        assertEquals(0L, token.reserve(0L));

        //t=1
        clock.advanceMillis(1);
        assertEquals(0L, token.reserve(1L));

        //t=2
        clock.advanceMillis(1);
        assertEquals(0L, token.reserve(2L));

        //t=3
        clock.advanceMillis(1);

        //t=4
        clock.advanceMillis(1);

        //t=5
        clock.advanceMillis(1);

        //t=6
        clock.advanceMillis(1);
        assertEquals(0L, token.reserve(0L));

        //t=7
        clock.advanceMillis(1);
        assertEquals(0L, token.reserve(0L));

        //t=8
        clock.advanceMillis(1);
        assertEquals(0L, token.reserve(0L));
    }


    @Test
    public void reserveTokens_returnsTimestampGivenCurrentReservedTokens(){


        Tokens token = new Tokens(clock, 3L, 6L, TimeUnit.MILLISECONDS);
        //t=0
        assertEquals(0L, token.reserve());

        //t=1
        clock.advanceMillis(1);
        assertEquals(0L, token.reserve());

        //t=2
        clock.advanceMillis(1);
        assertEquals(0L, token.reserve());

        //t=3
        clock.advanceMillis(1);
        assertEquals(6L, token.reserve());

        //t=4
        clock.advanceMillis(1);
        assertEquals(7L, token.reserve());

        //t=5
        clock.advanceMillis(1);
        assertEquals(8L, token.reserve());

        //t=6
        clock.advanceMillis(1);
        assertEquals(0L, token.reserve());

        //t=7
        clock.advanceMillis(1);
        assertEquals(0L, token.reserve());

        //t=8
        clock.advanceMillis(1);
        assertEquals(0L, token.reserve());


    }


    @Test
    public void reserveTokens_returnsTimestampAssumingAvailableTokensAreEagerlyTaken(){


        Tokens tokens = new Tokens(clock, 3L, 6L, TimeUnit.MILLISECONDS);
        //t=0
        assertEquals(0L, tokens.reserve());

        //t=2
        clock.advanceMillis(2);
        assertEquals(0L, tokens.reserve());

        //t=4
        clock.advanceMillis(2);
        assertEquals(0L, tokens.reserve());

        //t=5
        clock.advanceMillis(1);
        assertEquals(6L, tokens.reserve());
        assertEquals(8L, tokens.reserve());
        assertEquals(10L, tokens.reserve());
        assertEquals(10L, tokens.reserve());
    }

    @Test
    public void reserveTokens_returnsTimestamp_withTokensNotEagerlyTaken(){
        Tokens tokens = new Tokens(clock, 3L, 6L, TimeUnit.MILLISECONDS);
        //t=0
        assertEquals(0L, tokens.reserve());

        //t=1
        clock.advanceMillis(1);
        assertEquals(0L, tokens.reserve());

        //t=2
        clock.advanceMillis(1);
        assertEquals(0L, tokens.reserve());

        //t=3
        clock.advanceMillis(1);
        assertEquals(6L, tokens.reserve());

        //t=4
        clock.advanceMillis(1);
        assertEquals(7L, tokens.reserve());

        //t=5
        clock.advanceMillis(1);
        assertEquals(8L, tokens.reserve());

        //t=6
        clock.advanceMillis(1);

        //t=7
        clock.advanceMillis(1);
        assertEquals(0L, tokens.reserve());
        assertEquals(0L, tokens.reserve());
        assertEquals(8L, tokens.reserve());
        assertEquals(13L, tokens.reserve());
        assertEquals(13L, tokens.reserve());
    }

    private Long callReserveToken(long numTimes, Tokens tokens) {
        return LongStream.range(0, numTimes).boxed().map(i -> tokens.nextAvailableTimestamp()).reduce(Long::sum).get();
    }


}
