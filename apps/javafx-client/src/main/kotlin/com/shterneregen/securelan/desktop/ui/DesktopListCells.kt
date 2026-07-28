package com.shterneregen.securelan.desktop.ui

import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import java.util.function.Consumer

class MediaDeviceChoiceCell : ListCell<MediaDeviceChoice>() {
    override fun updateItem(item: MediaDeviceChoice?, empty: Boolean) {
        super.updateItem(item, empty)
        text = if (empty || item == null) null else item.toString()
        graphic = null
    }
}

class PeerCell : ListCell<PeerPresence>() {
    override fun updateItem(item: PeerPresence?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            text = null
            graphic = null
            return
        }

        val dot = Circle(5.0, if (item.online()) Color.web("#1f9d55") else Color.web("#9aa4b2"))
        val name = Label(item.nickname()).apply {
            styleClass.add("list-primary")
        }
        val meta = Label(DesktopPeerFormatters.formatListMeta(item)).apply {
            styleClass.add("list-secondary")
        }
        val textBox = VBox(2.0, name, meta)
        val row = HBox(8.0, dot, textBox).apply {
            styleClass.add("list-row")
            alignment = Pos.CENTER_LEFT
        }
        ensureContentListCellStyle()
        graphic = row
    }
}

class QuickShareCell(
    private val copyAction: Consumer<QuickShareEntry>,
    private val stopAction: Consumer<QuickShareEntry>,
) : ListCell<QuickShareEntry>() {
    override fun updateItem(item: QuickShareEntry?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            text = null
            graphic = null
            return
        }

        val snapshot = item.snapshot()
        val name = Label(snapshot.displayName()).apply {
            styleClass.add("list-primary")
        }
        val meta = Label(DesktopQuickShareFormatters.formatSnapshotMeta(snapshot)).apply {
            styleClass.add("list-secondary")
        }
        val copyButton = Button("Copy").apply {
            styleClass.addAll("app-button", "secondary-button")
            setOnAction { copyAction.accept(item) }
        }
        val stopButton = Button("Stop").apply {
            styleClass.addAll("app-button", "danger-button")
            setOnAction { stopAction.accept(item) }
            isDisable = !item.active()
        }
        val textBox = VBox(2.0, name, meta)
        val row = HBox(8.0, textBox, copyButton, stopButton).apply {
            HBox.setHgrow(textBox, Priority.ALWAYS)
            styleClass.add("list-row")
            alignment = Pos.CENTER_LEFT
        }
        ensureContentListCellStyle()
        graphic = row
    }
}

class TransferCell : ListCell<TransferEntry>() {
    override fun updateItem(item: TransferEntry?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            text = null
            graphic = null
            return
        }

        val name = Label(item.fileName).apply {
            styleClass.add("list-primary")
        }
        val meta = Label(DesktopTransferFormatters.formatTransferListMeta(item)).apply {
            styleClass.add("list-secondary")
        }
        val box = VBox(2.0, name, meta).apply {
            styleClass.add("list-row")
        }
        ensureContentListCellStyle()
        graphic = box
    }
}

private fun ListCell<*>.ensureContentListCellStyle() {
    if (!styleClass.contains("content-list-cell")) {
        styleClass.add("content-list-cell")
    }
}
