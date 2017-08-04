package com.perihelios.experimental.proc_oo_ast

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
