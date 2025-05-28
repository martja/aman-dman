package util

import javax.swing.JTextField
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.DocumentFilter

object Form {
    fun enforceUppercase(textField: JTextField, maxLength: Int? = null) {
        val doc = textField.document
        if (doc is AbstractDocument) {
            doc.documentFilter = object : DocumentFilter() {
                override fun insertString(fb: FilterBypass, offset: Int, string: String?, attr: AttributeSet?) {
                    if (string != null) {
                        val newString = string.uppercase()
                        val currentText = fb.document.getText(0, fb.document.length)
                        if (maxLength == null || (currentText.length + newString.length) <= maxLength) {
                            super.insertString(fb, offset, newString, attr)
                        }
                    }
                }

                override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String?, attrs: AttributeSet?) {
                    if (text != null) {
                        val newText = text.uppercase()
                        val currentText = fb.document.getText(0, fb.document.length)
                        if (maxLength == null || (currentText.length - length + newText.length) <= maxLength) {
                            super.replace(fb, offset, length, newText, attrs)
                        }
                    }
                }
            }
        }
    }
}