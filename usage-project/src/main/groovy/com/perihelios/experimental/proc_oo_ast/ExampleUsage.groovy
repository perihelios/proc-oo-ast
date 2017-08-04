package com.perihelios.experimental.proc_oo_ast

import groovy.transform.CompileStatic

@CompileStatic
class ExampleUsage {
	static void main(String[] args) {
		StrList input = new StrList("A", "C", "B")
		println "\n\nInput: " + input

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
