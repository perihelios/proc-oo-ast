package com.perihelios.experimental.proc_oo_ast

import static List.*

class ExampleUsage {
	static void main(String[] args) {
		List input = new List("A", "C", "B")
		println "\n\nInput: " + input

		procedural(new List(input))
		oo(new List(input))

		println "\n"
	}

	static void procedural(List list) {
		println "Procedural calls:"

		sort(list)
		lowercase(list)
		appendToAll(list, "X")

		println "\tOriginal list: " + list
		println "\tModified list: " + list
		println "\t  All Uppercase? " + allUppercase(list)
	}

	static void oo(List list) {
		println "OO calls:"

		List modified = list.sort()
		modified.lowercase() // Notice no assignment done--result ignored
		modified = modified.appendToAll("X")

		println "\tOriginal list: " + list
		println "\tModified list: " + modified
		println "\t  All Uppercase? " + modified.allUppercase()
	}
}
