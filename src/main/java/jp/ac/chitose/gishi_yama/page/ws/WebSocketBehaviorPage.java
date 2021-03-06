package jp.ac.chitose.gishi_yama.page.ws;

import jp.ac.chitose.gishi_yama.page.BasePage;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.wicket.Application;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.ws.api.WebSocketBehavior;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;
import org.apache.wicket.protocol.ws.api.message.TextMessage;
import org.apache.wicket.protocol.ws.api.registry.IKey;
import org.apache.wicket.protocol.ws.api.registry.SimpleWebSocketConnectionRegistry;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.wicketstuff.annotation.mount.MountPath;

@Slf4j
@MountPath("ws")
public class WebSocketBehaviorPage extends BasePage {
  private static final long serialVersionUID = -6048991130742250499L;

  private String applicationName;
  private String sessionId;
  private IKey key;
  private IModel<Integer> counterModel;

  public WebSocketBehaviorPage(final PageParameters parameters) {
    super(parameters);
    counterModel = Model.of(0);
    val coutner = new Label("counter", counterModel).setOutputMarkupId(true);
    add(coutner);

    add(new WebSocketBehavior() {
      private static final long serialVersionUID = -6393777237087512361L;

      @Override
      protected void onConnect(ConnectedMessage message) {
        applicationName = message.getApplication().getName();
        sessionId = message.getSessionId();
        key = message.getKey();
      }

      @Override
      protected void onMessage(WebSocketRequestHandler handler, TextMessage message) {
        String output = message.getText() + " : " + sessionId;
        log.info(output);

        // WebSocket接続しているクライアント全てに、他のクライアントからのメッセージを加工して送信する
        val registry = new SimpleWebSocketConnectionRegistry();
        val connections = registry.getConnections(Application.get(applicationName));
        connections.stream()
            .filter(con -> con != null)
            .filter(con -> con.isOpen())
            .forEach(con -> {
              try {
                con.sendMessage(message.getText() + " : " + sessionId);
              } catch (Exception e) {
                e.printStackTrace();
              }
            });

        // このページ（Session）によるonMessageが呼び出された回数をカウントアップし、コンポーネントを更新する
        counterModel.setObject(counterModel.getObject() + 1);
        handler.add(coutner);
      }
    });
  }

  @Override
  public void renderHead(IHeaderResponse response) {
    super.renderHead(response);
    response.render(JavaScriptHeaderItem.forUrl("js/websocket-callback.js"));
  }

}
