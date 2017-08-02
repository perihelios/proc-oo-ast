package com.perihelios.experimental.proc_oo_ast

import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.security.CodeSource

class AstTestHelper {
	private final GroovyClassLoader loader

	AstTestHelper(Class<? extends ASTTransformation> transformationClass) {
		GroovyASTTransformation annotation = transformationClass.getAnnotation(GroovyASTTransformation)

		if (!annotation) {
			throw new RuntimeException("$transformationClass.name is not annotated with " +
				GroovyASTTransformation.name + "; cannot proceed")
		}

		CompilePhase compilePhase = annotation.phase()
		ASTTransformation transformation = transformationClass.newInstance()

		loader = new TransformingClassLoader(transformation, compilePhase)
	}

	AstTestHelper parseCode(String groovyCode) {
		loader.parseClass(groovyCode)

		return this
	}

	Class<?> getClass(String name) {
		return loader.loadClass(name)
	}

	private static class TransformingClassLoader extends GroovyClassLoader {
		private final ASTTransformation transformation
		private final CompilePhase compilePhase

		TransformingClassLoader(ASTTransformation transformation, CompilePhase compilePhase) {
			this.transformation = transformation
			this.compilePhase = compilePhase
		}

		@Override
		protected CompilationUnit createCompilationUnit(CompilerConfiguration config, CodeSource codeSource) {
			CompilationUnit cu = super.createCompilationUnit(config, codeSource)
//			cu.addPhaseOperation(new TransformingPhaseOperation(transformation), compilePhase.phaseNumber)
			return cu
		}
	}

	private static class TransformingPhaseOperation extends CompilationUnit.SourceUnitOperation {
		private final ASTTransformation transformation

		TransformingPhaseOperation(ASTTransformation transformation) {
			this.transformation = transformation
		}

		@Override
		void call(SourceUnit source) throws CompilationFailedException {
			transformation.visit(null, source)
		}
	}
}
