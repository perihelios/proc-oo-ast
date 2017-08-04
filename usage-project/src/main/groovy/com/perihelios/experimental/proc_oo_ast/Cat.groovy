package com.perihelios.experimental.proc_oo_ast

@ObjectOriented
class Cat {
	private String name
	private BigDecimal weight

	static void setName(Cat cat, String name) {
		cat.name = name
	}

	static void setWeight(Cat cat, BigDecimal weight) {
		cat.weight = weight
	}

	static String getInfo(Cat cat) {
		return "$cat.name weighs $cat.weight"
	}
}
