package com.example.myqrapp

object SenderReaderVars {

    // timpul consumat pentru a transmite primul pachet
    var initialSyncTimeMs: Long = 4000

    // delay intre pachete trimise(pentru primul cdru)
    var firstPacketRepeatInterval: Long = 750

    // delay între pachetele trimise(pentru secventa de qr-uri)
    var packetDelayMs: Long = 1000

    // payload lenght (cate caractere are in payload)
    var payloadLength: Int = 2900 //124, 983, 983*2 ok

    //cat la suta din ecran sa folosesc
    var qrSizeScaleFactor: Float = 1.00f // 3/4 75% din ecran
}