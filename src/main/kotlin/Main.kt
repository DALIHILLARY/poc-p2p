import java.io.File
import java.lang.Thread.sleep
import java.net.InetAddress
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    /**
     * This is a p2p implementation of 400-node capacity
     */
    var myNode = Node()
    val client = Client()
    val id : Int
    if(File("device.txt").exists()) {
       id = File("device.txt").readLines()[0].toInt()
    }else {
        id = IntRange(1,400).random()
        File("device.txt").writeText(id.toString())
    }

    myNode = myNode.copy(id = id, ipAddress = InetAddress.getLocalHost().hostAddress)
    println("device id: $id")
    Server(33456,33457,myNode, client)
    thread {
        sleep(1000*10L)
        println(client.send("HELLO THIS IS A PING BACK#","127.0.1.1",33456))
    }
    thread {
        sleep(1000L*8L)
        println(client.send("FILE THIS IS A PING BACK#","127.0.1.1",33457))
    }


}