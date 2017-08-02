package com.perihelios.experimental.proc_oo_ast

import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.control.messages.SyntaxErrorMessage

import java.lang.reflect.Method
import java.lang.reflect.Modifier

class OoInjectedAstTransformationTest extends GroovyTestCase {
	private AstTestHelper helper = new AstTestHelper()
	private Class transformed = helper.parseCode("""
		|import ${this.getClass().getPackage().name}.ObjectOriented
		|import ${this.getClass().getPackage().name}.OoCopy
		|
		|@ObjectOriented
		|class ValueHolder {
		|    String value
		|
		|    ValueHolder(String value) {
		|        this.value = value
		|    }
		|
		|    ValueHolder(ValueHolder copyFrom) {
		|        this.value = copyFrom.value
		|    }
		|
		|    static void set(
		|            @Deprecated ValueHolder vh,
		|            @Deprecated String value = "defaultValue"
		|            ) {
		|        vh.value = value
		|    }
		|
		|    static void ooCopySet(@OoCopy ValueHolder vh, String value) {
		|        vh.value = value
		|    }
		|
		|    static boolean isUpperCase(ValueHolder vh) {
		|        return vh.value == vh.value.toUpperCase()
		|    }
		|
		|    static boolean ooCopyIsUpperCase(@OoCopy ValueHolder vh) {
		|        return vh.value == vh.value.toUpperCase()
		|    }
		|
		|    void nonStatic(ValueHolder vh) {
		|    }
		|
		|    private static void privateMethod(ValueHolder vh) {
		|    }
		|
		|    protected static void protectedMethod(ValueHolder vh) {
		|    }
		|
		|    static void nullary() {
		|    }
		|
		|    static void firstParamDiffType(String foo) {
		|    }
		|
		|    static void thrower(ValueHolder vh) throws java.io.IOException, RuntimeException {
		|    }
		|}
	""".stripMargin().trim()).getClass("ValueHolder")

	void test_ooMethodsAdded() {
		Method addedMethod
		assert (addedMethod = transformed.declaredMethods.find {
			it.name == "__oo_injection__set" && it.parameters.length == 2
		})

		assert Modifier.isPublic(addedMethod.modifiers)
		assert Modifier.isStatic(addedMethod.modifiers)
		assert addedMethod.modifiers & Modifier.SYNTHETIC

		assert addedMethod.parameters[0].type.name == "ValueHolder"
		assert addedMethod.parameters[0].getAnnotation(Deprecated)
		assert addedMethod.parameters[1].type.name == "java.lang.String"
		assert addedMethod.parameters[1].getAnnotation(Deprecated)

		Object instance = transformed.newInstance("initialValue")

		transformed.__oo_injection__set(instance, "newValue")
		assert instance.value == "newValue" : "Call passed through to underlying method"

		transformed.__oo_injection__set(instance)
		assert instance.value == "defaultValue" : "Default parameter values retained"
	}

	void test_nonStaticMethodsNotAdded() {
		assert ! transformed.declaredMethods.find { it.name == "__oo_injection__nonStatic" }
	}

	void test_nonPublicMethodsNotAdded() {
		assert ! transformed.declaredMethods.find { it.name == "__oo_injection__privateMethod" }
		assert ! transformed.declaredMethods.find { it.name == "__oo_injection__protectedMethod" }
	}

	void test_nullaryMethodsNotAdded() {
		assert ! transformed.declaredMethods.find { it.name == "__oo_injection__nullary" }
	}

	void test_methodsWithFirstParameterOfDifferentTypeNotAdded() {
		assert ! transformed.declaredMethods.find { it.name == "__oo_injection__firstParamDiffType" }
	}

	void test_ooMethodWithoutOoCopyRetainsReturnType() {
		assert transformed.declaredMethods.find { it.name == "__oo_injection__set" }.returnType == void.class
		assert transformed.declaredMethods.find { it.name == "__oo_injection__isUpperCase" }.returnType == boolean.class
	}

	void test_voidOoMethodWithOoCopyChangesReturnType() {
		assert transformed.declaredMethods.find {
			it.name == "__oo_injection__ooCopySet"
		}.returnType.name == "ValueHolder"
	}

	void test_nonVoidOoMethodWithOoCopyRetainsReturnType() {
		assert transformed.declaredMethods.find {
			it.name == "__oo_injection__ooCopyIsUpperCase"
		}.returnType == boolean.class
	}

	void test_exceptionDeclarationsPropagated() {
		Method thrower = transformed.declaredMethods.find { it.name == "__oo_injection__thrower" }

		assert thrower.exceptionTypes.length == 2
		assert thrower.exceptionTypes.contains(IOException)
		assert thrower.exceptionTypes.contains(RuntimeException)
	}

	void test_ooCopyOnClassWithoutCopyConstructorCausesError() {
		try {
			helper.parseCode("""
				|package com.perihelios.test
				|import ${this.getClass().getPackage().name}.ObjectOriented
				|import ${this.getClass().getPackage().name}.OoCopy
				|
				|@ObjectOriented
				|class Foo {
				|    static void blah(@OoCopy Foo foo) {
				|    }
				|}
			""".stripMargin().trim())
			assert false : "Expected exception $CompilationFailedException.name"
		} catch (MultipleCompilationErrorsException expected) {
			SyntaxErrorMessage message = expected.errorCollector.errors[0] as SyntaxErrorMessage
			assert message.cause.message == "Found @OoCopy on procedure parameter in class " +
				"com.perihelios.test.Foo, which has no copy constructor @ line 7, column 22."
			assert message.cause.sourceLocator =~ /script[0-9]+\.groovy/
		}
	}

	void test_ooCopyDoesNotModifyOriginal() {
		Object original = transformed.newInstance("original")
		assert original.value == "original"

		transformed.__oo_injection__ooCopySet(original, "modified")
		assert original.value == "original"
	}

	void test_valueReturnedFromProcedure() {
		assert transformed.__oo_injection__isUpperCase(transformed.newInstance("a")) == false
		assert transformed.__oo_injection__isUpperCase(transformed.newInstance("A")) == true
	}

	void test_valueReturnedFromNonVoidOoCopyProcedure() {
		assert transformed.__oo_injection__ooCopyIsUpperCase(transformed.newInstance("a")) == false
		assert transformed.__oo_injection__ooCopyIsUpperCase(transformed.newInstance("A")) == true
	}

	void test_copyReturnedFromVoidOoCopyProcedure() {
		Object instance = transformed.newInstance("original")

		assert transformed.__oo_injection__ooCopySet(instance, "modified").value == "modified"
	}

	void test_genericClassesSupported() {
		// Generics usage can cause a compilation error if the AST isn't handled correctly
		helper.parseCode("""
			|import ${this.getClass().getPackage().name}.ObjectOriented
			|import ${this.getClass().getPackage().name}.OoCopy
			|
			|@ObjectOriented
			|class Foo<T> {
			|    Foo(Foo<T> copyFrom) {
			|    }
			|
			|    static void blah(@OoCopy Foo<T> foo) {
			|    }
			|}
		""".stripMargin().trim()).getClass("Foo")
	}

	void test_ignoresClassesWithoutAnnotation() {
		Class transformed = helper.parseCode("""
			|class Foo {
			|    Foo(Foo copyFrom) {
			|    }
			|
			|    static void blah(Foo foo) {
			|    }
			|}
		""".stripMargin().trim()).getClass("Foo")

		assert ! transformed.declaredMethods.find { it.name == "__oo_injection__blah" }
	}

	void test_worksOnMultipleClassesInSameFile() {
		helper.parseCode("""
			|import ${this.getClass().getPackage().name}.ObjectOriented
			|import ${this.getClass().getPackage().name}.OoCopy
			|
			|@ObjectOriented
			|class Foo {
			|    Foo() {
			|    }
			|
			|    Foo(Foo copyFrom) {
			|    }
			|
			|    static void foo(Foo foo) {
			|    }
			|}
			|
			|@ObjectOriented
			|class Blah {
			|    Blah() {
			|    }
			|
			|    Blah(Blah copyFrom) {
			|    }
			|
			|    static void blah(Blah blah) {
			|    }
			|}
		""".stripMargin().trim())

		Class Foo = helper.getClass("Foo")
		Class Blah = helper.getClass("Blah")

		assert Foo.declaredMethods.find { it.name == "__oo_injection__foo" }
		assert Blah.declaredMethods.find { it.name == "__oo_injection__blah" }
	}
}
