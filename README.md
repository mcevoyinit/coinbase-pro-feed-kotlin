## Kotlin Coinbase Pro Level 2 Order Book Feed 

![](chart.png)

### Quick start
Depending on your OS run the corresponding CLI script with the target instrument as input: 

Mac/UNIX
```shell script
./gradlew runCLI --args=BTC-GBP
```

Windows
```shell script
gradlew.bat runCLI --args=BTC-GBP
```

Hit Ctrl-C to quit the CLI. 

### Program

- Establish websocket connection to coinbase pro exchange
- Bootstrap order book with the one-time snapshot of existing bids/asks
- Then process new incoming level two updates matches and add to order book
- Extract 10 highest bids and 10 lowest asks (level 10 depth) and write to console. 

### Expected output

```shell script
ericmcevoy@Erics-MacBook-Pro coinbase-pro-feed % ./gradlew runCLI --args=BTC-GBP
> Task :runCLI
L2 Subscription Request Sent
Coinbase Connection opened 
Level Two Snapshot Received - Bootstrapping Order Book
1. Ask           2. Bid
34844.47        34834.62
34845.22        34834.61
34845.55        34832.88
34847.04        34831.84
34847.05        34831.43
34848.32        34830.44
34849.04        34830.0
34852.52        34829.48
34852.53        34824.84
34852.54        34823.84
--------------------
Level Two Message Received
Processing & Adding to Order Book 

1. Ask           2. Bid
34844.47        34834.62
34845.22        34834.61
34845.55        34832.88
34847.04        34831.84
34847.05        34831.43
34848.32        34830.44
34849.04        34830.0
34852.52        34829.48
34852.53        34824.84
34852.54        34823.84
--------------------
etc..
```
