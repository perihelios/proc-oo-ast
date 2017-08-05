/*
 Copyright 2017, Perihelios LLC

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/
package com.perihelios.experimental.proc_oo_ast

class MethodCallToProcCallAstTransformationTest extends GroovyTestCase {
	private AstTestHelper helper = new AstTestHelper()
	private final String stub = """
		|import ${this.getClass().getPackage().name}.ObjectOriented
		|import ${this.getClass().getPackage().name}.OoCopy
		|import groovy.transform.CompileStatic
		|##IMPORTS##
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

	void test_qualifiedStaticCall() {
		Class<?> Compiled = compile("""
		|Module.call(m)
		""")

		Compiled.doIt()

		assert Compiled.ooMethodCalls == 0
	}

	void test_importedStaticCall() {
		Class<?> Compiled = compile("""
		|call(m)
		""", "static Module.*")

		Compiled.doIt()

		assert Compiled.ooMethodCalls == 0
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

	private Class<?> compile(String snippet, String... additionalImports) {
		String code = stub.replace("##CODE##", snippet.stripMargin().trim())
		code = code.replace("##IMPORTS##", additionalImports.collect {"import $it" }.join("\n"))
		helper.parseCode(code)

		return helper.getClass("ModuleUser")
	}
}
