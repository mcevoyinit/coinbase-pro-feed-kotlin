package com.gsr.challenge

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException
import java.util.*

/**
 * OkHttp Websocket Listener to Coinbase Pro API
 *
 * - handles messages for level 2 updates
 * - uses builder to build order objects
 * - adds them to existing order book
 * - displays depth 10 order book to console
 */
class CoinbaseListener : WebSocketListener() {
    
    private var orderBook: OrderBook = OrderBook(emptyMap(), emptyMap())

    private val mapper: ObjectMapper = ObjectMapper()
        .registerModule(KotlinModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
    
    private val builder = CoinbaseBuilder()

    override fun onOpen(webSocket: WebSocket, response: Response) {
        print("Coinbase Connection opened \n")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val decoded = mapper.readValue<JsonNode>(text)
        when (decoded.get("type").asText()) {
            "error" -> handleErrorMessage(decoded)
            "l2update" -> handleLevel2UpdateMessage(text)
            "snapshot" -> handleLevel2SnapshotMessage(text)
            else -> return
        }
    }

    private fun handleErrorMessage(decoded: JsonNode) {
        val message = decoded.get("message").asText()
        throw RuntimeException("Error message received: $message")
    }

    private fun handleLevel2UpdateMessage(response: String) {
        println("Level Two Message Received")
        println("Processing & Adding to Order Book \n")
        val orderBookUpdate = builder.buildOrderBookUpdate(response) ?: OrderBookUpdate(emptyList(), emptyList())
        orderBook.updateBook(orderBookUpdate)
        val orderBookNLevels: List<List<Float>> = orderBook.generateBook(10)
        val asks = orderBookNLevels[0]
        val bids = orderBookNLevels[1]
        println("1. Ask  " + "\t" + " 2. Bid")
        val depth = Math.max(asks.size, bids.size)
        for (i in 0 until depth) {
            println("" + asks[i] + "\t" + bids[i])
        }
        println("--------------------")
    }

    private fun handleLevel2SnapshotMessage(response: String) {
        println("Level Two Snapshot Received - Bootstrapping Order Book")
        orderBook = builder.buildOrderBookFromSnapshot(response)
        val orderBookNLevels: List<List<Float>> = orderBook.generateBook(10)
        val asks = orderBookNLevels[0]
        val bids = orderBookNLevels[1]
        println("1. Ask  " + "\t" + " 2. Bid")
        val depth = Math.max(asks.size, bids.size)
        for (i in 0 until depth) {
            println("" + asks[i] + "\t" + bids[i])
        }
        println("--------------------")
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        print("Message received")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        print("Closing")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        print("Closed")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        print("Failure")
    }
}

/**
 * Helper class that:
 *
 * - Builds order book updates from incoming messages received by listener
 * - iterates through JSON arrays and extracts/parses changes, prices
 */
class CoinbaseBuilder {

    private val parser = JSONParser()

    // build from update
    fun buildOrderBookUpdate(l2UpdateResponse: String?): OrderBookUpdate? {
        if (l2UpdateResponse == null || l2UpdateResponse.trim { it <= ' ' }.isEmpty()) {
            return null
        }
        var buySellUpdates: OrderBookUpdate? = null
        try {
            val obj: Any = parser.parse(l2UpdateResponse)
            val jsonObject = obj as JSONObject
            val changesList = jsonObject["changes"] as JSONArray?
            buySellUpdates = buildOrderBookUpdateFromJSON(changesList)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return buySellUpdates
    }

    // Bootstrap from snapshot
    fun buildOrderBookFromSnapshot(snapshotResponse: String?): OrderBook {
        var asks: Map<Float, Float>? = null
        var bids: Map<Float, Float>? = null
        try {
            val obj: Any = parser.parse(snapshotResponse)
            val jsonObject = obj as JSONObject
            val asksList = jsonObject["asks"] as JSONArray?
            val bidsList = jsonObject["bids"] as JSONArray?
            asks = getPrices(asksList)
            bids = getPrices(bidsList)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return OrderBook(asks, bids)
    }

    private fun buildOrderBookUpdateFromJSON(jsonArray: JSONArray?): OrderBookUpdate? {
        if (jsonArray == null || jsonArray.size == 0) {
            return null
        }
        val buyUpdates: MutableList<OrderBookItem> = ArrayList<OrderBookItem>()
        val sellUpdates: MutableList<OrderBookItem> = ArrayList<OrderBookItem>()
        for (sidePriceSize in jsonArray as Iterable<JSONArray>) {
            val side = sidePriceSize[0] as String
            val price: Float = (sidePriceSize[1] as String?)?.toFloat()!!
            val size: Float = (sidePriceSize[2] as String?)?.toFloat()!!
            val orderBookElement = OrderBookItem(price, size)
            when (side) {
                "buy" -> buyUpdates.add(orderBookElement)
                "sell" -> sellUpdates.add(orderBookElement)
                else -> System.err.println("Unsupported side value: $side")
            }
        }
        return OrderBookUpdate(buyUpdates, sellUpdates)
    }

    private fun getPrices(jsonArray: JSONArray?): Map<Float, Float>? {
        if (jsonArray == null || jsonArray.size == 0) {
            return null
        }
        val prices: MutableMap<Float, Float> = HashMap()
        for (priceAndSize in jsonArray as Iterable<JSONArray>) {
            val price: Float = (priceAndSize[0] as String?)?.toFloat()!!
            val size: Float = (priceAndSize[1] as String?)?.toFloat()!!
            prices[price] = size
        }
        return prices
    }

}

/**
 *  Extension functions that creates level two subscription messages for the coinbase websocket.
 *  Nested JSON Structure e.g
 ```
 {
    "product_ids" : ["BTC-GBP"],
    "channels" : [
        "level2",
        "heartbeat",
        {
            "product_ids" : ["BTC-GBP"],
            "name":"ticker"
        }
    ],
    "type" : "subscribe"
 }
 ```
 */
internal fun generateLevelTwoMessage(instrument: String): String {
    val message = JSONObject()
    message["type"] = "subscribe"
    val productIds = JSONArray()
    productIds.add(instrument)
    message["product_ids"] = productIds
    val channels = JSONArray()

    channels.add("level2")
    channels.add("heartbeat")

    val channelValue = JSONObject()
    channelValue["name"] = "ticker"
    channelValue["product_ids"] = productIds
    channels.add(channelValue)

    message["channels"] = channels
    return message.toJSONString()
}