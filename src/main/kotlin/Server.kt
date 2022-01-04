import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class Server(private val port : Int, private val filePort : Int, private var myNode : Node, private val client : Client) {
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
//                println("Waiting for connection on address $inetAddress and port : $filePort for file transfer")
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
//                println("Waiting for connection on address $inetAddress and port : $port for commands")
                socket = serverSocket.accept()
                thread{
                    try{
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
                                            myNode = myNode.copy(predecessor_id = fromId, predecessor_port = 33456, predecessor_address = fromAddress)
                                            response = "UPDATE : ${myNode.id} ${myNode.ipAddress} 33456 : ${myNode.predecessor_id} ${myNode.predecessor_address} ${myNode.predecessor_port}"
                                        }
                                        fromId < myNode.successor_id && fromId > myNode.id -> {
                                            client.send("UPDATE PREDECESSOR $fromId $fromPid $fromAddress",myNode.successor_address,myNode.successor_port)
                                            myNode = myNode.copy(successor_id = fromId,successor_port = 33456,successor_address = fromAddress)
                                            response = "UPDATE : ${myNode.successor_id} ${myNode.successor_address} 33456 : ${myNode.id} ${myNode.ipAddress} 33456"
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
                                                    myNode = myNode.copy(predecessor_id = fromId, predecessor_port = 33456, predecessor_address = fromAddress)
                                                    response = "UPDATE : ${myNode.id} ${myNode.ipAddress} 33456 : ${myNode.predecessor_id} ${myNode.predecessor_address} ${myNode.predecessor_port}"

                                                }
                                                fromId > myNode.id -> {
                                                    client.send("UPDATE PREDECESSOR $fromId $fromPid $fromAddress",myNode.successor_address,myNode.successor_port)
                                                    myNode = myNode.copy(successor_id = fromId,successor_port = 33456,successor_address = fromAddress)
                                                    response = "UPDATE : ${myNode.successor_id} ${myNode.successor_address} 33456 : ${myNode.id} ${myNode.ipAddress} 33456"

                                                }
                                            }

                                        }

                                    }
                                }else {
//                        this is a round token close connection
                                    response = "THIS IS A PING-BACK"

                                }
                            }
                            request.startsWith("UPDATE") -> {
                                val data = request.split(" ")
                                val command = data[1]
                                val fromId = data[2].toInt()
                                val fromAddress = data[4]

                                myNode = if(command == "PREDECESSOR") {
                                    myNode.copy(predecessor_address = fromAddress, predecessor_port = 33456, predecessor_id = fromId)
                                }else{
                                    myNode.copy(successor_address = fromAddress, successor_port = 33456, successor_id = fromId)
                                }

                            }
                            request.startsWith("SEARCH") -> {
                                response = "FILE NOT FOUND"
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
//                ServerThread(socket,myNode, client)
                println("New command connection made")
            }
        }catch (e : Exception) {
            println("Something t wrong with the listener")
        }
    }
}
private class FileServerThread(private val socket : Socket, private var myNode : Node, private val client : Client ) : Thread() {
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
private class ServerThread(private val socket : Socket, private var myNode : Node, private val client : Client) : Thread() {
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
                                myNode = myNode.copy(predecessor_id = fromId, predecessor_port = 33456, predecessor_address = fromAddress)
                                response = "UPDATE : ${myNode.id} ${myNode.ipAddress} 33456 : ${myNode.predecessor_id} ${myNode.predecessor_address} ${myNode.predecessor_port}"
                            }
                            fromId < myNode.successor_id && fromId > myNode.id -> {
                                client.send("UPDATE PREDECESSOR $fromId $fromPid $fromAddress",myNode.successor_address,myNode.successor_port)
                                myNode = myNode.copy(successor_id = fromId,successor_port = 33456,successor_address = fromAddress)
                                response = "UPDATE : ${myNode.successor_id} ${myNode.successor_address} 33456 : ${myNode.id} ${myNode.ipAddress} 33456"
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
                                        myNode = myNode.copy(predecessor_id = fromId, predecessor_port = 33456, predecessor_address = fromAddress)
                                        response = "UPDATE : ${myNode.id} ${myNode.ipAddress} 33456 : ${myNode.predecessor_id} ${myNode.predecessor_address} ${myNode.predecessor_port}"

                                    }
                                    fromId > myNode.id -> {
                                        client.send("UPDATE PREDECESSOR $fromId $fromPid $fromAddress",myNode.successor_address,myNode.successor_port)
                                        myNode = myNode.copy(successor_id = fromId,successor_port = 33456,successor_address = fromAddress)
                                        response = "UPDATE : ${myNode.successor_id} ${myNode.successor_address} 33456 : ${myNode.id} ${myNode.ipAddress} 33456"

                                    }
                                }

                            }

                        }
                    }else {
//                        this is a round token close connection
                        response = "THIS IS A PING-BACK"

                    }
                }
                request.startsWith("UPDATE") -> {
                    val data = request.split(" ")
                    val command = data[1]
                    val fromId = data[2].toInt()
                    val fromAddress = data[4]

                    myNode = if(command == "PREDECESSOR") {
                        myNode.copy(predecessor_address = fromAddress, predecessor_port = 33456, predecessor_id = fromId)
                    }else{
                        myNode.copy(successor_address = fromAddress, successor_port = 33456, successor_id = fromId)
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