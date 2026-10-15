package com.shterneregen.securelan.filetransfer.service

import java.io.IOException

/** Platform adapters preserve the shared failure category without exposing payloads in UI diagnostics. */
class FileTransferAttemptException(val kind: FileTransferFailureKind, cause: Throwable? = null) : IOException("File transfer failed: $kind", cause)
