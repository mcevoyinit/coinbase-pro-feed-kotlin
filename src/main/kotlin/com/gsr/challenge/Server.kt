@file:JvmName("Server")
package com.gsr.challenge

import okhttp3.OkHttpClient
import okhttp3.Request

const val COINBASE_WS_URL = "wss://ws-feed.pro.coinbase.com"

fun main(args: Array<String>) {
    val target = args[0]
    val listener = CoinbaseListener()
    val http = OkHttpClient()
    val request = Request.Builder().url(COINBASE_WS_URL).build()
    val socket = http.newWebSocket(request, listener)
    val l2Request = generateLevelTwoMessage(target)
    socket.send(l2Request)
    println("L2 Subscription Request Sent")
}