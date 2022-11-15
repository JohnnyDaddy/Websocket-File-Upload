const endPoint = "ws://" + self.location.hostname + ":8080/upload";

let files = [];
let ws;

function openWebSocket() {
  ws = new WebSocket(endPoint);
  ws.binaryType = "arraybuffer";

  ws.onmessage = function (event) {
    self.postMessage(JSON.parse(event.data));
  };

  ws.onopen = function () {
    process();
  };
}

function ready() {
  return ws !== undefined && ws.readyState !== WebSocket.CLOSED;
}

async function process() {
  while (files.length > 0) {
    const blob = files.shift();
    ws.send(
      JSON.stringify({
        command: "prepare",
        receivedBytes: (0).toString(),
        originalFileName: blob.name,
      })
    );
    const BYTES_PER_CHUNK = 1024 * 1024 * 2;
    const SIZE = blob.size;

    let start = 0;
    let end = BYTES_PER_CHUNK;
    let chunk;

    while (start < SIZE) {
      if ("mozSlice" in blob) {
        chunk = blob.mozSlice(start, end);
      } else if ("webkitSlice" in blob) {
        chunk = blob.webkitSlice(start, end);
      } else {
        chunk = blob.slice(start, end);
      }

      start = end;
      end = start + BYTES_PER_CHUNK;

      const bytes = await chunk.arrayBuffer();
      ws.send(bytes);
    }
    ws.send(
      JSON.stringify({
        command: "finish",
        receivedBytes: SIZE.toString(),
        originalFileName: blob.name,
      })
    );
  }
}

self.onmessage = function (e) {
  for (const file of e.data.files) {
    files.push(file);
  }

  if (ready()) {
    process();
  } else {
    openWebSocket();
  }
};
