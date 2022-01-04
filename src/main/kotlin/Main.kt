import java.io.File
import java.lang.Thread.sleep
import java.net.InetAddress
import java.util.*
import kotlin.concurrent.thread

var myNode = Node()

fun main(args: Array<String>) {
    /**
     * This is a p2p implementation of 400-node capacity
     */
    val client = Client()
    val id : Int
    val uuid : String
    if(!File("files").exists()){
        File("files").mkdir()
    }
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
    println("Node id: $id and Node pid : $uuid" )
    Server(33456,33457,myNode, client)

    while (true) {
        println("\n1. Join Network\n2. Leave Network\n3. Search File\n4. Print Finger Table\n")
        print("COMMAND ::>")
        when(readLine()) {
            "1" -> {
                print("Enter IP Address: ")
                val ip = readLine().toString()
                print("Enter port: ")
                val port = readLine()!!.toInt()
                println(client.send("HELLO ${myNode.id} ${myNode.pid} ${myNode.ipAddress}#", ip, port))
            }
            "2" -> {
                //leaving the network
                client.send("LEAVING ${myNode.predecessor_id} ${myNode.predecessor_address} ${myNode.predecessor_port}#",myNode.successor_address,myNode.successor_port)
                client.send("LEAVING ${myNode.successor_id} ${myNode.successor_address} ${myNode.successor_port}#", myNode.predecessor_address,myNode.predecessor_port)

                //send files to the successor
                client.sendFile("LEAVING ${myNode.successor_id} ${myNode.successor_address} ${myNode.successor_port}#", myNode.predecessor_address,myNode.predecessor_port)

            }
            "3" -> {
                print("Enter file Name: ")
                val fileName = readLine().toString()
                client.send("SEARCH $fileName#",myNode.successor_address,myNode.successor_port)
            }
            "4" -> {
                println("FINGER TABLE")
                val fingerTable = hashMapOf<Int,List<String>>()
//                fingerTable[236] = listOf("236","192.168.43.78","33456")
//                fingerTable[78] = listOf("78","192.168.43.193","33456")
                println(fingerTable)

            }
        }
    }


}