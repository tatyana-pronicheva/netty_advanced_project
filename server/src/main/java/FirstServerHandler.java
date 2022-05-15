import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import message.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


public class FirstServerHandler extends SimpleChannelInboundHandler<Message> {
    private boolean isAuthorized = false;
    FileInputStream inputStream = null;
    FileOutputStream outputStream = null;
    static final String BASE_PATH = System.getProperty("user.dir") + File.separator;
    String homeFolder;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("New active channel");
        TextMessage answer = new TextMessage();
        answer.setText("Успешное подключение");
        ctx.writeAndFlush(answer);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws IOException {
        if (msg instanceof AuthMessage){
            AuthMessage message = (AuthMessage) msg;
            System.out.println("incoming authorization: " + message.getLogin()+"/"+message.getPassword());
            isAuthorized = AuthorizationService.authorizationAttempt(message.getLogin(),message.getPassword());
            TextMessage answer = new TextMessage();
            if(isAuthorized){
                answer.setText("Успешная авторизация");
                homeFolder = BASE_PATH + message.getLogin() + File.separator;
                new File(homeFolder).mkdir();
            } else {
                answer.setText("Неправильный логин или пароль");
            };
            ctx.writeAndFlush(answer);
        }
        if(!(msg instanceof AuthMessage) && !isAuthorized){
            TextMessage answer = new TextMessage();
            answer.setText("Необходимо авторизоваться чтобы отправлять сообщения на сервер, пришлите логин и пароль");
            System.out.println("Получено сообщение от неавторизованного пользователя");
            ctx.writeAndFlush(answer);
        }
        if (isAuthorized) {
            if (msg instanceof RequestFileFromServerMessage) {
                if (inputStream == null) {
                    RequestFileFromServerMessage message = (RequestFileFromServerMessage) msg;
                    File filePath = new File(homeFolder + message.getPath());
                    inputStream = new FileInputStream(filePath);
                }
                sendFile(ctx);
            }
            if (msg instanceof SendFileToServerMessage) {
                SendFileToServerMessage message = (SendFileToServerMessage) msg;
                try {
                    if (outputStream == null) {
                        outputStream = new FileOutputStream(homeFolder + message.getPath());
                    }
                    outputStream.write(message.getContent());
                    if (message.isLast()) {
                        System.out.println("Файл от клиента получен полностью");
                         outputStream.close();
                         outputStream = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    outputStream.close();
                    outputStream = null;
                }
            }
            if (msg instanceof DeleteFileFromServerMessage) {
                DeleteFileFromServerMessage deleteMessage = (DeleteFileFromServerMessage) msg;
                File file = new File(homeFolder + deleteMessage.getPath());
                file.delete();
            }
            if (msg instanceof RequestFilelistMessage) {
                String[] list = (new File(homeFolder)).list();
                ListMessage listMessage = new ListMessage();
                listMessage.setList(list);
                ctx.writeAndFlush(listMessage);
            }
        }
    }

    private void sendFile(ChannelHandlerContext ctx) throws IOException {
            byte[] fileContent = inputStream.readNBytes(64*1024);
            ServerResponseWithContentMessage contentMessage = new ServerResponseWithContentMessage();
            contentMessage.setLast(inputStream.available()<=0);
            contentMessage.setContent(fileContent);

        ctx.channel().writeAndFlush(contentMessage).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (!contentMessage.isLast()){
                    sendFile(ctx);
                }
            }
        });
        if(contentMessage.isLast()){
            inputStream.close();
            inputStream = null;
            System.out.println("Клиенту отправлен файл");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws IOException {
        if (inputStream!=null){
            inputStream.close();
        }
        System.out.println("client disconnect");
    }
}