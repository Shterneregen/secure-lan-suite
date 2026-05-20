package com.shterneregen.securelan.chat.service

interface ChatHistoryService {
    fun append(line: String)
    fun getAll(): List<String>
    fun dumpAsText(): String
    fun clear()
}
