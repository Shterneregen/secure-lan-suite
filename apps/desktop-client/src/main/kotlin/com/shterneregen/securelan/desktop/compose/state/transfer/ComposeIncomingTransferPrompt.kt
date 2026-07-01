package com.shterneregen.securelan.desktop.compose.state.transfer

import com.shterneregen.securelan.desktop.ui.DesktopTransferFormatters
import com.shterneregen.securelan.filetransfer.protocol.FileTransferMetadata

public data class ComposeIncomingTransferPrompt(
    val id: String,
    val senderId: String,
    val fileName: String,
    val fileSize: Long,
    val remoteAddress: String,
    val status: ComposeIncomingTransferPromptStatus = ComposeIncomingTransferPromptStatus.WAITING,
) {
    val title: String = DesktopTransferFormatters.incomingFileTitle()
    val header: String = DesktopTransferFormatters.incomingFileHeader(senderId)
    val content: String = DesktopTransferFormatters.incomingFileContent(fileName, fileSize, remoteAddress)
    val sizeLabel: String = DesktopTransferFormatters.formatMegabytes(fileSize)
    val statusLabel: String = status.label
    val waitingForDecision: Boolean = status == ComposeIncomingTransferPromptStatus.WAITING

    fun withStatus(nextStatus: ComposeIncomingTransferPromptStatus): ComposeIncomingTransferPrompt = copy(status = nextStatus)

    companion object {
        fun from(
            metadata: FileTransferMetadata,
            remoteAddress: String,
            status: ComposeIncomingTransferPromptStatus = ComposeIncomingTransferPromptStatus.WAITING,
        ): ComposeIncomingTransferPrompt =
            ComposeIncomingTransferPrompt(
                id = metadata.transferId,
                senderId = metadata.senderId,
                fileName = metadata.fileName,
                fileSize = metadata.fileSize,
                remoteAddress = remoteAddress,
                status = status,
            )
    }
}
