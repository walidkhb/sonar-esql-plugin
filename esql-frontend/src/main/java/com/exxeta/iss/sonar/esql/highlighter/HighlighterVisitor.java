package com.exxeta.iss.sonar.esql.highlighter;

import java.util.List;

import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;

import com.exxeta.iss.sonar.esql.api.EsqlNonReservedKeyword;
import com.exxeta.iss.sonar.esql.api.tree.Tree;
import com.exxeta.iss.sonar.esql.api.tree.Tree.Kind;
import com.exxeta.iss.sonar.esql.api.tree.lexical.SyntaxToken;
import com.exxeta.iss.sonar.esql.api.tree.lexical.SyntaxTrivia;
import com.exxeta.iss.sonar.esql.api.tree.statement.CreateFunctionStatementTree;
import com.exxeta.iss.sonar.esql.api.tree.statement.CreateModuleStatementTree;
import com.exxeta.iss.sonar.esql.api.tree.statement.CreateProcedureStatementTree;
import com.exxeta.iss.sonar.esql.api.tree.statement.DeclareStatementTree;
import com.exxeta.iss.sonar.esql.api.visitors.SubscriptionVisitor;
import com.exxeta.iss.sonar.esql.compat.CompatibleInputFile;
import com.exxeta.iss.sonar.esql.lexer.EsqlReservedKeyword;
import com.exxeta.iss.sonar.esql.tree.impl.expression.LiteralTreeImpl;
import com.exxeta.iss.sonar.esql.tree.impl.lexical.InternalSyntaxToken;
import com.google.common.collect.ImmutableList;

public class HighlighterVisitor extends SubscriptionVisitor{

	  private final SensorContext sensorContext;
	  private NewHighlighting highlighting;


	  public HighlighterVisitor(SensorContext sensorContext) {
		    this.sensorContext = sensorContext;
		  }

	  @Override
	  public List<Kind> nodesToVisit() {
	    return ImmutableList.<Kind>builder()
	      .add(
	        Kind.DECLARE_STATEMENT,
	        Kind.CREATE_FUNCTION_STATEMENT,
	        Kind.CREATE_MODULE_STATEMENT,
	        Kind.CREATE_PROCEDURE_STATEMENT,
	        Kind.NUMERIC_LITERAL,
	        Kind.STRING_LITERAL,
	        Kind.TOKEN)
	      .build();
	  }

	  @Override
	  public void visitFile(Tree scriptTree) {
	    highlighting = sensorContext.newHighlighting().onFile(((CompatibleInputFile) getContext().getEsqlFile()).wrapped());
	  }

	  @Override
	  public void leaveFile(Tree scriptTree) {
	    highlighting.save();
	  }

	  @Override
	  public void visitNode(Tree tree) {
	    SyntaxToken token = null;
	    TypeOfText code = null;

	    if (tree.is(Kind.DECLARE_STATEMENT)) {
	      token = ((DeclareStatementTree) tree).constantKeyword();
	      code = TypeOfText.KEYWORD;

	    } else if (tree.is(Kind.CREATE_FUNCTION_STATEMENT)) {
		      token = ((CreateFunctionStatementTree) tree).functionKeyword();
		      code = TypeOfText.KEYWORD;

	    } else if (tree.is(Kind.CREATE_PROCEDURE_STATEMENT)) {
		      token = ((CreateProcedureStatementTree) tree).procedureKeyword();
		      code = TypeOfText.KEYWORD;

	    } else if (tree.is(Kind.CREATE_MODULE_STATEMENT)) {
		      token = ((CreateModuleStatementTree) tree).moduleKeyword();
		      code = TypeOfText.KEYWORD;

	    } else if (tree.is(Kind.TOKEN)) {
	      highlightToken((InternalSyntaxToken) tree);

	    } else if (tree.is(Kind.STRING_LITERAL)) {
	      token = ((LiteralTreeImpl) tree).token();
	      code = TypeOfText.STRING;

	    } else if (tree.is(Kind.NUMERIC_LITERAL)) {
	      token = ((LiteralTreeImpl) tree).token();
	      code = TypeOfText.CONSTANT;

	    }

	    if (token != null) {
	      highlight(token, code);
	    }
	  }

	  private void highlightToken(InternalSyntaxToken token) {
	    if (isKeyword(token.text())) {
	      highlight(token, TypeOfText.KEYWORD);
	    }
	    highlightComments(token);
	  }

	  private void highlightComments(InternalSyntaxToken token) {
	    TypeOfText type;
	    for (SyntaxTrivia trivia : token.trivias()) {
	      if (trivia.text().startsWith("/**")) {
	        type = TypeOfText.STRUCTURED_COMMENT;
	      } else {
	        type = TypeOfText.COMMENT;
	      }
	      highlight(trivia, type);
	    }
	  }

	  private void highlight(SyntaxToken token, TypeOfText type) {
	    highlighting.highlight(token.line(), token.column(), token.endLine(), token.endColumn(), type);
	  }

	  private static boolean isKeyword(String text) {
		    for (String keyword : EsqlReservedKeyword.keywordValues()) {
			      if (keyword.equals(text)) {
			        return true;
			      }
			    }
		    for (String keyword : EsqlNonReservedKeyword.keywordValues()) {
			      if (keyword.equals(text)) {
			        return true;
			      }
			    }
	    return false;
	  }
}