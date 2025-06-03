package com.example.myqrapp

object SenderReaderVars {

    // timpul consumat pentru a transmite primul pachet
    var initialSyncTimeMs: Long = 4000

    // delay intre pachete trimise(pentru primul cdru)
    var firstPacketRepeatInterval: Long = 750

    // delay între pachetele trimise(pentru secventa de qr-uri)
    var packetDelayMs: Long = 1000

    // in afara de payload
    var exceptPayload: Int = 40

    // payload lenght (cate caractere are in payload)
    var payloadLength: Int = 2200//2900 //124, 983, 983*2 ok

    //cat la suta din ecran sa folosesc
    var qrSizeScaleFactor: Float = 1.00f // 3/4 75% din ecran


    //cat ar trebui sa fie lungimea totala a unui frame-pachet pe care
    //il voi afisa folosind qr insert text
    var payloadLengthForInsertedText: Int = 60

    //din valoare de mai sus scad cat ar fi - trbeuie sa vad cate caractere ocupa restul de campuri
    //in afara de payload(mai mult e la ccr acolo), o sa fie aceasi valoare la file-read,
    var exceptPayloadForInsertedText: Int = 40

    //
    var PORT: Int = 9877
}