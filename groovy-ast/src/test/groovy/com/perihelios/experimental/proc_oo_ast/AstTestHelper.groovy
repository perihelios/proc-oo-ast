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
