# When to stop making changes and ask in prompt

After you try twice to resolve stale IDE errors, but it still doesn't work, ask in prompt.
They are likely caused by content difference between memory and disk.

# Main source code convention

Consider using functional programming style.

When you write `equals` method, use the following style:
```kotlin
class MyClass {

    override fun equals(other: Any?): Boolean =
        this === other
                || (
                other is MyClass
                        && property == other.property
                        && anotherProperty == other.anotherProperty
                )

}
```

When you write a `class` or an `object`,
put an empty line after the opening brace and before the closing brace.

`..` and `..<` are preferred over `rangeTo`, `until` or `rangeUntil`.

# Unit test

## Framework and style

Use FreeSpec of Kotest.

Use AAA (Arrange, Act, Assert) style if a test case is longer than 30 lines.
Even though a test case is within length of 30 lines,
refer other test cases in the same file to keep the style consistently.

Use prime numbers starting from 5 when you need arbitrary numbers as test data.
5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, ...

## Test coverage

koverVerify should succeed.
You can find the Kover configuration in build.gradle.kts.