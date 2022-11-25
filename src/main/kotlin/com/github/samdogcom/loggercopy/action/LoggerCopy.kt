package com.github.samdogcom.loggercopy.action

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class LoggerCopy() : AnAction() {

    companion object {
        const val TOP = "┌"
        const val MIDDLE = "├"
        const val LINE = "│"
        const val BOTTOM = "└"
        const val BODY = "Request Body"
        const val JSON = "Received response json string"
    }

    private val AnActionEvent.editor: Editor? get() = CommonDataKeys.EDITOR.getData(dataContext)

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabledAndVisible = hasEditorSelection(e) || mayTranslateWithNoSelection(e)
    }

    private fun hasEditorSelection(e: AnActionEvent): Boolean = e.editor?.selectionModel?.hasSelection() ?: false

    private fun mayTranslateWithNoSelection(e: AnActionEvent): Boolean {
        val isContextMenu = e.place == ActionPlaces.EDITOR_POPUP
        return !isContextMenu
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor ?: return

        val selectionModel = editor.selectionModel
        val isColumnSelectionMode = editor.caretModel.caretCount > 1
        val selectionRange = TextRange(selectionModel.selectionStart, selectionModel.selectionEnd)

        var text: String
        val starts: IntArray
        val ends: IntArray
        if (selectionModel.hasSelection(true) && isColumnSelectionMode) {
            starts = selectionModel.blockSelectionStarts
            ends = selectionModel.blockSelectionEnds
            text = selectionModel.getSelectedText(true).toString()
        } else {
            starts = intArrayOf(selectionRange.startOffset)
            ends = intArrayOf(selectionRange.endOffset)
            text = editor.document.getText(selectionRange)
        }

        var contentStart = false
        var content: String
        val data = text.split("\n")
        text = ""
        data.forEach {
            if (!text.isEmpty() && !contentStart) {
                text += "\n"
            }
            if (it.contains(TOP)) {
                text += it.substring(it.indexOf(TOP)).substring(0, 88)
            } else if (it.contains(MIDDLE)) {
                text += it.substring(it.indexOf(MIDDLE)).substring(0, 44)
            } else if (it.contains(LINE)) {
                content = it.substring(it.indexOf(LINE).plus(if (contentStart) 2 else 0))
                if (content.contains(BODY) || content.contains(JSON)) {
                    content = content.substring(2).replaceFirst("[", "\n[")
                    contentStart = true
                }
                text += content
            } else if (it.contains(BOTTOM)) {
                if (contentStart) {
                    text += "\n"
                }
                text += it.substring(it.indexOf(BOTTOM)).substring(0, 88)
                contentStart = false
            } else {
                text += it
            }
        }

        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val tText = StringSelection(text)
        clipboard.setContents(tText, null)
    }
}