package com.perihelios.experimental.proc_oo_ast

@ObjectOriented
class List {
	private final String[] elements

	List(String... elements) {
		this.elements = Arrays.copyOf(elements, elements.length)
	}

	List(List from) {
		this.elements = Arrays.copyOf(from.elements, from.elements.length)
	}

	@Override
	String toString() {
		return Arrays.toString(elements)
	}

	static void sort(@OoCopy List list) {
		Arrays.sort(list.elements)
	}

	static void lowercase(@OoCopy List list) {
		for (int i = 0; i < list.elements.length; i++) {
			list.elements[i] = list.elements[i].toLowerCase()
		}
	}

	static boolean allUppercase(List list) {
		for (int i = 0; i < list.elements.length; i++) {
			if (list.elements[i] != list.elements[i].toUpperCase()) {
				return false
			}
		}

		return true
	}

	static void appendToAll(@OoCopy List list, String toAppend) {
		for (int i = 0; i < list.elements.length; i++) {
			list.elements[i] = list.elements[i] + toAppend
		}
	}
}
