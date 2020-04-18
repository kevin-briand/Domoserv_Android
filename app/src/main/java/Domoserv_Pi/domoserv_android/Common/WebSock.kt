package Domoserv_Pi.domoserv_android.Common

import Domoserv_Pi.domoserv_android.R
import okhttp3.*
import java.lang.Exception

enum class NetworkError { NoError, PasswordError, DataError, UnknownError }

open class WebSock {
    private var mWs: WebSocket? = null
    private var mWsListener: EchoWebSocketListener? = null

    fun connect(url: String, port: Int, password: String) {
        val ws: OkHttpClient? = OkHttpClient()
        val request = Request.Builder().url("ws://$url:$port").build()
        mWsListener = EchoWebSocketListener(password)
        mWs = ws?.newWebSocket(request, mWsListener!!)
    }
    fun send(data: String) {
        mWs?.send(mWsListener?.getCrypto()!!.Encrypt_Data(data,"Android"))
    }

    fun Update() {

    }

    fun getLastMessage(): String {
        return mWsListener?.getLastMessage() ?: ""
    }

    fun isReadyRead(): Boolean {
        return mWsListener?.isReadyRead() ?: false
    }

    fun disconnect() {
        mWs?.close(1000,null)
    }

    fun getLastError(): Int {
        return mWsListener?.getLastErrorCode() ?: -1
    }

    fun isReady(): Boolean {
        if(mWsListener != null) {
            return mWsListener!!.isReady()
        }
        return false
    }
    fun isOpen(): Boolean {
        if(mWsListener != null) {
            return mWsListener!!.isOpen()
        }
        return false
    }
}

private class EchoWebSocketListener(private val password: String) : WebSocketListener() {
    private var mCrypto = CryptoFire()
    private var mReady = false
    private var mOpen = true
    private val mName = "Android"
    private var mErrorCode = 0
    private var mMessage = ""
    private var mReadyRead = false

    fun isReady(): Boolean {
        return mReady
    }

    fun getCrypto(): CryptoFire {
        return mCrypto
    }

    fun isOpen(): Boolean {
        return mOpen
    }

    fun getLastErrorCode(): Int {
        return mErrorCode
    }

    fun isReadyRead(): Boolean {
        return mReadyRead
    }

    fun getLastMessage(): String {
        mReadyRead = false
        return mMessage
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        mOpen = true
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        if (text.split(" ").count() == 50) {
            mCrypto = CryptoFire(50, 4, text)
            if (!mCrypto.Add_Encrypted_Key(mName, password)) {
                webSocket.close(1000, "Bad encrypted key")
                println("Closing socket")
                mReady = false
                mOpen = false
            } else {
                val data = "${NetworkError.NoError.ordinal} $mName"
                webSocket.send(mCrypto.Encrypt_Data(data,mName))
            }
        }
        else {
            if(text.count() == 1) {
                when (text.toInt()) {
                    NetworkError.NoError.ordinal -> mReady = true
                    NetworkError.PasswordError.ordinal -> {
                        mReady = false
                        mErrorCode = NetworkError.PasswordError.ordinal
                    }
                    NetworkError.DataError.ordinal -> {
                        mReady = false
                        mErrorCode = NetworkError.DataError.ordinal
                    }
                    else -> mErrorCode = NetworkError.UnknownError.ordinal
                }
            }
            else {
                mMessage = mCrypto.Decrypt_Data(text, mName)
                mReadyRead = true
            }
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(1000, null)
        println("Closing : $code / $reason")
        mReady = false
        mOpen = false
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        mReady = false
        mOpen = false
        println(t.message)
    }
}