package stomp;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.stomp.Frame;
import io.vertx.ext.stomp.ServerFrame;
import io.vertx.ext.stomp.StompServer;
import io.vertx.ext.stomp.StompServerHandler;
import io.vertx.ext.stomp.utils.Headers;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ServerStomp extends AbstractVerticle {

    private  Logger logger = Logger.getLogger(ServerStomp.class.getName());
    private  String username;
    private  List<String> topics = new ArrayList<>();

    public void run() throws IOException, ParseException {

        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);

        router.route(HttpMethod.GET, "/validate").handler(BodyHandler.create());
        router.get("/validate").handler(rc -> {
            String topic = rc.getBodyAsJson().getString("validate");
            if (topics.contains(topic)) {
                rc.end("true");
            }
            rc.end("false");
        });

        FileReader reader = new FileReader("keycloak.json");


        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);

        JsonObject keycloakJson = new JsonObject(jsonObject);

        OAuth2Auth oauth2 = KeycloakAuth.create(vertx, OAuth2FlowType.PASSWORD, keycloakJson);

        OAuth2AuthHandler oAuth2AuthHandler = OAuth2AuthHandler.create(vertx, oauth2);
        router.route("/").handler(oAuth2AuthHandler).handler(req -> req.response().end("Hello Vert.x!"));

        vertx.createHttpServer().requestHandler(router).listen(8081);

        AuthenticationProvider provider = KeycloakAuth.create(vertx, OAuth2FlowType.PASSWORD, keycloakJson);

        Factory factory = new Factory();

        topics = factory.getTopics();
        username = factory.getUsername();

//        Future<StompServer> server = StompServer.create(vertx)
//                .handler(StompServerHandler.create(vertx).destinationFactory(factory).authProvider(provider))
//                .listen();

        Future<StompServer> server = StompServer.create(vertx)
                .handler(StompServerHandler.create(vertx).destinationFactory(factory).authProvider(provider).subscribeHandler( rc -> {
                    factory.handle(rc);
                }).sendHandler(rc -> {
                    factory.correct(rc);
                }))
                .listen();
    }
}
