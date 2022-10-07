### Timed Tokens

[Tokens](../src/main/java/canthonyl/datastructure/concurrency/Tokens.java) is a token reservation system encapsulated in a single class instance that allows calling thread to reserve one or more tokens from a total fixed number of reserve.  Differing from [Semaphore](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Semaphore.html), tokens are automatically returned to the reserve once the observation time period  (specified as a constructor parameter) since the token was last received has elapsed. 

The total number of tokens can also be split into different levels or tiers (from 0 to n-1) to represent priority.  When trying to reserve a token at a level where all tokens are depleted, only tokens from lower levels can be granted.  In such manner, requests for token at lower levels would not hinder higher priority requests for token at higher levels (level 0 being highest).

Tokens can be used as part of a throttle limiting system, where each reserved token is equivalent to a used throttle and the observation time period is a sliding window over which a token become available again.  This is because from the perspective of each token, each token's reserved timestamp is tracked with respect to the internal Clock instance. When calling threads try to reserve a token, a pointer pointing at the earliest token is checked to see whether its latest reservation timestamp took place earlier than the observation time period ending at the current moment in time.  In such case, the pointer is bumped to the next token slot and a return value of 0 indicates a successful reservation.  

For a tokens instance initialized with reserve size of 5 and a window period of 15 seconds, after all 5 tokens were reserved 3 seconds apart (thus depleting all reserve), calling threads would be able to reserve tokens again in 3 seconds interval as each reserved tokens automatically returned to the reserve after 3 seconds.

```java
//Example: Total number of 5 tokens with a sliding window of 15 seconds
Tokens tokens = new Tokens(clock, 5L, 15L, TimeUnit.SECONDS);

Long result;
//t=0, reserve first token
result = tokens.reserve();

//t=3, reserve second token
result = tokens.reserve();

//t=6, reserve third token
result = tokens.reserve();

//t=9, reserve fourth token
result = tokens.reserve();

//t=12, reserve fith token
result = tokens.reserve();

//t=15, reserve first token
result = tokens.reserve();
        
```

A construction of Tokens instance where tokens are allocated to 3 levels, with availability of tokens capped at the current level or below:

```java
//Example: Total number of 5 tokens split to 3 levels [1,3,1]
Tokens tieredTokens = new Tokens(clock, 5L, 15L, TimeUnit.SECONDS, 1, 3, 1);

//reserve tokens at level 0
tieredTokens.reserve(0); //tokens: [0,3,1]
tieredTokens.reserve(0); //tokens: [0,2,1]

//reserve tokens at level 1
tieredTokens.reserve(1); //tokens: [0,1,1]

//reserve tokens at level 2
tieredTokens.reserve(2); //tokens: [0,1,0]

```

