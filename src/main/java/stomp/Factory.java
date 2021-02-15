package stomp;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.stomp.*;
import io.vertx.ext.stomp.utils.Headers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.List;

public class Factory extends AbstractVerticle implements DestinationFactory, Frames, Handler<ServerFrame> {

    private String username;
    private List<String> topics = new ArrayList<>();

    public Factory() {
    }

    public List<String> parseJSON() {

        String configuration = "{ "+
                "\"username\":\"testuser\"," +
                "\"topics\":[" +
                "    \"/topicA\"," +
                "    \"/topic5\"," +
                "    \"/topic52\"," +
                "    \"/topic71\"]" + "}";

        try {
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(configuration);
            username = (String) jsonObject.get("username");
            JSONArray array= (JSONArray) jsonObject.get("topics");
            for(int i=0; i<array.size(); i++) {
                topics.add((String) array.get(i));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return topics;
    }

    public  String getUsername(){
        return username;
    }

    public  ArrayList<String> getTopics() {
        return (ArrayList<String>) parseJSON();
    }


    @Override
    public Destination create(Vertx vertx, String name)  {
        topics = parseJSON();
        if (!topics.contains(name)) {
            return null;
        }
        return Destination.topic(vertx,name);
    }

    @Override
    public void handle(ServerFrame serverFrame) {
        Frame frame = serverFrame.frame();
        StompServerConnection connection = serverFrame.connection();
        String id = frame.getHeader(Frame.ID);
        String destination = frame.getHeader(Frame.DESTINATION);
        String ack = frame.getHeader(Frame.ACK);
        if (ack == null) {
            ack = "auto";
        }
        Destination dest = connection.handler().getOrCreateDestination(destination);
        if (dest!=null) {
            connection.write(Frames.createReceiptFrame(frame.getReceipt(), Headers.create()));
            //connection.write(Frames.createReceiptFrame(frame.getReceipt(), frame.getHeaders()));
//            String receipt = frame.getReceipt();
//            if (receipt != null) {
//                connection.write(Frames.createReceiptFrame(frame.getReceipt(), Headers.create()));
//            }
            //Frames.handleReceipt(frame, connection);
        } else {
            connection.write(Frames.createErrorFrame(
                    "Error",
                    Headers.create(frame.getHeaders()), "Invalid topic"));
            connection.close();
            return;
        }
    }

    public void correct(ServerFrame serverFrame) {
        Frame frame = serverFrame.frame();
        StompServerConnection connection = serverFrame.connection();
        connection.write(frame);
    }
}
