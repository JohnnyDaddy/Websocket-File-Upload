package com.example.demo.ws;

import jakarta.annotation.PostConstruct;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@ServerEndpoint(
  value = "/upload",
  decoders = UploadServer.FileUploadMessageDecoder.class,
  encoders = UploadServer.FileUploadMessageEncoder.class
)
@Slf4j
@Component
public class UploadServer {

  // TODO: uploadPath DI가 @OnMessage 내에서는 적용되지 않는다. 아래의 하드코딩은 이를 위한 땜빵이다.
  @Value("${app.uploadPath}")
  private String uploadPath = "C:\\Dtonic\\upload\\socket";

  private RandomAccessFile uploadFile;
  private String filePath;
  private String originalFileName;
  private long receivedBytes = 0;

  @PostConstruct
  private void init() {
    if (uploadPath != null) {
      File uploadDir = new File(uploadPath);
      if (!uploadDir.exists()) {
        uploadDir.mkdirs();
      }
    }
  }

  @OnMessage
  public void receivePart(byte[] chunk, boolean last, Session session) {
    if (uploadFile == null && filePath != null) {
      try {
        uploadFile = new RandomAccessFile(filePath, "rw");
      } catch (FileNotFoundException e) {
        log.error("File does not exist", e);
      }
    }

    if (uploadFile != null) {
      try {
        uploadFile.write(chunk);
        receivedBytes += chunk.length;

        if (!last) {
          FileUploadMessage fileUploadMessage = new FileUploadMessage();
          fileUploadMessage.command = "receving";
          fileUploadMessage.receivedBytes = String.valueOf(receivedBytes);
          fileUploadMessage.originalFileName = originalFileName;
          session.getAsyncRemote().sendObject(fileUploadMessage);
        }
      } catch (IOException e) {
        log.error("An error occurred while writing file", e);
      }
    }
  }

  @OnMessage
  public void process(FileUploadMessage fileUploadMessage, Session session) {
    if ("prepare".equals(fileUploadMessage.command)) {
      originalFileName = fileUploadMessage.originalFileName;
      filePath = uploadPath + File.separatorChar + originalFileName;
    } else {
      close(session);
      fileUploadMessage.command = "complete";
      session.getAsyncRemote().sendObject(fileUploadMessage);
    }
  }

  @OnClose
  public void close(Session session) {
    if (uploadFile != null) {
      try {
        uploadFile.close();
      } catch (IOException e) {
        log.error("An error occurred while closing file");
      }
      uploadFile = null;
      filePath = null;
      originalFileName = null;
      receivedBytes = 0;
    }
  }

  static class FileUploadMessage {

    protected String command;
    protected String receivedBytes;
    protected String originalFileName;
  }

  public static class FileUploadMessageDecoder
    implements Decoder.Text<FileUploadMessage> {

    @Override
    public FileUploadMessage decode(final String text) throws DecodeException {
      FileUploadMessage fileUploadMessage = new FileUploadMessage();
      try (JsonReader reader = Json.createReader(new StringReader(text))) {
        JsonObject obj = reader.readObject();
        fileUploadMessage.originalFileName = obj.getString("originalFileName");
        fileUploadMessage.receivedBytes = obj.getString("receivedBytes");
        fileUploadMessage.command = obj.getString("command");
      }
      return fileUploadMessage;
    }

    @Override
    public boolean willDecode(final String s) {
      return true;
    }
  }

  public static class FileUploadMessageEncoder
    implements Encoder.Text<FileUploadMessage> {

    @Override
    public String encode(final FileUploadMessage fileUploadMessage)
      throws EncodeException {
      return Json
        .createObjectBuilder()
        .add("command", fileUploadMessage.command)
        .add("receivedBytes", fileUploadMessage.receivedBytes)
        .add("originalFileName", fileUploadMessage.originalFileName)
        .build()
        .toString();
    }
  }
}
