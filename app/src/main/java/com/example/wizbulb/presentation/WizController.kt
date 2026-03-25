package com.example.wizbulb.presentation

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object WizController {
    var ip   = BulbSettings.DEFAULT_IP
    var port = BulbSettings.DEFAULT_PORT

    fun configure(newIp: String, newPort: Int) {
        ip   = newIp
        port = newPort
    }

    private fun send(command: String) {
        Thread {
            try {
                val socket  = DatagramSocket()
                val bytes   = command.toByteArray()
                val address = InetAddress.getByName(ip)
                socket.send(DatagramPacket(bytes, bytes.size, address, port))
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    /** Sends getPilot to [targetIp]:[targetPort] and waits up to 1.5s for a reply. */
    fun test(targetIp: String, targetPort: Int, onResult: (Boolean) -> Unit) {
        Thread {
            val reachable = try {
                val socket  = DatagramSocket()
                socket.soTimeout = 1500
                val msg     = """{"method":"getPilot","params":{}}"""
                val bytes   = msg.toByteArray()
                socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName(targetIp), targetPort))
                val buf = ByteArray(512)
                socket.receive(DatagramPacket(buf, buf.size))
                socket.close()
                true
            } catch (e: Exception) { false }
            android.os.Handler(android.os.Looper.getMainLooper()).post { onResult(reachable) }
        }.start()
    }

    private val colors = listOf(
        Triple(255, 0, 0),     // red
        Triple(255, 165, 0),   // orange
        Triple(255, 255, 0),   // yellow
        Triple(0, 255, 0),     // green
        Triple(0, 255, 255),   // cyan
        Triple(0, 0, 255),     // blue
        Triple(128, 0, 255),   // purple
        Triple(255, 0, 255),   // magenta
        Triple(255, 255, 255), // white
        Triple(255, 100, 50)   // warm white
    )
    private var colorIndex = 0
    private var isOn = true

    fun toggle() {
        isOn = !isOn
        send("""{"method":"setPilot","params":{"state":$isOn}}""")
    }

    fun turnOn()  = send("""{"method":"setPilot","params":{"state":true,"dimming":100}}""")
    fun turnOff() = send("""{"method":"setPilot","params":{"state":false}}""")
    fun dim()     = send("""{"method":"setPilot","params":{"state":true,"dimming":20}}""")
    fun bright()  = send("""{"method":"setPilot","params":{"state":true,"dimming":100}}""")
    fun red()     = send("""{"method":"setPilot","params":{"state":true,"r":255,"g":0,"b":0,"dimming":100}}""")
    fun green()   = send("""{"method":"setPilot","params":{"state":true,"r":0,"g":255,"b":0,"dimming":100}}""")
    fun blue()    = send("""{"method":"setPilot","params":{"state":true,"r":0,"g":0,"b":255,"dimming":100}}""")
    fun nextColor() {
        val (r, g, b) = colors[colorIndex % colors.size]
        colorIndex++
        send("""{"method":"setPilot","params":{"state":true,"r":$r,"g":$g,"b":$b,"dimming":100}}""")
    }
}
