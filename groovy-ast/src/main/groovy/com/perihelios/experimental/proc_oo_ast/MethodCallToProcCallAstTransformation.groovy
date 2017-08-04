package com.perihelios.experimental.proc_oo_ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class MethodCallToProcCallAstTransformation implements ASTTransformation {
	@Override
	void visit(ASTNode[] nodes, SourceUnit source) {
		source.AST.classes*.visitContents(new MethodCallFixer(source))
	}

	private static class MethodCallFixer extends ClassCodeVisitorSupport {
		private final SourceUnit sourceUnit

		MethodCallFixer(SourceUnit sourceUnit) {
			this.sourceUnit = sourceUnit
		}

		@Override
		void visitMethodCallExpression(MethodCallExpression call) {
			super.visitMethodCallExpression(call)

			use(AstCategory) {
				if (!(call.method instanceof ConstantExpression)) return

				Expression objExpr = call.objectExpression

				if (!objExpr.type.hasAnnotation(ObjectOriented)) return
				if (objExpr instanceof ClassExpression) return

				ConstantExpression methodExpr = call.method as ConstantExpression
				ArgumentListExpression argListExpr = call.arguments as ArgumentListExpression

				ClassNode returnType = objExpr.type.getMethods("__oo_injection__" + methodExpr.text)[0].returnType

				argListExpr.expressions.add(0, objExpr)
				call.setObjectExpression(new ClassExpression(objExpr.type))
				call.setMethod(new ConstantExpression("__oo_injection__" + methodExpr.text))
				call.setType(returnType)
			}
		}

		@Override
		protected SourceUnit getSourceUnit() {
			return sourceUnit
		}
	}
}
