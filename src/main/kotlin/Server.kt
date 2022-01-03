import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class Server(private val port : Int, private val filePort : Int, private val myNode : Node, private val client : Client) {
    private var serverSocket : ServerSocket
    private var serverFileSocket : ServerSocket
    init {
        try{
            serverSocket = ServerSocket(port)
            serverFileSocket = ServerSocket(filePort)
            thread {
                startListener()
            }
            thread {
                startFileListener()
            }
        }catch(e : Throwable) {
            println("Something went wrong with the sockets ${e.message}")
            exitProcess(0)
        }
    }
    private fun startFileListener() {
        var socket : Socket
        try {
            val inetAddress = InetAddress.getLocalHost().hostAddress
            while (true) {
                println("Waiting for connection on address $inetAddress and port : $filePort for file transfer")
                socket = serverFileSocket.accept()
                FileServerThread(socket,myNode, client)
                println("New file connection made")
            }
        }catch (e : Exception) {
            println("Something wrong with the file listener")
        }
    }
    private fun startListener() {
        var socket : Socket
        try {
            val inetAddress = InetAddress.getLocalHost().hostAddress
            while (true) {
                println("Waiting for connection on address $inetAddress and port : $port for commands")
                socket = serverSocket.accept()
                ServerThread(socket,myNode, client)
                println("New command connection made")
            }
        }catch (e : Exception) {
            println("Something t wrong with the listener")
        }
    }
}
private class FileServerThread(private val socket : Socket, private val myNode : Node,  private val client : Client ) : Thread() {
    init {
        start()
    }

    override fun run() {
        try {
            super.run()
            val inputStream = socket.getInputStream()
            val inputStreamReader = InputStreamReader(inputStream)
            val stringBuffer = StringBuffer()
            while (true) {
                val x = inputStreamReader.read()
                if (x.toChar() == '#') break
                stringBuffer.append(x.toChar())
            }
            val request = stringBuffer.toString()
            var response = ""

//          Check request and service it
            when{
                request.startsWith("HELLO") -> {
//                    get the hello data
                    val data = request.split(" ")
                    var fromId =  data[1].toInt()
                    val fromPid = data[2]
                    val fromAddress = data[3]

//                    don't handle requests from yourself
                    if(fromPid != myNode.pid) {
//                        determine attempted connection position
                        when{
                            fromId >= myNode.successor_id -> {
                                response = client.send("HELLO $fromId $fromPid $fromAddress",myNode.successor_address,myNode.successor_port)
                            }
                            fromId <= myNode.predecessor_id -> {
                                response = client.send("HELLO $fromId $fromPid $fromAddress",myNode.predecessor_address,myNode.predecessor_port)
                            }
                            fromId > myNode.predecessor_id && fromId < myNode.id -> {
                                client.send("UPDATE SUCCESSOR $fromId $fromPid $fromAddress",myNode.predecessor_address,myNode.predecessor_port)
                                response = "UPDATE ${myNode.id} ${myNode.ipAddress} 33456 : ${myNode.predecessor_id} ${myNode.predecessor_address} ${myNode.predecessor_port}"
                            }
                            fromId < myNode.successor_id && fromId > myNode.id -> {
                                client.send("UPDATE PREDECESSOR $fromId $fromPid $fromAddress",myNode.successor_address,myNode.successor_port)
                            }
                            else -> {
                                //generate number between predecessor and successor but not me
                                while (true) {
                                    fromId = IntRange(myNode.predecessor_id,myNode.successor_id -1).random()
                                    if(fromId != myNode.id) break
                                }
                                when {
                                    fromId < myNode.id -> {
                                        client.send("UPDATE SUCCESSOR $fromId $fromPid $fromAddress",myNode.predecessor_address,myNode.predecessor_port)
                                    }
                                    fromId > myNode.id -> {
                                        client.send("UPDATE PREDECESSOR $fromId $fromPid $fromAddress",myNode.successor_address,myNode.successor_port)
                                    }
                                }

                            }

                        }
                    }else {
//                        this is a round token close connection
                        response = "THIS IS A PING-BACK"

                    }
                }
                else -> {
                    response = "COMMAND NOT KNOWN"
                }
            }


//            Send response
            val outputStream = socket.getOutputStream()
            val outputStreamWriter = OutputStreamWriter(outputStream)
            outputStreamWriter.write(response)
            outputStreamWriter.flush()
            socket.close()

        } catch (e: Throwable) {
            println("Something went wrong")
        }

    }
}
private class ServerThread(private val socket : Socket,private val myNode : Node,  private val client : Client) : Thread() {
    init {
        start()
    }

    override fun run() {
        try{
            super.run()
            val inputStream = socket.getInputStream()
            val inputStreamReader = InputStreamReader(inputStream)
            val stringBuffer = StringBuffer()
            while (true) {
                val x = inputStreamReader.read()
                if ( x.toChar() == '#') break
                stringBuffer.append(x.toChar())
            }
            val request = stringBuffer.toString()
            var response = ""

//          Check request and service it
            when{
                request.startsWith("HELLO") -> {
//                    get the hello data
                    val data = request.split(" ")
                    val fromId =  data[1].toInt()
                    val fromAddress = data[2]

//                    don't handle requests from yourself
                    if(fromId != myNode.id) {
                        if ( )
                    }else {
                        response = "THIS IS A PING-BACK"
                    }
                }
                else -> {
                    response = "COMMAND NOT KNOWN"
                }
            }
            val outputStream = socket.getOutputStream()
            val outputStreamWriter = OutputStreamWriter(outputStream)
            outputStreamWriter.write(response)
            outputStreamWriter.flush()
            socket.close()

        }catch (e : Throwable) {
            println("Something went wrong")
        }

    }
}