package com.gsr.challenge

import java.util.*

/**
 * Represents and OrderBook
 *
 * - Electronic list of buyer and seller bids and asks, with history
 * - Function to generate a book and get price lists to a certain depth
 *
 */
class OrderBook(
    initialAsks: Map<Float, Float>?,
    initialBids: Map<Float, Float>?
) {
    // Asks will be sorted in the ascending order, thus naturally allowing to select N lowest asks from the start
    // Bids on the contrary sorted in the descending order, thus allowing to select N highest bids from the start
    private var asks: SortedMap<Float, Float> = TreeMap(initialAsks!!)
    private var bids: SortedMap<Float, Float> = TreeMap(Collections.reverseOrder())

    init {
        // first declared descending ordered TreeMap then place initial bids
        bids.putAll(initialBids!!)
    }

    fun updateBook(orderBookUpdate: OrderBookUpdate) {
        // process buy updates, if any available
        val buyUpdates = orderBookUpdate.buyUpdates
        for (orderBookElement in buyUpdates) {
            if (orderBookElement.size == 0f) {
                // A size of "0" indicates the price level can be removed
                bids.remove(orderBookElement.price)
            } else {
                // place new price point
                bids[orderBookElement.price] = orderBookElement.size
            }
        }
        // process sell updates, if any available
        val sellUpdates = orderBookUpdate.sellUpdates
        for (orderBookElement in sellUpdates) {
            if (orderBookElement.size == 0f) {
                asks.remove(orderBookElement.price)
            } else {
                asks[orderBookElement.price] = orderBookElement.size
            }
        }
    }

    fun generateBook(level: Int): List<List<Float>> {
        val nAsks = getPrices(asks, level)
        val nBids = getPrices(bids, level)
        return listOf(nAsks, nBids)
    }

    private fun getPrices(prices: SortedMap<Float, Float>, level: Int): List<Float> {
        val result: MutableList<Float> = ArrayList()
        val iterator: Iterator<Float> = prices.keys.iterator()
        var count = 0
        while (iterator.hasNext()) {
            result.add(iterator.next())
            count++
            if(count >= level) break
        }
        return result
    }
}

/**
 * Individual Order Book item
 * - Price of an item and the bid size i.e relative amount requested to buy/sell
 */
data class OrderBookItem(val price: Float, val size: Float)

/**
 *
 * Class to store update received over the exchange feed before it's added to program [OrderBook]
 */
data class OrderBookUpdate(
    val buyUpdates: List<OrderBookItem>,
    val sellUpdates: List<OrderBookItem>
)