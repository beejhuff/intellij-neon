package cz.juzna.intellij.neon.editor;

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.util.PsiUtilCore;
import cz.juzna.intellij.neon.lexer.NeonTokenTypes;
import cz.juzna.intellij.neon.parser.NeonElementTypes;
import cz.juzna.intellij.neon.psi.NeonArray;
import cz.juzna.intellij.neon.psi.NeonFile;
import cz.juzna.intellij.neon.psi.NeonKeyValPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class NeonEnterHandler implements EnterHandlerDelegate {


	@Override
	public Result preprocessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull Ref<Integer> caretOffset, @NotNull Ref<Integer> caretAdvance, @NotNull DataContext dataContext, @Nullable EditorActionHandler originalHandler) {
		return null;
	}

	@Override
	public Result postProcessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull DataContext dataContext) {
		if (!(file instanceof NeonFile)) {
			return null;
		}
		int offset = editor.getCaretModel().getOffset();
		PsiElement el = PsiUtilCore.getElementAtOffset(file, offset - 1);
		if (!shouldProcess(el)) {
			return null;
		}
		Document document = editor.getDocument();
		String indent = getIndentString(file, isKeyAfterBullet(el.getParent()));
		document.insertString(offset, indent);
		editor.getCaretModel().moveToOffset(offset + indent.length());
		EditorModificationUtil.scrollToCaret(editor);
		editor.getSelectionModel().removeSelection();
		return null;
	}

	private boolean shouldProcess(PsiElement el) {
		PsiElement prev = el.getPrevSibling();
		if (prev != null && prev.getNode().getElementType() == NeonTokenTypes.NEON_ITEM_DELIMITER) { //inline array
			return false;
		}
		if (prev != null && prev.getNode().getElementType() == NeonTokenTypes.NEON_INDENT) { //empty line
			return false;
		}
		PsiElement parent = el.getParent();
		if (parent == null) {
			return false;
		}
		return parent instanceof NeonKeyValPair || parent.getNode().getElementType() == NeonElementTypes.ITEM || isKeyAfterBullet(parent);
	}

	@NotNull
	private String getIndentString(@NotNull PsiFile file, boolean keyAfterBullet) {
		if (keyAfterBullet) {
			return "  ";
		}
		CodeStyleSettingsManager styleSettingsManager = CodeStyleSettingsManager.getInstance(file.getProject());
		CommonCodeStyleSettings.IndentOptions indentOpt = styleSettingsManager.getCurrentSettings().getIndentOptionsByFile(file);
		return indentOpt.USE_TAB_CHARACTER ? "\t" : StringUtil.repeat(" ", indentOpt.INDENT_SIZE);
	}

	private boolean isKeyAfterBullet(PsiElement el) {
		el = el instanceof NeonKeyValPair ? el.getParent() : el;

		return el instanceof NeonArray
				&& el.getChildren().length == 1
				&& el.getParent().getNode().getElementType() == NeonElementTypes.ITEM
				&& el.getPrevSibling().getText().equals(" ")
				&& el.getParent().getFirstChild().getText().equals("-");
	}

}
