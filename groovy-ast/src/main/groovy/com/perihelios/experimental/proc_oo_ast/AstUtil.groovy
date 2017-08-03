package com.perihelios.experimental.proc_oo_ast

import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.ReturnStatement

class AstUtil {
	static ReturnStatement returnVariable(String variable) {
		new ReturnStatement(new VariableExpression(variable))
	}
}
