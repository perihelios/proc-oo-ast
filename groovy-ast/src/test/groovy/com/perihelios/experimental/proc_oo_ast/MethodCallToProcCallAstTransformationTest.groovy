package com.perihelios.experimental.proc_oo_ast

class MethodCallToProcCallAstTransformationTest extends GroovyTestCase {
	private AstTestHelper helper = new AstTestHelper()
	private final String stub = """
		|import ${this.getClass().getPackage().name}.ObjectOriented
		|import ${this.getClass().getPackage().name}.OoCopy
		|import groovy.transform.CompileStatic
		|
		|@CompileStatic
		|@ObjectOriented
		|class Module {
		|    static int calls
		|
		|    Module() {}
		|    Module(Module m) {}
		|
		|    static void call(Module m) {
		|        if (Thread.currentThread().stackTrace*.methodName.any { it == "__oo_injection__call" }) {
		|            calls++
		|        }
		|    }
		|
		|    static void copyCall(@OoCopy Module m) {
		|        if (Thread.currentThread().stackTrace*.methodName.any { it == "__oo_injection__copyCall" }) {
		|            calls++
		|        }
		|    }
		|
		|    static boolean booleanCall(Module m) {
		|        if (Thread.currentThread().stackTrace*.methodName.any { it == "__oo_injection__booleanCall" }) {
		|            calls++
		|        }
		|
		|        true
		|    }
		|
		|    static int intCall(Module m) {
		|        if (Thread.currentThread().stackTrace*.methodName.any { it == "__oo_injection__intCall" }) {
		|            calls++
		|        }
		|
		|        1
		|    }
		|}
		|
		|@CompileStatic
		|class ModuleUser {
		|    static Module m
		|
		|    static void doIt() {
		|        ##CODE##
		|    }
		|
		|    static int getOoMethodCalls() {
		|        return Module.calls
		|    }
		|}
	""".stripMargin().trim()

	void test_simpleCall() {
		Class<?> Compiled = compile("""
		|m.call()
		""")

		Compiled.doIt()

		assert Compiled.ooMethodCalls == 1
	}

	void test_nestedCall() {
		Class<?> Compiled = compile("""
		|String.valueOf(m.call())
		""")

		Compiled.doIt()

		assert Compiled.ooMethodCalls == 1
	}

	void test_chainedCall() {
		Class<?> Compiled = compile("""
		|m.copyCall().copyCall()
		""")

		Compiled.doIt()

		assert Compiled.ooMethodCalls == 2
	}

	void test_switch() {
		Class<?> Compiled = compile("""
		|switch (m.call()) {
		|    default: break
		|}
		""")

		Compiled.doIt()

		assert Compiled.ooMethodCalls == 1
	}

	void test_while() {
		Class<?> Compiled = compile("""
		|while (m.booleanCall()) {
		|    break
		|}
		""")

		Compiled.doIt()

		assert Compiled.ooMethodCalls == 1
	}

	void test_expression() {
		Class<?> Compiled = compile("""
		|(m.intCall() + m.intCall()) * m.intCall()
		""")

		Compiled.doIt()

		assert Compiled.ooMethodCalls == 3
	}

	private Class<?> compile(String snippet) {
		helper.parseCode(stub.replace("##CODE##", snippet.stripMargin().trim()))

		return helper.getClass("ModuleUser")
	}
}
