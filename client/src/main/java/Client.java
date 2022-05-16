import handler.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import message.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class Client {
    FileOutputStream outputStream = null;
    FileInputStream inputStream = null;
    Channel channel;

    public static void main(String[] args) throws FileNotFoundException {
        new Client().start();
    }

    public void start() throws FileNotFoundException {
        final NioEventLoopGroup group = new NioEventLoopGroup(1);
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    //максимальный размер сообщения равен 1024*1024 байт, в начале сообщения пдля хранения длины зарезервировано 3 байта,
                                    //которые отбросятся после получения всего сообщения и передачи его дальше по цепочке
                                    new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 3, 0, 3),
                                    //Перед отправкой добавляет в начало сообщение 3 байта с длиной сообщения
                                    new LengthFieldPrepender(3),
                                    new JsonDecoder(),
                                    new JsonEncoder(),
                                    new SimpleChannelInboundHandler<Message>() {

                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws FileNotFoundException {
                                            if (msg instanceof TextMessage){
                                                System.out.println("  Получено сообщение " + msg);
                                            }
                                            if (msg instanceof ServerResponseWithContentMessage){
                                                ServerResponseWithContentMessage message = (ServerResponseWithContentMessage) msg;
                                                try{
                                                  outputStream.write(message.getContent());
                                                  if (message.isLast()){
                                                      System.out.println("  С сервера получен файл");
                                                      outputStream.close();
                                                      outputStream = null;}
                                                } catch(Exception e){
                                                    e.printStackTrace();
                                                }
                                            }
                                            if (msg instanceof ListMessage){
                                                String[] list = ((ListMessage) msg).getList();
                                                if (list.length>0){
                                                System.out.println("  Список файлов на сервере:");
                                                for(int i = 0; i< list.length; i++){
                                                    System.out.println("  "+list[i]);
                                                }
                                                } else System.out.println("  Файлов на сервере нет");
                                            }
                                        }
                                    }
                            );
                        }
                    });

            System.out.println("  Приложение запущено");
            channel = bootstrap.connect("localhost", 9000).sync().channel();
            new Thread(()->{
                new CommandHandler(this);
            }).start();

            channel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    void sendFile(String fileSourcePoint, String fileDestinationPoint) throws Exception {
            if (inputStream == null) {
                inputStream = new FileInputStream(fileSourcePoint);
                System.out.println("  Отправляю файл из " + fileSourcePoint);
            }
            byte[] fileContent = inputStream.readNBytes(64 * 1024);
            SendFileToServerMessage contentMessage = new SendFileToServerMessage();
            contentMessage.setPath(fileDestinationPoint);
            contentMessage.setLast(inputStream.available() <= 0);
            contentMessage.setContent(fileContent);

            channel.writeAndFlush(contentMessage).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (!contentMessage.isLast()) {
                        sendFile(fileSourcePoint, fileDestinationPoint);
                    }
                }
            });
            if (contentMessage.isLast()) {
                System.out.println("  Файл отправлен");
                inputStream.close();
                inputStream = null;
            }
    }

    void getFileFromServer(String fileSourcePoint, String fileDestinationPoint) throws FileNotFoundException {
            RequestFileFromServerMessage message = new RequestFileFromServerMessage();
            message.setPath(fileSourcePoint);
            if (outputStream==null) {
              outputStream = new FileOutputStream(fileDestinationPoint);
            }
             channel.writeAndFlush(message);
    }

    void deleteFromServer(String filePath) {
        DeleteFileFromServerMessage message = new DeleteFileFromServerMessage();
        message.setPath(filePath);
        System.out.println("  Отправляю запрос на удаление файла");
        channel.writeAndFlush(message);
    }
    void requestFilelist() {
        RequestFilelistMessage message = new RequestFilelistMessage();
        channel.writeAndFlush(message);
    }
    void tryToAuth(String login, String password) {
        AuthMessage authMessage = new AuthMessage();
        authMessage.setLogin(login);
        authMessage.setPassword(password);
        System.out.println("  Пытаюсь авторизоваться: " + authMessage.getLogin() + "/" + authMessage.getPassword());
        channel.writeAndFlush(authMessage);
    }
}