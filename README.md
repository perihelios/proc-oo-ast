# proc-oo-ast
A quick example of global AST transformations in Groovy and how other languages
(like Python) might be able to implement "automatic OO".

## Overview
Object-oriented (OO) programming is a popular paradigm. However, a large portion
of OO is simply tricks by the compiler to allow a syntax that is viewed as more
desirable.

For example, consider a class written in pseudocode:

```
class Cat
    name as String
    weight as Decimal
    
    method walk(speed as Decimal, direction as Decimal)
        ...
```

This is effectively translated by the compiler into:

```
struct Cat
    name
    weight
    
proc walk(this as Cat, speed as Decimal, direction as Decimal)
    ...
```

That is, methods are really procedures *separate from* the data structure of
class; objects are no more, nor less, than data elements laid out in memory,
and do not contain the executable code of the methods. (Although we're not
considering the complexities of dynamic dispatch in this project, the prior
statement is true even with overriding mechanisms like vtables; the data
structure is merely augmented by the compiler with hidden elements pointing to
externally-defined procedures that represent methods.) 

The compiler then translates method calls into procedure calls with another
syntax trick:

```
fluffy.walk(5.1, 87.2)
```

...becomes:

```
walk(fluffy, 5.1, 87.2)
```

This project demonstrates how a language *could* allow procedural code to be
written, but still provide an OO API by syntactically supporting OO-style
"method" calls using those procedures. Groovy is used simply because its AST
transformations provide an easy way to hook into the compilation process.

## Usage
A "module" (term intended to cover non-OO languages) is represented here as
a class; the class has data members, but no true (instance) methods; thus, the
module represents a data structure, rather than a true OO class. (*Note:*
Modules in this project are annotated with `@ObjectOriented`; this is an
arbitrary way to signal that a Groovy class is really a module, since we're
piggybacking the module concept on top of a class. Other languages might use
another mechanism to signal this, or have their own concept of a module.)

Modules can further define procedures (as static methods, in our Groovy world);
the module acts as a namespace for the procedures. If a procedure defines an
initial argument of the same type as the module, it is eligible for exposure as
an instance method on instances of the data structure.

Here's an example of a module:

```
@ObjectOriented
class Cat {
    private String name
    private BigDecimal weight
    
    static void setName(Cat cat, String name) {
        cat.name = name
    }
    
    static void setWeight(Cat cat, BigDecimal weight) {
        cat.weight = weight
    }
    
    static String getInfo(Cat cat) {
        return "$name weighs $weight"
    }
}
```

A procedural usage of the module is as you would expect: Procedures are under
the Cat namespace, take a Cat data structure as their first argument, and (in
the case of `set*`) mutate the passed structure.

```
Cat fluffy = new Cat()
Cat.setName(fluffy, "Fluffy")
Cat.setWeight(fluffy, 7.5)
println Cat.getInfo(fluffy)
```

How about OO-style? Procedures are automatically converted to methods for us:

```
Cat fluffy = new Cat()
fluffy.setName("Fluffy")
fluffy.setWeight(7.5)
println fluffy.getInfo()
```

The AST transformations have done their work; procedures are also methods!

### Mutation & Immutability
This isn't a hard and fast rule, but it's frequently the case that classic
procedural APIs mutate the data structures they're passed, while OO APIs may
chose to favor immutability. For example, in Java (and many other languages),
`String` is immutable, and methods like `toLowerCase()` that look like they
would mutate their object actually return copies. It would be nice if we
could *retain the mutating behavior* when procedures are called in a procedural
context, but *augment the OO APIs* to behave immutably, if desired.

The `@OoCopy` annotation is for this purpose. When applied to the data structure
parameter of a procedure, a copy is made of the data structure before the
procedure is executed. If the return type of the procedure is `void`, the return
type of the OO method will be adjusted to be that of the data structure, so the
copy can be returned. In order for a copy to be made, the data structure must
provide a copy-constructor.

Here's a trivial example, featuring a module for lists of strings:

```
@ObjectOriented
class StrList {
    private String[] elements

    StrList(String... elements) { this.elements = elements }

    StrList(StrList list) {
        this.elements = Arrays.copyOf(list.elements, list.elements.length)
    }

    static void add(StrList list, String element) {
        int length = list.elements.length
        list.elements = Arrays.copyOf(list.elements, length + 1)
        list.elements[length] = element
    }

    static void sort(@OoCopy StrList list) {
        Arrays.sort(list.elements)
    }

    static void toLowerCase(@OoCopy StrList list) {
        for (int i = 0; i < list.elements.length; i++) {
            list.elements[i] = list.elements[i].toLowerCase()
        }
    }

    static void print(StrList list) {
        println list.elements
    }
}
```

From a procedural context, it's clear mutation is occurring, as every change
made to the data structure is visible in the final result:

```
StrList list = new StrList("A", "C", "B")
StrList.add(list, "A")
StrList.sort(list)
StrList.toLowerCase(list)
StrList.print(list) // Output: [a, a, b, c]
```

However, when we switch to an OO context, we see the result of the `@OoCopy`
annotation: `sort()` and `toLowerCase()` now return *copies* of the list, and do
not mutate the original. (`add()` still mutates the list, as we decided that was
appropriate for our API.)

```
StrList list = new StrList("A", "C", "B")
list.add("A")
list.sort().toLowerCase().print() // Output: [a, a, b, c]
list.print() // Output: [A, C, B, A]
```

These are the OO and procedural APIs we wanted, and we achieved them from only a
procedural API and some annotations.

## Running the Project
Executing `./gradlew run` will build the project and run an example, with
output. You can experiment with the
[ExampleUsage](usage-project/src/main/groovy/com/perihelios/experimental/proc_oo_ast/ExampleUsage.groovy)
and [StrList](usage-project/src/main/groovy/com/perihelios/experimental/proc_oo_ast/StrList.groovy)
classes, or write your own.

## Technical Details
There are two global AST transformations in play.

[OoInjectionAstTransformation](groovy-ast/src/main/groovy/com/perihelios/experimental/proc_oo_ast/OoInjectionAstTransformation.groovy)
is responsible for adding wrapper procedures that receive calls made in an OO
context. This allows copying of data structures annotated with `@OoCopy`.

[MethodCallToProcCallAstTransformation](groovy-ast/src/main/groovy/com/perihelios/experimental/proc_oo_ast/MethodCallToProcCallAstTransformation.groovy)
uses a visitor pattern to change OO-style calls, like `obj.foo(arg)`, into
procedural calls, such as `foo(obj, arg)`.

Much of the grunge work of the AST transformations is moved into other category
or builder classes, using a cleaner API in the transformation code so it's
easier to see how the transformations actually work.

## License
Apache 2.0 (see [LICENSE.txt] and [NOTICE.txt] for details).
