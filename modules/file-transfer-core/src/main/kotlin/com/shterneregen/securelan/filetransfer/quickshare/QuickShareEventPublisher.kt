package com.shterneregen.securelan.filetransfer.quickshare

fun interface QuickShareEventPublisher {
    fun publish(event: QuickShareEvent)

    companion object {
        @JvmStatic
        fun noOp(): QuickShareEventPublisher = QuickShareEventPublisher { }
    }
}
