package com.perihelios.experimental.proc_oo_ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import static CopyVariable.copy
import static com.perihelios.experimental.proc_oo_ast.AstUtil.returnVariable

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class OoInjectionAstTransformation implements ASTTransformation {
	private static final String PREFIX = "__oo_injection__"
	private static final String COPY_VARIABLE = "__oo_injection"

	@Override
	void visit(ASTNode[] nodes, SourceUnit source) {
		use(AstCategory) {
			source.AST.classes.findAll { it.hasAnnotation(ObjectOriented) }.each { ClassNode clazz ->
				visitClass(clazz, source)
			}
		}
	}

	private static void visitClass(ClassNode type, SourceUnit source) {
		use(AstCategory) {
			type << type.proceduralMethods.collect { MethodNode oldMethod ->
				List<String> arguments = oldMethod.parameters*.name
				MethodBuilder newMethod = oldMethod.buildCopyNamed(PREFIX + oldMethod.name)

				if (oldMethod.proceduralParameter.hasAnnotation(OoCopy)) {
					if (!type.hasCopyConstructor()) {
						AnnotationNode copyAnnotation = oldMethod.proceduralParameter.annotation(OoCopy)

						source.reportError(
							"Found @$OoCopy.simpleName on procedure parameter in class $type.name, " +
								"which has no copy constructor"
						).atLocation(copyAnnotation.lineNumber, copyAnnotation.columnNumber)
					}

					newMethod << copy(oldMethod.proceduralParameter.name)
						.toNewVariable(COPY_VARIABLE).ofType(type).usingCopyConstructor()
					newMethod << type.callStatic(oldMethod.name, arguments.drop(1).plus(0, COPY_VARIABLE))

					if (oldMethod.voidMethod) {
						newMethod.useReturnType(type)
						newMethod << returnVariable(COPY_VARIABLE)
					}
				} else {
					newMethod << type.callStatic(oldMethod.name, arguments)
				}

				return newMethod
			}
		}
	}
}
