import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;


public class FirstServerHandler extends SimpleChannelInboundHandler<Message> {
    private boolean isAuthorized = false;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("New active channel");
        TextMessage answer = new TextMessage();
        answer.setText("Successfully connection");
        ctx.writeAndFlush(answer);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        if (msg instanceof AuthMessage){
            AuthMessage message = (AuthMessage) msg;
            System.out.println("incoming authorization: " + message.getLogin()+"/"+message.getPassword());
            isAuthorized = AuthorizationService.authorizationAttempt(message.getLogin(),message.getPassword());
            TextMessage answer = new TextMessage();
            System.out.println(isAuthorized);
            if(isAuthorized){
                answer.setText("Успешная авторизация");
            } else {
                answer.setText("Неправильно введены данные или такой пользователь уже залогинен");
            };
            ctx.writeAndFlush(answer);
        }
        if (msg instanceof TextMessage && isAuthorized) {
            TextMessage message = (TextMessage) msg;
            System.out.println("incoming text message: " + message.getText());
            ctx.writeAndFlush(msg);
        }
        if (msg instanceof DateMessage && isAuthorized) {
            DateMessage message = (DateMessage) msg;
            System.out.println("incoming date message: " + message.getDate());
            ctx.writeAndFlush(msg);
        }
        if(!(msg instanceof AuthMessage) && !isAuthorized){
            TextMessage answer = new TextMessage();
            answer.setText("Необходимо авторизоваться чтобы отправлять сообщения на сервер, пришлите логин и пароль");
            System.out.println("Сообщение без авторизации");
            ctx.writeAndFlush(answer);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("client disconnect");
    }
}