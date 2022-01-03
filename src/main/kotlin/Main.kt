import java.io.File
import java.lang.Thread.sleep
import java.net.InetAddress
import java.util.*
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    /**
     * This is a p2p implementation of 400-node capacity
     */
    var myNode = Node()
    val client = Client()
    val id : Int
    val uuid : String
    if(File("device.txt").exists()) {
         val data = File("device.txt").readLines()
        id = data[0].toInt()
        uuid = data[1]
    }else {
        id = IntRange(1,400).random()
        uuid = UUID.randomUUID().toString()
        File("device.txt").writeText("$id\n$uuid")
    }

    myNode = myNode.copy(id = id, pid = uuid, ipAddress = InetAddress.getLocalHost().hostAddress)
    println("device id: $id and mesh id : $uuid" )
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