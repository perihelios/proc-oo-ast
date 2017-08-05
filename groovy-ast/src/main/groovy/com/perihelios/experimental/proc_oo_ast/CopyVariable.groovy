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

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.syntax.Token;

class CopyVariable {
	static CopyContext copy(String variableName) {
		new CopyContext(variableName)
	}

	static class CopyContext {
		private final String oldVariableName

		private CopyContext(String oldVariableName) {
			this.oldVariableName = oldVariableName
		}

		ToNewVariableContext toNewVariable(String newVariableName) {
			new ToNewVariableContext(oldVariableName, newVariableName)
		}
	}

	static class ToNewVariableContext {
		private final String oldVariableName
		private final String newVariableName

		private ToNewVariableContext(String oldVariableName, String newVariableName) {
			this.oldVariableName = oldVariableName
			this.newVariableName = newVariableName
		}

		OfTypeContext ofType(ClassNode type) {
			new OfTypeContext(oldVariableName, newVariableName, type)
		}
	}

	static class OfTypeContext {
		private static final int UNKNOWN_LINE_NUMBER = -1
		private static final int UNKNOWN_COLUMN_NUMBER = -1

		private final String oldVariableName
		private final String newVariableName
		private final ClassNode type

		OfTypeContext(String oldVariableName, String newVariableName, ClassNode type) {
			this.oldVariableName = oldVariableName
			this.newVariableName = newVariableName
			this.type = type
		}

		ExpressionStatement usingCopyConstructor() {
			VariableExpression newVariable = new VariableExpression(newVariableName, type)
			Token assignmentOperator = Token.newSymbol("=", UNKNOWN_LINE_NUMBER, UNKNOWN_COLUMN_NUMBER)
			VariableExpression oldVariable = new VariableExpression(oldVariableName, type)
			Expression newInstanceFromCopyConstructor =
				new ConstructorCallExpression(type, new ArgumentListExpression(oldVariable))

			new ExpressionStatement(
				new DeclarationExpression(newVariable, assignmentOperator, newInstanceFromCopyConstructor)
			)
		}
	}
}
