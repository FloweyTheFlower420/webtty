var socket = null;
var term = null;
function connect() {
    console.log("ws://" + location.hostname + ":" + location.port + "/device");
    socket = new WebSocket("ws://" + location.hostname + ":" + location.port + "/device");
    // Create WebSocket connection.
    term = new Terminal();
    var status = 0;
    term.open(document.getElementById('terminal'));
    term.write('Welcome to the webtty client!\n\r');

    socket.addEventListener('open', function (event) {
        console.log("connected to server!");
        var thing = {}
        thing.device = document.getElementById('devin').value;
        thing.tty = document.getElementById('ttyin').value;
        socket.send(JSON.stringify(thing));

        term.onData( (data) => {
            socket.send(data);
        });

        // Listen for messages
        socket.addEventListener('message', function (event) {
            if(status == 0) {
                document.getElementById('tty').innerHTML = "TTY: " + JSON.parse(event.data).tty + "<button onclick='disconnect()'>Disconnect</button>";
                status = 1;
                socket.send('\n');
            }
            else
                term.write(event.data);
        });
    });

    socket.addEventListener('close', () => {
        console.log("Closed!");
        disconnect();
    });
}

function disconnect() {
    delete term;
    socket.close();
    document.getElementById('tty').innerHTML = '<div id="tty">Device name: <input type="text" id="devin" placeholder="Device name"><br>TTY name: <input type="text" id="ttyin" placeholder="TTY name"><br><button onclick="connect()">Connect!</button></div>'
    document.getElementById('terminal').innerHTML = '';
}