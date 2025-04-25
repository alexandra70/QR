package com.example.myqrapp

object SenderReaderVars {

    // timpul trimis în primul pachet (de obicei 10 secunde)
    var initialSyncTimeMs: Long = 10_000

    // delay între pachetele trimise
    var packetDelayMs: Long = 3_000

    // delay de trimitere prim pachet (poți schimba de test)
    var firstPacketRepeatInterval: Long = 1_000

    // payload lenght (cate caractere are in payload)
    var payloadLength: Int = 983//124

    //cat la suta din ecran sa folosesc
    var qrSizeScaleFactor: Float = 1.00f // 3/4 75% din ecran
}