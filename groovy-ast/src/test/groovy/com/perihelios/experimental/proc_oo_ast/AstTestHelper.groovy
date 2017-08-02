package com.perihelios.experimental.proc_oo_ast

class AstTestHelper {
	private final GroovyClassLoader loader

	AstTestHelper() {
		loader = new GroovyClassLoader()
	}

	AstTestHelper parseCode(String groovyCode) {
		loader.parseClass(groovyCode)

		return this
	}

	Class<?> getClass(String name) {
		return loader.loadClass(name)
	}
}
