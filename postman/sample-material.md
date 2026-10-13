# Week 5 — Recursion

A recursive method is a method that calls itself to solve a smaller version of the same problem. Every recursive method needs two parts: a base case and a recursive case.

The base case is the input that is small enough to answer directly, without another call. For a list, the base case is usually the empty list. For a number n, it is often n == 0 or n == 1. Without a base case the method calls itself forever, and Java stops it with a StackOverflowError.

The recursive case breaks the problem into a smaller piece and calls the method on that piece. Each call must move closer to the base case, for example by removing the first element of a list or by subtracting one from n. If the input does not shrink, the recursion never ends.

To trust a recursive method, assume the recursive call already returns the right answer for the smaller input, then check that you combine it correctly with the current piece. This is called the leap of faith, and it is the same idea as a proof by induction.

Every call gets its own stack frame with its own local variables. Deep recursion uses a lot of stack memory, so for very long lists a loop can be the safer choice. Tail recursion does not help in Java, because the JVM does not remove tail calls.

Common examples are the factorial of n, the sum of a list, counting the nodes of a tree, and binary search. Trees are a natural fit because each subtree is itself a smaller tree.

Typical mistakes: forgetting the base case, a recursive call that does not shrink the input, and ignoring the value the recursive call returns.
