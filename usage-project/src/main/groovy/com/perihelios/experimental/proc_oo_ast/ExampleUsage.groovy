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

import groovy.transform.CompileStatic

@CompileStatic
class ExampleUsage {
	static void main(String[] args) {
		StrList input = new StrList("A", "C", "B")
		print "\n\nInput: "
		StrList.print(input)

		procedural(new StrList(input))
		oo(new StrList(input))

		println()
	}

	static void procedural(StrList list) {
		println "Procedural calls:"

		StrList.add(list, "A")
		StrList.sort(list)
		StrList.toLowerCase(list)

		print "\tOriginal list: "
		StrList.print(list)

		print "\tModified list: "
		StrList.print(list)
	}

	static void oo(StrList list) {
		println "OO calls:"

		list.add("A")
		StrList modified = list.sort().toLowerCase()

		print "\tOriginal list: "
		list.print()

		print "\tModified list: "
		modified.print()
	}
}
